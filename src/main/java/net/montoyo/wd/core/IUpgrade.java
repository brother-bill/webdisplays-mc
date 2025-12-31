/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface IUpgrade {
    void onInstall(@Nonnull ScreenBlockEntity tes, @Nonnull BlockSide screenSide, @Nullable Player player, @Nonnull ItemStack is);
    boolean onRemove(@Nonnull ScreenBlockEntity tes, @Nonnull BlockSide screenSide, @Nullable Player player, @Nonnull ItemStack is); //Return true to prevent dropping
    boolean isSameUpgrade(@Nonnull ItemStack myStack, @Nonnull ItemStack otherStack); //myStack.getItem() is an instance of this class
    String getJSName(@Nonnull ItemStack is); //modname:upgradename
}
