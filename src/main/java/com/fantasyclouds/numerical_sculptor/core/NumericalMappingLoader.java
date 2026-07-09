package com.fantasyclouds.numerical_sculptor.core;

import com.fantasyclouds.numerical_sculptor.data.DamageMappingData;
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
public class NumericalMappingLoader {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Map<String, DamageMappingData> MAPPINGS = new HashMap<>();

    public static void reload(ResourceManager resourceManager) {
        MAPPINGS.clear();

        Collection<ResourceLocation> resources = resourceManager
                .listResources("entity_damage", loc -> loc.getPath().endsWith(".json"))
                .keySet();

        LOGGER.info("Found {} damage mapping file(s).", resources.size());

        for (ResourceLocation res : resources) {
            try {
                Optional<Resource> optResource = resourceManager.getResource(res);
                if (optResource.isEmpty()) {
                    LOGGER.error("Resource not present: {}", res);
                    continue;
                }

                DamageMappingData newConfig;
                try (Reader reader = new InputStreamReader(optResource.get().open())) {
                    newConfig = GSON.fromJson(reader, DamageMappingData.class);
                }

                if (newConfig == null) {
                    LOGGER.error("Failed to parse (null): {}", res);
                    continue;
                }

                if (!newConfig.isOverallValid()) {
                    LOGGER.error("Invalid config, skipping. entity='{}', hasHealth={}, attemptsDamage={}. File: {}",
                            newConfig.getEntity(), newConfig.hasHealthConfig(), newConfig.attemptsDamageConfig(), res);
                    continue;
                }

                String key = newConfig.getEntity();
                DamageMappingData existing = MAPPINGS.get(key);

                if (existing == null) {
                    // 第一个有效配置，直接放入
                    MAPPINGS.put(key, newConfig);
                    LOGGER.debug("Registered config for entity '{}' from {}", key, res);
                } else {
                    // 合并缺失字段（生命优先，伤害优先），不覆盖已有有效配置
                    boolean merged = false;

                    // 合并生命
                    if (newConfig.hasHealthConfig() && !existing.hasHealthConfig()) {
                        existing.maxHealth = newConfig.getMaxHealth(); // 直接访问字段或通过 setter
                        merged = true;
                    } else if (newConfig.hasHealthConfig()) {
                        LOGGER.warn("Health config for entity '{}' already present, ignoring from {}", key, res);
                    }

                    // 合并伤害
                    if (newConfig.hasValidDamageConfig() && !existing.hasValidDamageConfig()) {
                        // 复制全部伤害相关字段
                        existing.standardRange = newConfig.getStandardRange();
                        existing.targetRange = newConfig.getTargetRange();
                        existing.function = newConfig.getFunction();
                        existing.curvature = newConfig.getCurvature();
                        existing.nodes = newConfig.getNodes();
                        existing.clampSourceMin = newConfig.isClampSourceMin();
                        existing.clampSourceMax = newConfig.isClampSourceMax();
                        existing.clampTargetMin = newConfig.isClampTargetMin();
                        existing.clampTargetMax = newConfig.isClampTargetMax();
                        merged = true;
                    } else if (newConfig.hasValidDamageConfig()) {
                        LOGGER.warn("Damage config for entity '{}' already present, ignoring from {}", key, res);
                    }

                    if (merged) {
                        LOGGER.info("Merged missing fields into entity '{}' from {}", key, res);
                    } else {
                        LOGGER.debug("No new fields to merge for entity '{}' from {}", key, res);
                    }
                }

            } catch (IOException e) {
                LOGGER.error("I/O error reading: {}", res, e);
            } catch (Exception e) {
                LOGGER.error("Unexpected error: {}", res, e);
            }
        }

        LOGGER.info("Damage mapping reloaded, {} entities configured.", MAPPINGS.size());
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
                NumericalMappingLoader.reload(resourceManager);
            }
        });
    }
}