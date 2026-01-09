package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Log;

import java.util.UUID;
import java.util.function.Function;

/**
 * Network control for video playback synchronization.
 *
 * SIMPLIFIED ONE-TIME SYNC APPROACH:
 * - Sync happens ONCE when a player first loads a screen with sync-enabled content
 * - The first player becomes the "master" and their time is stored on the server
 * - New players joining seek ONCE to the master's time, then stop syncing
 * - After initial sync, browsers play naturally without continuous syncing
 *
 * This avoids the issues with continuous syncing (pausing, frame-by-frame playback)
 * while still ensuring all players start at approximately the same time.
 *
 * Flow:
 * 1. Player loads screen with sync-enabled URL
 * 2. Client sends their current playback time to server
 * 3. Server either makes them master (if first) or responds with master's time
 * 4. Non-master clients seek ONCE to master's time
 * 5. No more syncing after initial sync
 */
public class VideoSyncControl extends ScreenControl {
    public static final ResourceLocation id = ResourceLocation.parse("webdisplays:video_sync");

    // The player UUID who is the sync master (null = no master yet)
    private UUID masterUUID;
    // Current playback time in seconds
    private double playbackTime;
    // Whether video is paused
    private boolean paused;
    // Whether this is a master update (client->server) or broadcast (server->client)
    private boolean isMasterUpdate;

    /**
     * Create a sync state update from the master player.
     * Used when master sends their playback state to server.
     */
    public static VideoSyncControl masterUpdate(double playbackTime, boolean paused) {
        VideoSyncControl ctrl = new VideoSyncControl();
        ctrl.masterUUID = null; // Server will fill this in
        ctrl.playbackTime = playbackTime;
        ctrl.paused = paused;
        ctrl.isMasterUpdate = true;
        return ctrl;
    }

    /**
     * Create a sync broadcast from server to clients.
     * Used when server broadcasts the master's state to all viewers.
     */
    public static VideoSyncControl broadcast(UUID masterUUID, double playbackTime, boolean paused) {
        VideoSyncControl ctrl = new VideoSyncControl();
        ctrl.masterUUID = masterUUID;
        ctrl.playbackTime = playbackTime;
        ctrl.paused = paused;
        ctrl.isMasterUpdate = false;
        return ctrl;
    }

    private VideoSyncControl() {
        super(id);
    }

    public VideoSyncControl(FriendlyByteBuf buf) {
        super(id);
        isMasterUpdate = buf.readBoolean();
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
        buf.writeBoolean(isMasterUpdate);
        buf.writeBoolean(masterUUID != null);
        if (masterUUID != null) {
            buf.writeUUID(masterUUID);
        }
        buf.writeDouble(playbackTime);
        buf.writeBoolean(paused);
    }

    @Override
    public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
        // This is a master update from a client
        if (!isMasterUpdate) {
            // Ignore broadcasts received on server (shouldn't happen)
            return;
        }

        ServerPlayer sender = (ServerPlayer) ctx.player();
        UUID senderUUID = sender.getGameProfile().getId();

        ScreenData screen = tes.getScreen(side);
        if (screen == null) {
            Log.warning("[VideoSync] No screen found at side %s", side);
            return;
        }

        // Check if sync is enabled for this URL
        screen.updateSyncEnabled();
        if (!screen.syncEnabled) {
            if (Log.DEBUG_VIDEO_SYNC) {
                Log.info("[VideoSync] Sync not enabled for URL: %s", screen.url);
            }
            return;
        }

        // Check if current master has timed out (no updates for 30+ seconds)
        long now = System.currentTimeMillis();
        long masterTimeout = 30000; // 30 seconds
        boolean masterTimedOut = screen.syncMasterUUID != null &&
            (now - screen.syncUpdateTimestamp) > masterTimeout;

        if (masterTimedOut) {
            Log.info("[VideoSync] Previous master timed out (no updates for %d seconds), clearing master",
                (now - screen.syncUpdateTimestamp) / 1000);
            screen.syncMasterUUID = null;
        }

        // If no master yet (or timed out), this player becomes master
        if (screen.syncMasterUUID == null) {
            screen.syncMasterUUID = senderUUID;
            Log.info("[VideoSync] Player %s became sync master for screen at %s (time=%.2f)",
                sender.getName().getString(), pos, playbackTime);
        }

        // Check if sender is the current master
        boolean isMaster = screen.syncMasterUUID.equals(senderUUID);

        // Log if a non-master is trying to send updates (for debugging)
        if (!isMaster && Log.DEBUG_VIDEO_SYNC) {
            Log.info("[VideoSync] Non-master %s sent sync update, current master is %s",
                sender.getName().getString(), screen.syncMasterUUID);
        }

        if (isMaster) {
            // Update server-side sync state from master
            screen.syncPlaybackTime = playbackTime;
            screen.syncPaused = paused;
            screen.syncUpdateTimestamp = System.currentTimeMillis();

            if (Log.DEBUG_VIDEO_SYNC) {
                Log.info("[VideoSync] Master update: time=%.2f, paused=%s", playbackTime, paused);
            }
        }

        // Always broadcast current state to the sender (and all watchers)
        // This ensures new joiners get the current state
        if (Log.DEBUG_VIDEO_SYNC) {
            Log.info("[VideoSync] Broadcasting sync state to watchers (master=%s, time=%.2f)",
                screen.syncMasterUUID, screen.syncPlaybackTime);
        }

        VideoSyncControl broadcast = VideoSyncControl.broadcast(
            screen.syncMasterUUID, screen.syncPlaybackTime, screen.syncPaused);
        tes.broadcastToWatchers(side, broadcast);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
        // This is a broadcast from the server
        if (isMasterUpdate) {
            // Ignore master updates on client (shouldn't happen)
            return;
        }

        ScreenData screen = tes.getScreen(side);
        if (screen == null) {
            return;
        }

        if (Log.DEBUG_VIDEO_SYNC) {
            Log.info("[VideoSync] Received sync broadcast: master=%s, time=%.2f, paused=%s",
                masterUUID, playbackTime, paused);
        }

        // Update local sync state
        screen.updateSyncState(masterUUID, playbackTime, paused);

        // Mark when we received this for one-time initial sync
        // Only set this once - the first broadcast we receive triggers the seek
        if (screen.initialSyncReceivedTime == 0) {
            screen.initialSyncReceivedTime = System.currentTimeMillis();
            if (Log.DEBUG_VIDEO_SYNC) {
                Log.info("[VideoSync] Initial sync data received, will seek on next tick");
            }
        }
    }

    // Getters for the sync state
    public UUID getMasterUUID() {
        return masterUUID;
    }

    public double getPlaybackTime() {
        return playbackTime;
    }

    public boolean isPaused() {
        return paused;
    }
}
