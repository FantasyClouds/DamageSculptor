package com.fantasyclouds.numerical_sculptor.core;

import com.fantasyclouds.numerical_sculptor.data.DamageMappingData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber
public class HealthModificationHandler {

    @SubscribeEvent(priority = EventPriority.LOW) // 低优先级保证在其他 mod 之后执行
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;


        applyHealthConfig(entity);
    }

    public static void applyHealthConfig(LivingEntity entity) {
        String key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()).toString();
        DamageMappingData data = NumericalMappingLoader.getMapping(key);
        if (data == null || !data.hasHealthConfig()) return;

        double health = data.getMaxHealth();
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        // 如果当前生命值超出新的最大生命值，可以裁剪，也可以让原血量不变（可能超过最大，但游戏会自己处理）
        if (entity.getHealth() > health) {
            entity.setHealth((float) health);
        }
    }
}
