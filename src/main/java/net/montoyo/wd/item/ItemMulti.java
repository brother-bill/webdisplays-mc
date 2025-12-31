/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.world.item.Item;

import java.util.BitSet;

public class ItemMulti extends Item {
    protected final Enum[] values;
    protected final BitSet creativeTabItems;

    public ItemMulti(Class<? extends Enum> cls, Properties properties) {
        super(properties);
        values = cls.getEnumConstants();
        creativeTabItems = new BitSet(values.length);
        creativeTabItems.set(0, values.length);
    }

    public Enum[] getEnumValues() {
        return values;
    }
}
