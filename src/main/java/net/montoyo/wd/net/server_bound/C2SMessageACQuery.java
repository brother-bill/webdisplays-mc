/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server_bound;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageACResult;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.Arrays;

public class C2SMessageACQuery extends Packet {
    public static final CustomPacketPayload.Type<C2SMessageACQuery> TYPE =
            new CustomPacketPayload.Type<>(id("ac_query"));

    public static final StreamCodec<FriendlyByteBuf, C2SMessageACQuery> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    C2SMessageACQuery::new
            );

    private String beginning;
    private boolean matchExact;

    public C2SMessageACQuery(String beg, boolean exact) {
        beginning = beg;
        matchExact = exact;
    }

    public C2SMessageACQuery(FriendlyByteBuf buf) {
        super(buf);
        beginning = buf.readUtf();
        matchExact = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(beginning);
        buf.writeBoolean(matchExact);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void run(ServerPlayer player) {
        GameProfile[] profiles = WebDisplays.PROXY.getOnlineGameProfiles();
        NameUUIDPair[] result;

        if (matchExact)
            result = Arrays.stream(profiles).filter(gp -> gp.getName().equalsIgnoreCase(beginning)).map(NameUUIDPair::new).toArray(NameUUIDPair[]::new);
        else {
            final String lBeg = beginning.toLowerCase();
            result = Arrays.stream(profiles).filter(gp -> gp.getName().toLowerCase().startsWith(lBeg)).map(NameUUIDPair::new).toArray(NameUUIDPair[]::new);
        }

        WDNetworkRegistry.sendToPlayer(player, new S2CMessageACResult(result));
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isServer(ctx)) {
            ServerPlayer player = getSender(ctx);
            if (player != null) {
                ctx.enqueueWork(() -> run(player));
            }
        }
    }
}
