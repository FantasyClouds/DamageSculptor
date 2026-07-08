package com.fantasyclouds.one_enough_math_damage;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(OneEnoughMathDamage.MODID)
public class OneEnoughMathDamage {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "one_enough_math_damage";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public OneEnoughMathDamage() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, OEMDConfig.SPEC);

    }

}
