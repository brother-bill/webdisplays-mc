/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.WebDisplays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface WDItem {
    static void addInformation(@Nullable List<String> tt) {
        if (tt != null && WebDisplays.PROXY.isShiftDown())
            tt.add(ChatFormatting.GRAY + I18n.get("item.webdisplays.wiki"));
    }

    String getWikiName(@Nonnull ItemStack is);
}
