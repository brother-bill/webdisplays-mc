/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server_bound;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.controls.ScreenControlRegistry;
import net.montoyo.wd.controls.builtin.*;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.Packet;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

public class C2SMessageScreenCtrl extends Packet {
    public static final CustomPacketPayload.Type<C2SMessageScreenCtrl> TYPE =
            new CustomPacketPayload.Type<>(id("screen_ctrl"));

    public static final StreamCodec<FriendlyByteBuf, C2SMessageScreenCtrl> STREAM_CODEC =
            StreamCodec.of(
                    (buf, msg) -> msg.write(buf),
                    C2SMessageScreenCtrl::new
            );

    @Deprecated(forRemoval = true)
    public static final int CTRL_LASER_MOVE = 0;
    @Deprecated(forRemoval = true)
    public static final int CTRL_LASER_UP = 0;
    @Deprecated(forRemoval = true)
    public static final int CTRL_LASER_DOWN = 0;
    @Deprecated(forRemoval = true)
    public static final int CTRL_SET_RESOLUTION = 0;

    ScreenControl control;
    BlockPos pos;
    BlockSide side;

    public C2SMessageScreenCtrl() {
    }

    public C2SMessageScreenCtrl(ScreenBlockEntity screen, BlockSide side, ScreenControl control) {
        this.pos = screen.getBlockPos();
        this.side = side;
        this.control = control;
    }

    protected static C2SMessageScreenCtrl base(ScreenBlockEntity screen, BlockSide side) {
        C2SMessageScreenCtrl packet = new C2SMessageScreenCtrl();
        packet.pos = screen.getBlockPos();
        packet.side = side;
        return packet;
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl setURL(ScreenBlockEntity tes, BlockSide side, String url, Vector3i remoteLocation) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new SetURLControl(url, remoteLocation);
        return ret;
    }

    @Deprecated(forRemoval = true)
    public C2SMessageScreenCtrl(ScreenBlockEntity tes, BlockSide side, NameUUIDPair friend, boolean del) {
        this(tes, side, new ModifyFriendListControl(friend, !del));
    }

    @Deprecated(forRemoval = true)
    public C2SMessageScreenCtrl(ScreenBlockEntity tes, BlockSide side, int fr, int or) {
        this(tes, side, new ManageRightsAndUpdgradesControl(fr, or));
    }

    @Deprecated(forRemoval = true)
    public C2SMessageScreenCtrl(ScreenBlockEntity tes, BlockSide side, ItemStack toRem) {
        this(tes, side, new ManageRightsAndUpdgradesControl(false, toRem));
    }

    @Deprecated(forRemoval = true)
    public C2SMessageScreenCtrl(ScreenBlockEntity tes, BlockSide side, Rotation rot) {
        this(tes, side, new ScreenModifyControl(rot));
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl vec2(ScreenBlockEntity tes, BlockSide side, int ctrl, Vector2i vec) {
        throw new RuntimeException("Moved: look into ScreenControlRegistry");
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl resolution(ScreenBlockEntity tes, BlockSide side, Vector2i vec) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new ScreenModifyControl(vec);
        return ret;
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl type(ScreenBlockEntity tes, BlockSide side, String text, BlockPos soundPos) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new KeyTypedControl(text, soundPos);
        return ret;
    }

    public static C2SMessageScreenCtrl laserMove(ScreenBlockEntity tes, BlockSide side, Vector2i vec) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new LaserControl(LaserControl.ControlType.MOVE, vec);
        return ret;
    }

    public static C2SMessageScreenCtrl laserMove(ScreenBlockEntity tes, BlockSide side, Vector2i vec, int button) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new LaserControl(LaserControl.ControlType.MOVE, vec, button);
        return ret;
    }

    public static C2SMessageScreenCtrl laserDown(ScreenBlockEntity tes, BlockSide side, Vector2i vec, int button) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new LaserControl(LaserControl.ControlType.DOWN, vec, button);
        return ret;
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl laserUp(ScreenBlockEntity tes, BlockSide side, int button) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new LaserControl(LaserControl.ControlType.UP, null, button);
        return ret;
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl jsRequest(ScreenBlockEntity tes, BlockSide side, int reqId, JSServerRequest reqType, Object... data) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new JSRequestControl(reqId, reqType, data);
        return ret;
    }

    @Deprecated(forRemoval = true)
    public static C2SMessageScreenCtrl autoVol(ScreenBlockEntity tes, BlockSide side, boolean av) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new AutoVolumeControl(av);
        return ret;
    }

    /**
     * Toggle video sync enabled/disabled for a screen.
     */
    public static C2SMessageScreenCtrl syncEnabled(ScreenBlockEntity tes, BlockSide side, boolean enabled) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new SyncEnabledControl(enabled);
        return ret;
    }

    /**
     * Create a video sync update from the master player.
     */
    public static C2SMessageScreenCtrl videoSync(ScreenBlockEntity tes, BlockSide side, double playbackTime, boolean paused) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = VideoSyncControl.masterUpdate(playbackTime, paused);
        return ret;
    }

    /**
     * Create a force sync request - triggers all viewers to re-sync to current master.
     */
    public static C2SMessageScreenCtrl forceSync(ScreenBlockEntity tes, BlockSide side) {
        C2SMessageScreenCtrl ret = base(tes, side);
        ret.control = new ForceSyncControl();
        return ret;
    }

    public C2SMessageScreenCtrl(FriendlyByteBuf buf) {
        super(buf);

        pos = buf.readBlockPos();
        side = (BlockSide) BufferUtils.readEnum(buf, (i) -> BlockSide.values()[i], (byte) 1);

        this.control = ScreenControlRegistry.parse(buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        BufferUtils.writeEnum(buf, side, (byte) 1);

        buf.writeUtf(control.getId().toString());
        control.write(buf);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void checkPermission(ServerPlayer sender, ScreenBlockEntity scr, int right) throws MissingPermissionException {
        int prights = scr.getScreen(side).rightsFor(sender);
        if ((prights & right) == 0)
            throw new MissingPermissionException(right, sender);
    }

    @Override
    public void handle(IPayloadContext ctx) {
        if (isServer(ctx)) {
            ctx.enqueueWork(() -> {
                try {
                    ServerPlayer sender = getSender(ctx);
                    if (sender == null) return;

                    Level level = sender.level();
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be instanceof ScreenBlockEntity tes) {
                        control.handleServer(pos, side, tes, ctx, (perm) -> {
                            try {
                                checkPermission(sender, tes, perm);
                                return true;
                            } catch (Throwable ignored) {
                                return false;
                            }
                        });
                    }
                } catch (MissingPermissionException e) {
                    e.printStackTrace();
                } catch (Throwable ignored) {
                }
            });
        }
    }
}
