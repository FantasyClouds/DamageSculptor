package com.fantasyclouds.numerical_sculptor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(NumericalSculptor.MODID)
public class NumericalSculptor {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "numerical_sculptor";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public NumericalSculptor() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NSConfig.SPEC);

    }

}
