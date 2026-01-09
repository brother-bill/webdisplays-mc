package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

/**
 * Network control for toggling per-screen video sync enabled/disabled.
 * Allows users to disable sync for a specific screen if it causes issues.
 */
public class SyncEnabledControl extends ScreenControl {
    public static final ResourceLocation id = ResourceLocation.parse("webdisplays:sync_enabled");

    boolean syncEnabled;

    public SyncEnabledControl(boolean syncEnabled) {
        super(id);
        this.syncEnabled = syncEnabled;
    }

    public SyncEnabledControl(FriendlyByteBuf buf) {
        super(id);
        syncEnabled = buf.readBoolean();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(syncEnabled);
    }

    @Override
    public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
        // Use same permission as auto-volume (manage upgrades) since it's a similar screen setting
        checkPerms(ScreenRights.MANAGE_UPGRADES, permissionChecker, (net.minecraft.server.level.ServerPlayer) ctx.player());
        tes.setSyncEnabled(side, syncEnabled);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
        tes.setSyncEnabled(side, syncEnabled);
    }
}
