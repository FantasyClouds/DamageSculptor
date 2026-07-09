package com.fantasyclouds.numerical_sculptor.mixin;

import com.fantasyclouds.numerical_sculptor.NSConfig;
import com.fantasyclouds.numerical_sculptor.data.DamageMappingData;
import com.fantasyclouds.numerical_sculptor.core.NumericalMappingLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(value = LivingEntity.class)
public abstract class hurtMixin {
    @ModifyVariable(method = "hurt", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float applyDamageMapping(float amount, DamageSource source) {

        if (source.getEntity() == null) {
            return amount;
        }
        Entity attacker = source.getEntity();
        if (attacker.level().isClientSide() || attacker instanceof Player) {
            return amount;
        }
        Entity trueAttacker = attacker;
        if (trueAttacker instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            trueAttacker = owner;
        }

        String entityKey = ForgeRegistries.ENTITY_TYPES.getKey(trueAttacker.getType()).toString();
        DamageMappingData data = NumericalMappingLoader.getMapping(entityKey);
        if (data != null && !data.hasValidDamageConfig()) {
            return amount;
        }

        double srcMin = data.getStandardRange().get(0);
        double srcMax = data.getStandardRange().get(1);
        double tgtMin = data.getTargetRange().get(0);
        double tgtMax = data.getTargetRange().get(1);

        if (srcMax == 0) {
            return amount;
        }

        double clampedAmount = amount;

        // ----- 源范围独立钳制（上下限）-----
        if (data.isClampSourceMin() && clampedAmount < srcMin) {
            clampedAmount = srcMin;
        }
        if (data.isClampSourceMax() && clampedAmount > srcMax) {
            clampedAmount = srcMax;
        }

        // 标准化比例 t
        double X = (clampedAmount / srcMax) * tgtMax;
        double t = X / tgtMax;

        // 应用映射函数
        double mappedT;
        String function = data.getFunction().toLowerCase();

        switch (function) {
            case "nonlinear":
                mappedT = applyNonlinearMapping(t, data.getCurvature());
                break;
            case "piecewise":
                mappedT = applyPiecewiseMapping(t, data.getNodes());
                break;
            default: // linear
                mappedT = t;
                break;
        }

        // 计算最终伤害
        double newDamage = tgtMin + mappedT * (tgtMax - tgtMin);

        // ----- 目标范围独立钳制（上下限）-----
        if (data.isClampTargetMin() && newDamage < tgtMin) {
            newDamage = tgtMin;
        }
        if (data.isClampTargetMax() && newDamage > tgtMax) {
            newDamage = tgtMax;
        }

        // ----- 浮点安全性修正 -----
        if (Double.isNaN(newDamage) || Double.isInfinite(newDamage)) {
            return amount; // 回退到原始伤害，避免崩溃
        }
        if (newDamage < 0.0) {
            newDamage = 0.0;
        }
        if (newDamage > Float.MAX_VALUE) {
            newDamage = Float.MAX_VALUE;
        }

        // 调试信息
        if (NSConfig.DEBUG_ENABLED.get()) {
            LivingEntity victim = (LivingEntity) (Object) this;
            if (victim instanceof Player player) {
                String msg = String.format("§e[DamageMapper] §fRaw: %.1f → Mapped: %.1f §7(from %s)",
                        amount, newDamage, source.getEntity() != null ? trueAttacker.getName().getString() : "unknown");
                player.displayClientMessage(Component.literal(msg), false);
            }
        }

        return (float) newDamage;
    }

    // ---------- 映射方法（保持不变）----------
    private double applyNonlinearMapping(double t, double curvature) {
        if (curvature <= 0) return t;
        double c = curvature;
        if (Math.abs(c - 1.0) < 1e-9) {
            return Math.log1p(t) / Math.log(2.0);
        }
        double d = c - 1.0;
        double numerator = Math.expm1(d * Math.log1p(t));
        double g_t = numerator / d;
        double g_1 = Math.expm1(d * Math.log(2.0)) / d;
        return g_t / g_1;
    }

    private double applyPiecewiseMapping(double t, List<List<Double>> nodes) {
        if (nodes == null || nodes.size() < 2) return t;

        if (t <= nodes.get(0).get(0)) {
            double x0 = nodes.get(0).get(0), y0 = nodes.get(0).get(1);
            double x1 = nodes.get(1).get(0), y1 = nodes.get(1).get(1);
            if (x1 == x0) return y0;
            double slope = (y1 - y0) / (x1 - x0);
            return y0 + slope * (t - x0);
        }

        int last = nodes.size() - 1;
        if (t >= nodes.get(last).get(0)) {
            double x0 = nodes.get(last - 1).get(0), y0 = nodes.get(last - 1).get(1);
            double x1 = nodes.get(last).get(0), y1 = nodes.get(last).get(1);
            if (x1 == x0) return y1;
            double slope = (y1 - y0) / (x1 - x0);
            return y1 + slope * (t - x1);
        }

        for (int i = 1; i < nodes.size(); i++) {
            double x0 = nodes.get(i - 1).get(0), y0 = nodes.get(i - 1).get(1);
            double x1 = nodes.get(i).get(0), y1 = nodes.get(i).get(1);
            if (t >= x0 && t <= x1) {
                if (x1 == x0) return y0;
                return y0 + (y1 - y0) * (t - x0) / (x1 - x0);
            }
        }

        return t;
    }
}
