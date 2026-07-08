package com.fantasyclouds.damage_sculptor;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(DamageSculptor.MODID)
public class DamageSculptor {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "Damage Sculptor";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();


    public DamageSculptor() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, DSConfig.SPEC);

    }

}
