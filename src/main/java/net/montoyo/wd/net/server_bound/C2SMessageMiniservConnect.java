/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server_bound;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.miniserv.server.ClientManager;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageMiniservKey;

public class C2SMessageMiniservConnect extends Packet {
    public static final CustomPacketPayload.Type<C2SMessageMiniservConnect> TYPE =
            new CustomPacketPayload.Type<>(id("miniserv_connect"));

    public static final StreamCodec<FriendlyByteBuf, C2SMessageMiniservConnect> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    C2SMessageMiniservConnect::new
            );

    private byte[] modulus;
    private byte[] exponent;

    public C2SMessageMiniservConnect(byte[] mod, byte[] exp) {
        modulus = mod;
        exponent = exp;
    }

    public C2SMessageMiniservConnect(FriendlyByteBuf buf) {
        super(buf);

        modulus = BufferUtils.readBytes(buf);
        exponent = BufferUtils.readBytes(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        BufferUtils.writeBytes(buf, modulus);
        BufferUtils.writeBytes(buf, exponent);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isServer(ctx)) {
            ctx.enqueueWork(() -> {
                try {
                    ServerPlayer sender = getSender(ctx);
                    if (sender != null) {
                        ClientManager cliMgr = Server.getInstance().getClientManager();
                        byte[] encKey = cliMgr.encryptClientKey(sender.getGameProfile().getId(), modulus, exponent);

                        if (encKey != null) {
                            WDNetworkRegistry.sendToPlayer(sender, new S2CMessageMiniservKey(encKey));
                        }
                    }
                } catch (Throwable err) {
                    err.printStackTrace();
                    throw new RuntimeException(err);
                }
            });
        }
    }
}
