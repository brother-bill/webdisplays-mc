/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.ServerData;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.serialization.Util;

public class ServerBlockEntity extends BlockEntity {
    private NameUUIDPair owner;

    public ServerBlockEntity(BlockPos arg2, BlockState arg3) {
        super(TileRegistry.SERVER.get(), arg2, arg3);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = Util.readOwnerFromNBT(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        Util.writeOwnerToNBT(tag, owner);
    }

    public void setOwner(Player ep) {
        owner = new NameUUIDPair(ep.getGameProfile());
        setChanged();
    }

    public void onPlayerRightClick(Player ply) {
        if (level.isClientSide)
            return;

        if (WebDisplays.INSTANCE.miniservPort == 0)
            Util.toast(ply, "noMiniserv");
        else if (owner != null && ply instanceof ServerPlayer)
            (new ServerData(getBlockPos(), owner)).sendTo((ServerPlayer) ply);
    }
}
