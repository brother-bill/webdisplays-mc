/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server_bound;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.RedstoneControlBlockEntity;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.math.Vector3i;

public class C2SMessageRedstoneCtrl extends Packet {
    public static final CustomPacketPayload.Type<C2SMessageRedstoneCtrl> TYPE =
            new CustomPacketPayload.Type<>(id("redstone_ctrl"));

    public static final StreamCodec<FriendlyByteBuf, C2SMessageRedstoneCtrl> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    C2SMessageRedstoneCtrl::new
            );

    private Vector3i pos;
    private String risingEdgeURL;
    private String fallingEdgeURL;

    public C2SMessageRedstoneCtrl() {
    }

    public C2SMessageRedstoneCtrl(Vector3i p, String r, String f) {
        pos = p;
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    public C2SMessageRedstoneCtrl(FriendlyByteBuf buf) {
        super(buf);
        pos = new Vector3i(buf);
        risingEdgeURL = buf.readUtf();
        fallingEdgeURL = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        pos.writeTo(buf);
        buf.writeUtf(risingEdgeURL);
        buf.writeUtf(fallingEdgeURL);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void run(Player player) {
        Level world = player.level();
        BlockPos blockPos = pos.toBlock();
        final double maxRange = player.blockInteractionRange();

        if (player.distanceToSqr(blockPos.getX(), blockPos.getY(), blockPos.getZ()) > maxRange * maxRange)
            return;

        BlockEntity te = world.getBlockEntity(blockPos);
        if (te == null || !(te instanceof RedstoneControlBlockEntity))
            return;

        RedstoneControlBlockEntity redCtrl = (RedstoneControlBlockEntity) te;
        if (!redCtrl.isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return;
        }

        ScreenBlockEntity tes = redCtrl.getConnectedScreen();
        if (tes == null)
            return;

        if ((tes.getScreen(redCtrl.getScreenSide()).rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
            return;

        redCtrl.setURLs(risingEdgeURL, fallingEdgeURL);
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
