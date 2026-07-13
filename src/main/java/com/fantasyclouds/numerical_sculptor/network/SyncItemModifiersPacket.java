package com.fantasyclouds.numerical_sculptor.network;

import com.fantasyclouds.numerical_sculptor.NumericalSculptor;
import com.fantasyclouds.numerical_sculptor.core.ItemAttributeLoader;
import com.fantasyclouds.numerical_sculptor.data.ItemModifierEntry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
public class SyncItemModifiersPacket {
    private final String json;

    public SyncItemModifiersPacket(String json) {
        this.json = json;
    }

    public static void encode(SyncItemModifiersPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.json);
    }

    public static SyncItemModifiersPacket decode(FriendlyByteBuf buf) {
        return new SyncItemModifiersPacket(buf.readUtf());
    }

    public static void handle(SyncItemModifiersPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 客户端处理
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                try {
                    Map<String, List<ItemModifierEntry>> data = ItemAttributeLoader.deserializeFromJson(msg.json);
                    ItemAttributeLoader.applySyncData(data);
                } catch (Exception e) {
                    NumericalSculptor.LOGGER.error("Failed to apply synced item modifiers", e);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    // 发送给所有玩家
    public static void sendToAll(String json) {
        NumericalSculptor.NETWORK.send(PacketDistributor.ALL.noArg(), new SyncItemModifiersPacket(json));
    }
}
