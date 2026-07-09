package com.fantasyclouds.numerical_sculptor.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;

public class DamageMappingData {

    @SerializedName("entity")
    private String entity;

    @SerializedName("max_health")
    public Double maxHealth;   // 可选，null 表示不修改血量

    @SerializedName("standard_range")
    public List<Double> standardRange;

    @SerializedName("target_range")
    public List<Double> targetRange;

    @SerializedName("function")
    public String function;

    @SerializedName("curvature")
    public double curvature;

    @SerializedName("nodes")
    public List<List<Double>> nodes;

    // 替换原来的 clampSource / clampTarget 为四个独立开关，默认均为 true
    @SerializedName("clamp_source_min")
    public boolean clampSourceMin;

    @SerializedName("clamp_source_max")
    public boolean clampSourceMax;

    @SerializedName("clamp_target_min")
    public boolean clampTargetMin;

    @SerializedName("clamp_target_max")
    public boolean clampTargetMax;

    // ---------- getters ----------
    public String getEntity() {
        return entity;
    }

    public List<Double> getStandardRange() {
        return standardRange;
    }

    public List<Double> getTargetRange() {
        return targetRange;
    }

    public String getFunction() {
        return function;
    }

    public double getCurvature() {
        return curvature;
    }

    public List<List<Double>> getNodes() {
        return nodes;
    }

    public boolean isClampSourceMin() {
        return clampSourceMin;
    }

    public boolean isClampSourceMax() {
        return clampSourceMax;
    }

    public boolean isClampTargetMin() {
        return clampTargetMin;
    }

    public boolean isClampTargetMax() {
        return clampTargetMax;
    }

    public Double getMaxHealth() {
        return maxHealth;
    }
    // ---------- 数据校验 ----------
    public boolean isValid() {
        if (entity == null || entity.isEmpty()) return false;
        if (standardRange == null || standardRange.size() < 2) return false;
        if (targetRange == null || targetRange.size() < 2) return false;
        if (standardRange.get(0) >= standardRange.get(1) || standardRange.get(1) <= 0) return false;
        if (targetRange.get(0) > targetRange.get(1)) return false;
        if (function == null || function.isEmpty()) return false;
        if (maxHealth != null && maxHealth <= 0) return false;

        String funcLower = function.toLowerCase(Locale.ROOT);
        if (!funcLower.matches("linear|nonlinear|piecewise")) {
            return false;
        }

        if (funcLower.equals("piecewise")) {
            if (nodes == null || nodes.size() < 2) return false;
            double prevX = -Double.MAX_VALUE;
            for (List<Double> node : nodes) {
                if (node == null || node.size() != 2) return false;
                double x = node.get(0);
                if (x < prevX) return false;
                prevX = x;
            }
            if (nodes.get(0).get(0) != 0.0 || nodes.get(nodes.size() - 1).get(0) != 1.0) {
                return false;
            }
        }
        return true;
    }
    public boolean hasHealthConfig() {
        return maxHealth != null && maxHealth > 0;
    }

    /** 是否尝试提供伤害映射字段（即至少填写了部分伤害字段） */
    public boolean attemptsDamageConfig() {
        return standardRange != null || targetRange != null || function != null;
    }

    /** 伤害配置是否完整有效 */
    public boolean hasValidDamageConfig() {
        if (standardRange == null || standardRange.size() != 2) return false;
        if (targetRange == null || targetRange.size() != 2) return false;
        if (function == null || function.isEmpty()) return false;

        double srcMin = standardRange.get(0);
        double srcMax = standardRange.get(1);
        double tgtMin = targetRange.get(0);
        double tgtMax = targetRange.get(1);
        if (srcMin >= srcMax || srcMax <= 0) return false;
        if (tgtMin > tgtMax) return false;

        String funcLower = function.toLowerCase(Locale.ROOT);
        if (!funcLower.matches("linear|nonlinear|piecewise")) return false;

        if (funcLower.equals("nonlinear") && curvature <= 0) return false;
        if (funcLower.equals("piecewise")) {
            if (nodes == null || nodes.size() < 2) return false;
            double prevX = -Double.MAX_VALUE;
            for (List<Double> node : nodes) {
                if (node.size() != 2) return false;
                double x = node.get(0);
                if (x < prevX) return false;
                prevX = x;
            }
            if (nodes.get(0).get(0) != 0.0 || nodes.get(nodes.size() - 1).get(0) != 1.0) return false;
        }
        return true;
    }

    /**
     * 全局有效性（用于加载时判断整个 JSON 是否可用）
     * 规则：
     * - 如果没有生命配置，也没有尝试提供伤害字段，则无效。
     * - 如果尝试提供了伤害字段，则必须通过完整伤害校验，否则整个 JSON 无效。
     * - 如果只有生命配置（且无伤害字段），则只要生命有效即可。
     */
    public boolean isOverallValid() {
        if (entity == null || entity.isEmpty()) return false;

        boolean hasHealth = hasHealthConfig();
        boolean attemptsDamage = attemptsDamageConfig();

        // 必须至少提供一种有效配置
        if (!hasHealth && !attemptsDamage) return false;

        // 如果尝试提供伤害，则伤害必须完整有效
        if (attemptsDamage && !hasValidDamageConfig()) return false;

        // 如果有生命配置，maxHealth 必须合法（已经在 hasHealthConfig 中判断）
        return true;
    }
}
