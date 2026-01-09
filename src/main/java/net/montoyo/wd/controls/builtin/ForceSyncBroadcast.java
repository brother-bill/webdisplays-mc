package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.UUID;
import java.util.function.Function;

/**
 * Broadcast sent from server to clients to force them to re-sync.
 * Resets the client's initialSyncDone flag and provides current sync state.
 */
public class ForceSyncBroadcast extends ScreenControl {
    public static final ResourceLocation id = ResourceLocation.parse("webdisplays:force_sync_broadcast");

    private UUID masterUUID;
    private double playbackTime;
    private boolean paused;

    public ForceSyncBroadcast(UUID masterUUID, double playbackTime, boolean paused) {
        super(id);
        this.masterUUID = masterUUID;
        this.playbackTime = playbackTime;
        this.paused = paused;
    }

    public ForceSyncBroadcast(FriendlyByteBuf buf) {
        super(id);
        if (buf.readBoolean()) {
            masterUUID = buf.readUUID();
        } else {
            masterUUID = null;
        }
        playbackTime = buf.readDouble();
        paused = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(masterUUID != null);
        if (masterUUID != null) {
            buf.writeUUID(masterUUID);
        }
        buf.writeDouble(playbackTime);
        buf.writeBoolean(paused);
    }

    @Override
    public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
        // This is only sent server -> client, should not be received on server
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
        ScreenData screen = tes.getScreen(side);
        if (screen == null) {
            return;
        }

        Log.info("[VideoSync] Force sync broadcast received: master=%s, time=%.2f, paused=%s",
            masterUUID, playbackTime, paused);

        // Update sync state
        screen.syncMasterUUID = masterUUID;
        screen.syncPlaybackTime = playbackTime;
        screen.syncPaused = paused;
        screen.syncUpdateTimestamp = System.currentTimeMillis();

        // Reset sync flags to force re-sync
        screen.initialSyncDone = false;
        screen.syncStateRequested = true; // Already have state, don't request again
        screen.initialSyncReceivedTime = System.currentTimeMillis();
        screen.syncAttemptCount = 0;
        screen.lastSyncAttemptTime = 0;

        Log.info("[VideoSync] Client will re-sync on next tick");
    }
}
