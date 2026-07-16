package com.fantasyclouds.numerical_sculptor.network;

import com.fantasyclouds.numerical_sculptor.NumericalSculptor;
import com.fantasyclouds.numerical_sculptor.core.ItemAttributeLoader;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 分片发送物品属性配置，解决单包 32KB 限制
 */
public class SyncItemModifiersChunkPacket {
    public final int requestId;
    public final int chunkIndex;
    public final int totalChunks;
    public final String data;

    public SyncItemModifiersChunkPacket(int requestId, int chunkIndex, int totalChunks, String data) {
        this.requestId = requestId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    public static void encode(SyncItemModifiersChunkPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.requestId);
        buf.writeInt(msg.chunkIndex);
        buf.writeInt(msg.totalChunks);
        buf.writeUtf(msg.data);
    }

    public static SyncItemModifiersChunkPacket decode(FriendlyByteBuf buf) {
        return new SyncItemModifiersChunkPacket(
                buf.readInt(),
                buf.readInt(),
                buf.readInt(),
                buf.readUtf()
        );
    }

    public static void handle(SyncItemModifiersChunkPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                ItemAttributeLoader.handleChunk(msg);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    /**
     * 向指定玩家发送完整配置（自动分片）
     */
    public static void sendToClient(ServerPlayer player, String fullJson) {
        int maxChunkSize = 30000;
        int totalChunks = (int) Math.ceil((double) fullJson.length() / maxChunkSize);
        int requestId = (int) (System.nanoTime() % Integer.MAX_VALUE);
        for (int i = 0; i < totalChunks; i++) {
            String chunk = fullJson.substring(i * maxChunkSize, Math.min((i + 1) * maxChunkSize, fullJson.length()));
            SyncItemModifiersChunkPacket packet = new SyncItemModifiersChunkPacket(requestId, i, totalChunks, chunk);
            NumericalSculptor.NETWORK.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    /**
     * 向所有在线玩家发送完整配置（自动分片）
     */
    public static void sendToAll(String fullJson) {
        int maxChunkSize = 30000;
        int totalChunks = (int) Math.ceil((double) fullJson.length() / maxChunkSize);
        int requestId = (int) (System.nanoTime() % Integer.MAX_VALUE);
        for (int i = 0; i < totalChunks; i++) {
            String chunk = fullJson.substring(i * maxChunkSize, Math.min((i + 1) * maxChunkSize, fullJson.length()));
            SyncItemModifiersChunkPacket packet = new SyncItemModifiersChunkPacket(requestId, i, totalChunks, chunk);
            NumericalSculptor.NETWORK.send(PacketDistributor.ALL.noArg(), packet);
        }
    }
}
