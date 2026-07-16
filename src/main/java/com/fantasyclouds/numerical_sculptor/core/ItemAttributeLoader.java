package com.fantasyclouds.numerical_sculptor.core;

import com.fantasyclouds.numerical_sculptor.NumericalSculptor;
import com.fantasyclouds.numerical_sculptor.data.ItemModifierConfig;
import com.fantasyclouds.numerical_sculptor.data.ItemModifierData;
import com.fantasyclouds.numerical_sculptor.data.ItemModifierEntry;
import com.fantasyclouds.numerical_sculptor.network.SyncItemModifiersChunkPacket;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

@Mod.EventBusSubscriber
public class ItemAttributeLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static Map<String, List<ItemModifierEntry>> itemModifiers = new HashMap<>();

    // 客户端缓存：存储未完成的分片数据
    private static final Map<Integer, List<String>> CHUNK_CACHE = new HashMap<>();
    private static final Map<Integer, Integer> CHUNK_TOTALS = new HashMap<>();

    public static void reloadResource(ResourceManager resourceManager) {
        Map<String, List<ItemModifierEntry>> newMap = new HashMap<>();

        Collection<ResourceLocation> resources = resourceManager.listResources(
                "item_modifiers", loc -> loc.getPath().endsWith(".json")
        ).keySet();

        NumericalSculptor.LOGGER.info("Found {} item modifier configuration(s)", resources.size());

        for (ResourceLocation res : resources) {
            try {
                Optional<Resource> optResource = resourceManager.getResource(res);
                if (optResource.isEmpty()) {
                    NumericalSculptor.LOGGER.warn("Resource not found: {}", res);
                    continue;
                }

                Reader reader = new InputStreamReader(optResource.get().open());
                JsonElement json = JsonParser.parseReader(reader);
                List<ItemModifierData> items = new ArrayList<>();

                if (json.isJsonObject()) {
                    ItemModifierConfig config = GSON.fromJson(json, ItemModifierConfig.class);
                    if (config != null && config.getItems() != null) {
                        items = config.getItems();
                    }
                } else if (json.isJsonArray()) {
                    items = GSON.fromJson(json, new TypeToken<List<ItemModifierData>>(){}.getType());
                } else {
                    NumericalSculptor.LOGGER.warn("Unexpected JSON structure in {}", res);
                    continue;
                }

                for (ItemModifierData itemData : items) {
                    if (!itemData.isValid()) {
                        NumericalSculptor.LOGGER .warn("Invalid item modifier entry in {}, itemId={}", res, itemData.getItemId());
                        continue;
                    }
                    newMap.put(itemData.getItemId(), itemData.getModifiers());
                    NumericalSculptor.LOGGER.debug("Loaded/Overwrote modifiers for item {}", itemData.getItemId());
                }
            } catch (Exception e) {
                NumericalSculptor.LOGGER.error("Failed to load item modifier config: {}", res, e);
            }
        }

        itemModifiers = newMap;
        NumericalSculptor.LOGGER.info("Item attribute modifiers reloaded, {} items configured.", itemModifiers.size());
    }

    public static List<ItemModifierEntry> getModifiers(String itemId) {
        return itemModifiers.get(itemId);
    }

    @OnlyIn(Dist.CLIENT)
    public static void applySyncData(Map<String, List<ItemModifierEntry>> data) {
        itemModifiers = data;
        NumericalSculptor.LOGGER.info("Received item modifier sync from server, {} items.", data.size());
    }

    public static String serializeToJson() {
        return GSON.toJson(itemModifiers);
    }

    public static Map<String, List<ItemModifierEntry>> deserializeFromJson(String json) {
        java.lang.reflect.Type type = new TypeToken<Map<String, List<ItemModifierEntry>>>(){}.getType();
        return GSON.fromJson(json, type);
    }

    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager rm, ProfilerFiller f) { return null; }
            @Override
            protected void apply(Void v, ResourceManager rm, ProfilerFiller f) {
                reloadResource(rm);
                if (ServerLifecycleHooks.getCurrentServer() != null) {
                    syncToAll();  // 分片同步
                }
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);  // 分片同步
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        syncToAll();  // 分片同步
    }

    /**
     * 客户端处理接收到的分片包（仅客户端调用）
     */
    @OnlyIn(Dist.CLIENT)
    public static void handleChunk(SyncItemModifiersChunkPacket packet) {
        synchronized (CHUNK_CACHE) {
            List<String> chunks = CHUNK_CACHE.computeIfAbsent(packet.requestId, k -> new ArrayList<>(packet.totalChunks));
            // 用 null 填充到指定索引
            while (chunks.size() <= packet.chunkIndex) {
                chunks.add(null);
            }
            chunks.set(packet.chunkIndex, packet.data);
            CHUNK_TOTALS.put(packet.requestId, packet.totalChunks);

            if (chunks.size() == packet.totalChunks && chunks.stream().allMatch(Objects::nonNull)) {
                StringBuilder sb = new StringBuilder();
                chunks.forEach(sb::append);
                String fullJson = sb.toString();
                try {
                    Map<String, List<ItemModifierEntry>> data = deserializeFromJson(fullJson);
                    applySyncData(data);
                } catch (Exception e) {
                    NumericalSculptor.LOGGER.error("Failed to apply synced item modifiers from chunks", e);
                }
                CHUNK_CACHE.remove(packet.requestId);
                CHUNK_TOTALS.remove(packet.requestId);
            }
        }
    }

    /**
     * 向所有在线玩家同步当前配置（自动分片，服务端调用）
     */
    public static void syncToAll() {
        String json = serializeToJson();
        SyncItemModifiersChunkPacket.sendToAll(json);
    }

    /**
     * 向指定玩家同步当前配置（自动分片，服务端调用）
     */
    public static void syncToClient(ServerPlayer player) {
        String json = serializeToJson();
        SyncItemModifiersChunkPacket.sendToClient(player, json);
    }
}
