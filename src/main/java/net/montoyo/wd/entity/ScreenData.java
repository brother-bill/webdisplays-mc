package net.montoyo.wd.entity;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.listeners.MCEFCursorChangeListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.browser.InWorldQueries;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import org.cef.browser.CefBrowser;

import java.util.ArrayList;
import java.util.UUID;

public class ScreenData {
    public BlockSide side;
    public Vector2i size;
    public Vector2i resolution;
    public Rotation rotation = Rotation.ROT_0;
    public String url;
    protected VideoType videoType;
    public NameUUIDPair owner;
    public ArrayList<NameUUIDPair> friends;
    public int friendRights;
    public int otherRights;
    public CefBrowser browser;
    public ArrayList<ItemStack> upgrades;
    public boolean doTurnOnAnim;
    public long turnOnTime;
    public Player laserUser;
    public final Vector2i lastMousePos = new Vector2i();
    public NibbleArray redstoneStatus; //null on client
    public boolean autoVolume = true;

    public int mouseType;

    // ===== VIDEO SYNC FIELDS =====
    // Per-screen user preference to enable/disable sync (saved with screen)
    public boolean userSyncEnabled = false;
    // Master UUID is set server-side when first player loads a sync-enabled URL
    public UUID syncMasterUUID = null;
    // Last known playback time from master (server-side state)
    public double syncPlaybackTime = 0.0;
    // Is the video paused? (server-side state)
    public boolean syncPaused = false;
    // When the sync state was last updated (for interpolation)
    public long syncUpdateTimestamp = 0;
    // Is sync enabled for current URL
    public boolean syncEnabled = false;
    // Last time this client sent a sync update (client-side, to throttle)
    public long lastSyncSentTime = 0;
    // CLIENT-SIDE: Has this client completed initial sync for this screen?
    public boolean initialSyncDone = false;
    // CLIENT-SIDE: Have we requested sync state from server?
    public boolean syncStateRequested = false;
    // CLIENT-SIDE: Time when we received initial sync data (for one-time seek)
    public long initialSyncReceivedTime = 0;
    // CLIENT-SIDE: Number of sync attempts made (for retry logic)
    public int syncAttemptCount = 0;
    // CLIENT-SIDE: Time of last sync attempt (for spacing out retries)
    public long lastSyncAttemptTime = 0;
    // CLIENT-SIDE: Max sync attempts before giving up
    public static final int MAX_SYNC_ATTEMPTS = 10;
    // CLIENT-SIDE: Delay between sync attempts in ms (allows ads to finish)
    public static final long SYNC_RETRY_DELAY_MS = 3000;

    public static ScreenData deserialize(CompoundTag tag, HolderLookup.Provider registries) {
        ScreenData ret = new ScreenData();
        ret.side = BlockSide.values()[tag.getByte("Side")];
        ret.size = new Vector2i(tag.getInt("Width"), tag.getInt("Height"));
        ret.resolution = new Vector2i(tag.getInt("ResolutionX"), tag.getInt("ResolutionY"));
        ret.rotation = Rotation.values()[tag.getByte("Rotation")];
        ret.url = tag.getString("URL");
        ret.videoType = VideoType.getTypeFromURL(ret.url);

        if (ret.resolution.x <= 0 || ret.resolution.y <= 0) {
            float psx = ((float) ret.size.x) * 16.f - 4.f;
            float psy = ((float) ret.size.y) * 16.f - 4.f;
            psx *= 8.f; //TODO: Use ratio in config file
            psy *= 8.f;

            ret.resolution.x = (int) psx;
            ret.resolution.y = (int) psy;
        }

        if (tag.contains("OwnerName")) {
            String name = tag.getString("OwnerName");
            UUID uuid = tag.getUUID("OwnerUUID");
            ret.owner = new NameUUIDPair(name, uuid);
        }

        ListTag friends = tag.getList("Friends", 10);
        ret.friends = new ArrayList<>(friends.size());

        for (int i = 0; i < friends.size(); i++) {
            CompoundTag nf = friends.getCompound(i);
            NameUUIDPair pair = new NameUUIDPair(nf.getString("Name"), nf.getUUID("UUID"));
            ret.friends.add(pair);
        }

        ret.friendRights = tag.getByte("FriendRights");
        ret.otherRights = tag.getByte("OtherRights");

        ListTag upgrades = tag.getList("Upgrades", 10);
        ret.upgrades = new ArrayList<>();

        for (int i = 0; i < upgrades.size(); i++)
            ret.upgrades.add(ItemStack.parseOptional(registries, upgrades.getCompound(i)));

        if (tag.contains("AutoVolume"))
            ret.autoVolume = tag.getBoolean("AutoVolume");

        if (tag.contains("SyncEnabled"))
            ret.userSyncEnabled = tag.getBoolean("SyncEnabled");

        return ret;
    }

    public CompoundTag serialize(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("Side", (byte) side.ordinal());
        tag.putInt("Width", size.x);
        tag.putInt("Height", size.y);
        tag.putInt("ResolutionX", resolution.x);
        tag.putInt("ResolutionY", resolution.y);
        tag.putByte("Rotation", (byte) rotation.ordinal());
        tag.putString("URL", url);

        if (owner == null)
            Log.warning("Found TES with NO OWNER!!");
        else {
            tag.putString("OwnerName", owner.name);
            tag.putUUID("OwnerUUID", owner.uuid);
        }

        ListTag list = new ListTag();
        for (NameUUIDPair f : friends) {
            CompoundTag nf = new CompoundTag();
            nf.putString("Name", f.name);
            nf.putUUID("UUID", f.uuid);

            list.add(nf);
        }

        tag.put("Friends", list);
        tag.putByte("FriendRights", (byte) friendRights);
        tag.putByte("OtherRights", (byte) otherRights);

        list = new ListTag();
        for (ItemStack is : upgrades)
            list.add(is.save(registries));

        tag.put("Upgrades", list);
        tag.putBoolean("AutoVolume", autoVolume);
        tag.putBoolean("SyncEnabled", userSyncEnabled);
        return tag;
    }

