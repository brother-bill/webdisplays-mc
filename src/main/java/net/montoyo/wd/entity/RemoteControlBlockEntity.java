/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.utilities.serialization.Util;

public class RemoteControlBlockEntity extends AbstractPeripheralBlockEntity {
    public RemoteControlBlockEntity(BlockPos arg2, BlockState arg3) {
        super(TileRegistry.REMOTE_CONTROLLER.get(), arg2, arg3);
    }

    @Override
    public InteractionResult onRightClick(Player player, InteractionHand hand) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        if (!isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return InteractionResult.SUCCESS;
        }

        ScreenBlockEntity tes = getConnectedScreen();
        if (tes == null) {
            Util.toast(player, "notLinked");
            return InteractionResult.SUCCESS;
        }

        ScreenData scr = tes.getScreen(screenSide);
        if((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0) {
            Util.toast(player, "restrictions");
            return InteractionResult.SUCCESS;
        }

        (new SetURLData(screenPos, screenSide, scr.url, getBlockPos())).sendTo((ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }
}
