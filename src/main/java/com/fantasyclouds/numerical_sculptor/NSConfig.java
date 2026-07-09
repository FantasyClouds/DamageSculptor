package com.fantasyclouds.numerical_sculptor;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "one_enough_math_damage", bus = Mod.EventBusSubscriber.Bus.MOD)
public class NSConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue DEBUG_ENABLED =
            BUILDER.comment("If true, damaged players will receive chat messages showing damage before and after mapping.")
                    .define("debug", false);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
