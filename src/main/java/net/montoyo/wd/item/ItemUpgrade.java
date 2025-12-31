/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemUpgrade extends ItemMulti implements IUpgrade, WDItem {
    public final DefaultUpgrade type;

    public ItemUpgrade(DefaultUpgrade type) {
        super(DefaultUpgrade.class, new Properties()/*.tab(WebDisplays.CREATIVE_TAB)*/);
        this.type = type;
    }

    @Override
    public void onInstall(@Nonnull ScreenBlockEntity tes, @Nonnull BlockSide screenSide, @Nullable Player player, @Nonnull ItemStack is) {
    }

    @Override
    public boolean onRemove(@Nonnull ScreenBlockEntity tes, @Nonnull BlockSide screenSide, @Nullable Player player, @Nonnull ItemStack is) {
        if (DefaultUpgrade.LASERMOUSE.matchesLaserMouse(is))
            tes.clearLaserUser(screenSide);

        return false;
    }

    @Override
    public boolean isSameUpgrade(@Nonnull ItemStack myStack, @Nonnull ItemStack otherStack) {
        if (myStack.getItem() instanceof ItemUpgrade upgrade0) {
            if (otherStack.getItem() instanceof ItemUpgrade upgrade1) {
                return upgrade0.type == upgrade1.type;
            }
        }
        return false;
    }

    @Override
    public String getJSName(@Nonnull ItemStack is) {
        Item item = is.getItem();
        if (item instanceof ItemUpgrade upgrade)
            return "webdisplays:" + upgrade.type.toString();
        return "webdisplays:null";
    }

    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
