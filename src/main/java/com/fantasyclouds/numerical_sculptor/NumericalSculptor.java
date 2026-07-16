package com.fantasyclouds.numerical_sculptor;

import com.fantasyclouds.numerical_sculptor.network.SyncItemModifiersChunkPacket;
import com.fantasyclouds.numerical_sculptor.network.SyncItemModifiersPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(NumericalSculptor.MODID)
public class NumericalSculptor {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "numerical_sculptor";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel NETWORK = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );


    public NumericalSculptor() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, NSConfig.SPEC);

        NETWORK.registerMessage(0, SyncItemModifiersPacket.class,
                SyncItemModifiersPacket::encode,
                SyncItemModifiersPacket::decode,
                SyncItemModifiersPacket::handle);


        NETWORK.registerMessage(0, SyncItemModifiersChunkPacket.class,
                SyncItemModifiersChunkPacket::encode,
                SyncItemModifiersChunkPacket::decode,
                SyncItemModifiersChunkPacket::handle);
    }

}
