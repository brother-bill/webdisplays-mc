package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Base class for all WebDisplays network packets.
 * Packets now implement CustomPacketPayload for NeoForge 1.21.1.
 */
public abstract class Packet implements CustomPacketPayload {

    public Packet() {
    }

    public Packet(FriendlyByteBuf buf) {
        // Subclasses should override and decode from buffer
    }

    /**
     * Write packet data to buffer
     */
    public abstract void write(FriendlyByteBuf buf);

    /**
     * Handle the packet on the receiving side
     */
    public abstract void handle(IPayloadContext ctx);

    /**
     * Check if we're on the client side
     */
    public boolean isClient(IPayloadContext ctx) {
        return ctx.flow().isClientbound();
    }

    /**
     * Check if we're on the server side
     */
    public boolean isServer(IPayloadContext ctx) {
        return ctx.flow().isServerbound();
    }

    /**
     * Get the sender as ServerPlayer (only valid on server side)
     */
    public ServerPlayer getSender(IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    /**
     * Helper to create a ResourceLocation for this mod
     */
    protected static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("webdisplays", path);
    }
}
