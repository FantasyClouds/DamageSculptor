package com.fantasyclouds.damage_sculptor.util;

import java.util.Locale;

public enum DamageFunction {
    /** 线性：f(t) = t，弯曲度无效 */
    LINEAR,
    /** 对数压缩（上凸）：f(t) = ln(1 + α·t) / ln(1 + α)，α>0 时低伤拉高、高伤压缩，α→0 退化为线性 */
    LOG,
    /** 指数拉伸（下凸）：f(t) = (e^(α·t) - 1) / (e^α - 1)，α>0 时放大高伤差异，α→0 退化为线性 */
    EXPONENTIAL,
    /** 平方根（上凸，无弯曲度）：f(t) = √t */
    SQRT,
    /** 平方（下凸，无弯曲度）：f(t) = t² */
    SQUARE;

    /**
     * 应用映射函数。
     *
     * @param t         归一化参数，一般为 (原始伤害经过外推后的X) / targetMax，允许 ≥0
     * @param curvature 弯曲度，仅 LOG 和 EXPONENTIAL 使用，其他类型忽略；传入负值将视为 0
     * @return 映射后的归一化值 f(t)，f(0)=0，f(1)=1
     */
    public double apply(double t, double curvature) {
        if (t < 0) t = 0; // 安全钳制，避免负数导致数学错误
        switch (this) {
            case LINEAR:
                return t;
            case LOG: {
                if (curvature <= 0) return t; // 退化为线性
                double alpha = curvature;
                return Math.log1p(alpha * t) / Math.log1p(alpha); // log1p(x) = ln(1+x)，数值更稳定
            }
            case EXPONENTIAL: {
                if (curvature <= 0) return t;
                double alpha = curvature;
                return (Math.exp(alpha * t) - 1.0) / (Math.exp(alpha) - 1.0);
            }
            case SQRT:
                return Math.sqrt(t);
            case SQUARE:
                return t * t;
            default:
                return t;
        }
    }

    /**
     * 通过字符串获取枚举值，忽略大小写，未匹配时返回 LINEAR。
     */
    public static DamageFunction fromString(String name) {
        if (name == null) return LINEAR;
        try {
            return valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            // 兼容可能的简写
            return switch (name.toLowerCase(Locale.ROOT)) {
                case "log", "logarithmic" -> LOG;
                case "exp", "exponential" -> EXPONENTIAL;
                case "sqrt", "root" -> SQRT;
                case "square", "sq" -> SQUARE;
                default -> LINEAR;
            };
        }
    }
}
