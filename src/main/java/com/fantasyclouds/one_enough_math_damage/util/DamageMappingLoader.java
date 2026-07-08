package com.fantasyclouds.one_enough_math_damage.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;

import static com.mojang.text2speech.Narrator.LOGGER;

@Mod.EventBusSubscriber
public class DamageMappingLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, DamageMappingData> MAPPINGS = new HashMap<>();

    /**
     * 重新读取所有配置，清空旧数据。
     * 对无效配置会记录 ERROR 并跳过，不再静默忽略。
     */
    public static void reload(ResourceManager resourceManager) {
        MAPPINGS.clear();

        Collection<ResourceLocation> resources = resourceManager
                .listResources("entity_damage", loc -> loc.getPath().endsWith(".json"))
                .keySet();

        LOGGER.info("Found {} damage mapping file(s) in all namespaces.", resources.size());

        for (ResourceLocation res : resources) {
            try {
                Optional<Resource> optResource = resourceManager.getResource(res);
                if (optResource.isEmpty()) {
                    LOGGER.error("Resource not present: {}", res);
                    continue;
                }

                DamageMappingData config;
                try (Reader reader = new InputStreamReader(optResource.get().open())) {
                    config = GSON.fromJson(reader, DamageMappingData.class);
                }

                if (config == null) {
                    LOGGER.error("Failed to parse damage mapping file (null result): {}", res);
                    continue;
                }

                if (!config.isValid()) {
                    LOGGER.error("Invalid damage mapping config, skipping. Reason: entity='{}', function='{}', standard_range={}, target_range={}. File: {}",
                            config.getEntity(), config.getFunction(),
                            config.getStandardRange(), config.getTargetRange(), res);
                    continue;
                }

                String key = config.getEntity();
                if (MAPPINGS.containsKey(key)) {
                    LOGGER.warn("Duplicate damage mapping for entity '{}'. Overwriting with {}.", key, res);
                }
                MAPPINGS.put(key, config);
                LOGGER.debug("Loaded damage mapping for entity '{}' from {}", key, res);

            } catch (IOException e) {
                LOGGER.error("I/O error while reading damage mapping file: {}", res, e);
            } catch (Exception e) {
                LOGGER.error("Unexpected error while processing damage mapping file: {}", res, e);
            }
        }

        LOGGER.info("Damage mapping reloaded successfully. {} valid configurations loaded.", MAPPINGS.size());
    }

    @Nullable
    public static DamageMappingData getMapping(String entityId) {
        return MAPPINGS.get(entityId);
    }

    public static Map<String, DamageMappingData> getAllMappings() {
        return Collections.unmodifiableMap(MAPPINGS);
    }

    @SubscribeEvent
    public static void onServerReload(AddReloadListenerEvent event) {
        event.addListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return null;
            }
            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                DamageMappingLoader.reload(resourceManager);
            }
        });
    }
}
