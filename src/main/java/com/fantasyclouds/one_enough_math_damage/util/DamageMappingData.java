package com.fantasyclouds.one_enough_math_damage.util;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;

public class DamageMappingData {
    @SerializedName("entity")
    private String entity;

    @SerializedName("standard_range")
    private List<Double> standardRange;

    @SerializedName("target_range")
    private List<Double> targetRange;

    @SerializedName("function")
    private String function;

    @SerializedName("curvature")
    private double curvature = 1.0;

    @SerializedName("nodes")
    private List<List<Double>> nodes;

    // 替换原来的 clampSource / clampTarget 为四个独立开关，默认均为 true
    @SerializedName("clamp_source_min")
    private boolean clampSourceMin = true;

    @SerializedName("clamp_source_max")
    private boolean clampSourceMax = true;

    @SerializedName("clamp_target_min")
    private boolean clampTargetMin = true;

    @SerializedName("clamp_target_max")
    private boolean clampTargetMax = true;

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


    // ---------- 数据校验 ----------
    public boolean isValid() {
        if (entity == null || entity.isEmpty()) return false;
        if (standardRange == null || standardRange.size() < 2) return false;
        if (targetRange == null || targetRange.size() < 2) return false;
        if (standardRange.get(0) >= standardRange.get(1) || standardRange.get(1) <= 0) return false;
        if (targetRange.get(0) > targetRange.get(1)) return false;
        if (function == null || function.isEmpty()) return false;

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
}