    public int rightsFor(Player ply) {
        return rightsFor(ply.getGameProfile().getId());
    }

    public int rightsFor(UUID uuid) {
        if (owner.uuid.equals(uuid))
            return ScreenRights.ALL;

        return friends.stream().anyMatch(f -> f.uuid.equals(uuid)) ? friendRights : otherRights;
    }

    public void setupRedstoneStatus(Level world, BlockPos start) {
        if (world.isClientSide()) {
            Log.warning("Called Screen.setupRedstoneStatus() on client.");
            return;
        }

        if (redstoneStatus != null) {
            Log.warning("Called Screen.setupRedstoneStatus() on server, but redstone status is non-null");
            return;
        }

        Direction[] VALUES = Direction.values();
        redstoneStatus = new NibbleArray(size.x * size.y);
        final Direction facing = VALUES[side.reverse().ordinal()];
        final ScreenIterator it = new ScreenIterator(start, side, size);

        while (it.hasNext()) {
            int idx = it.getIndex();
            redstoneStatus.set(idx, world.getSignal(it.next(), facing));
        }
    }


    public void clampResolution() {
        if (resolution.x > CommonConfig.Screen.maxResolutionX) {
            float newY = ((float) resolution.y) * ((float) CommonConfig.Screen.maxResolutionX) / ((float) resolution.x);
            resolution.x = CommonConfig.Screen.maxResolutionX;
            resolution.y = (int) newY;
        }

        if (resolution.y > CommonConfig.Screen.maxResolutionY) {
            float newX = ((float) resolution.x) * ((float) CommonConfig.Screen.maxResolutionY) / ((float) resolution.y);
            resolution.x = (int) newX;
            resolution.y = CommonConfig.Screen.maxResolutionY;
        }
    }

    public void createBrowser(ScreenBlockEntity be, boolean doAnim) {
        if (WebDisplays.PROXY instanceof ClientProxy) {
            browser = WDBrowser.createBrowser(WebDisplays.applyBlacklist(url != null ? url : "https://www.google.com"), false);

            // set screen
            if (browser instanceof MCEFBrowser mcefBrowser) {
                if (rotation.isVertical)
                    mcefBrowser.resize(resolution.y, resolution.x);
                else
                    mcefBrowser.resize(resolution.x, resolution.y);

                mcefBrowser.setCursorChangeListener((type) -> mouseType = type);
            }

            // setup screen as in world
            if (browser instanceof WDBrowser wdBrowser) {
                InWorldQueries.attach(be, side, wdBrowser);
            }

            doTurnOnAnim = doAnim;
            turnOnTime = System.currentTimeMillis();
        }
    }

    // ===== VIDEO SYNC METHODS =====

    /**
     * Check if video sync is enabled for the current URL.
     * Updates the syncEnabled flag based on config, URL whitelist, AND user preference.
     */
    public void updateSyncEnabled() {
        // Sync is enabled if: global config enabled AND URL is whitelisted AND user hasn't disabled it for this screen
        syncEnabled = userSyncEnabled && VideoType.isSyncEnabledForURL(url);
    }

    /**
     * Called when URL changes to reset sync state.
     */
    public void resetSyncState() {
        syncMasterUUID = null;
        syncPlaybackTime = 0.0;
        syncPaused = false;
        syncUpdateTimestamp = 0;
        lastSyncSentTime = 0;
        initialSyncDone = false;
        syncStateRequested = false;
        initialSyncReceivedTime = 0;
        syncAttemptCount = 0;
        lastSyncAttemptTime = 0;
        updateSyncEnabled();
    }

    /**
     * Update sync state from master's broadcast.
     */
    public void updateSyncState(UUID masterUUID, double playbackTime, boolean paused) {
        this.syncMasterUUID = masterUUID;
        this.syncPlaybackTime = playbackTime;
        this.syncPaused = paused;
        this.syncUpdateTimestamp = System.currentTimeMillis();
    }

    /**
     * Get the interpolated playback time, accounting for time elapsed since last sync.
     */
    public double getInterpolatedPlaybackTime() {
        if (syncPaused || syncUpdateTimestamp == 0) {
            return syncPlaybackTime;
        }
        long elapsed = System.currentTimeMillis() - syncUpdateTimestamp;
        return syncPlaybackTime + (elapsed / 1000.0);
    }

    /**
     * Check if this client is the sync master for this screen.
     * @param playerUUID The local player's UUID
     */
    public boolean isSyncMaster(UUID playerUUID) {
        return syncMasterUUID != null && syncMasterUUID.equals(playerUUID);
    }

    /**
     * Check if client should seek to sync position (if difference is too large).
     * @param currentTime The current playback time from the local browser
     * @return The time to seek to, or -1 if no seek needed
     */
    public double shouldSyncSeek(double currentTime) {
        if (!syncEnabled || syncMasterUUID == null) {
            return -1;
        }
        double targetTime = getInterpolatedPlaybackTime();
        double tolerance = CommonConfig.VideoSync.syncToleranceSeconds;
        if (Math.abs(currentTime - targetTime) > tolerance) {
            return targetTime;
        }
        return -1;
    }
}
