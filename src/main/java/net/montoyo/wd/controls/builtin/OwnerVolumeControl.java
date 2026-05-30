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
 * Owner-set master volume for a screen, 0..100. Multiplied into the final spatial
 * audio volume in {@link ScreenBlockEntity#updateTrackDistance}. Permission gated on
 * {@link ScreenRights#MANAGE_UPGRADES} to match the existing autoVolume permission.
 */
public class OwnerVolumeControl extends ScreenControl {
    public static final ResourceLocation id = ResourceLocation.parse("webdisplays:owner_volume");

    int volume;

    public OwnerVolumeControl(int volume) {
        super(id);
        this.volume = Math.max(0, Math.min(100, volume));
    }

    public OwnerVolumeControl(FriendlyByteBuf buf) {
        super(id);
        volume = Math.max(0, Math.min(100, buf.readVarInt()));
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(volume);
    }

    @Override
    public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx,
                             Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
        checkPerms(ScreenRights.MANAGE_UPGRADES, permissionChecker, (net.minecraft.server.level.ServerPlayer) ctx.player());
        tes.setOwnerVolume(side, volume);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
        tes.setOwnerVolume(side, volume);
    }
}
