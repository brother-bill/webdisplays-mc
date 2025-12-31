/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.item.ItemUpgrade;

public enum DefaultUpgrade {
    LASERMOUSE("lasermouse", "LaserMouse"),
    REDINPUT("redinput", "RedInput"),
    REDOUTPUT("redoutput", "RedOutput"),
    GPS("gps", "GPS");
    
    public final String name;
    public final String wikiName;

    DefaultUpgrade(String n, String wn) {
        name = n;
        wikiName = wn;
    }

    @Override
    public String toString() {
        return name;
    }

    protected static boolean matches(ItemStack stack, DefaultUpgrade upgrade) {
        if (stack.getItem() instanceof ItemUpgrade upgrade1)
            return upgrade1.type == upgrade;
        return false;
    }
    
    public boolean matchesLaserMouse(ItemStack is) {
        return matches(is, LASERMOUSE);
    }

    public boolean matchesRedInput(ItemStack is) {
        return matches(is, REDINPUT);
    }

    public boolean matchesRedOutput(ItemStack is) {
        return matches(is, REDOUTPUT);
    }

    public boolean matchesGps(ItemStack is) {
        return matches(is, GPS);
    }
}
