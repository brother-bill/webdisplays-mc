/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.core.CraftComponent;
import org.jetbrains.annotations.NotNull;

public class ItemCraftComponent extends ItemMulti implements WDItem {
    public ItemCraftComponent(Properties properties) {
        super(CraftComponent.class, properties
//                .tab(WebDisplays.CREATIVE_TAB)
        );

        //Hide the bad extension card from the creative tab
        creativeTabItems.clear(CraftComponent.BADEXTCARD.ordinal());
    }

    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
