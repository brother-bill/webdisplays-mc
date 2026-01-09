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
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

/**
 * Control to force all viewers to re-sync to the current master's playback time.
 *
 * When a player clicks "Sync Now" in the screen config UI:
 * 1. This control is sent to the server
 * 2. Server resets all viewers' initialSyncDone flags (via broadcast)
 * 3. All viewers will re-sync on their next tick
 *
 * This is useful for:
 * - Testing that sync works correctly
 * - Re-syncing after someone's video got out of sync
 * - Forcing sync after ads finished
 */
public class ForceSyncControl extends ScreenControl {
    public static final ResourceLocation id = ResourceLocation.parse("webdisplays:force_sync");

    public ForceSyncControl() {
        super(id);
    }

    public ForceSyncControl(FriendlyByteBuf buf) {
        super(id);
        // No additional data needed
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        // No additional data needed
    }

    @Override
    public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
        ServerPlayer sender = (ServerPlayer) ctx.player();

        ScreenData screen = tes.getScreen(side);
        if (screen == null) {
            Log.warning("[VideoSync] Force sync: No screen found at side %s", side);
            return;
        }

        Log.info("[VideoSync] Force sync requested by %s for screen at %s, side %s",
            sender.getName().getString(), pos, side);

        // If no sync master exists yet, make the requester the master
        if (screen.syncMasterUUID == null) {
            screen.syncMasterUUID = sender.getGameProfile().getId();
            Log.info("[VideoSync] Force sync: %s became sync master (no previous master)",
                sender.getName().getString());
        }

        // Log current sync state
        Log.info("[VideoSync] Force sync: Broadcasting current state - master=%s, time=%.2f, paused=%s",
            screen.syncMasterUUID, screen.syncPlaybackTime, screen.syncPaused);

        // Create a force sync broadcast that will tell all clients to re-sync
        ForceSyncBroadcast broadcast = new ForceSyncBroadcast(
            screen.syncMasterUUID, screen.syncPlaybackTime, screen.syncPaused);
        tes.broadcastToWatchers(side, broadcast);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
        // This shouldn't be called on client - use ForceSyncBroadcast instead
    }
}
