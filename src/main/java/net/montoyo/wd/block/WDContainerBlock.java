/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.BaseEntityBlock;

public abstract class WDContainerBlock extends BaseEntityBlock {

    protected static BlockItem itemBlock;

    public WDContainerBlock(Properties arg) {
        super(arg);
    }

    public BlockItem getItem() {
        return itemBlock;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // This is a simple implementation - the actual codec is not needed for runtime
        // since we don't serialize blocks this way
        throw new UnsupportedOperationException("Codec not supported for WDContainerBlock");
    }
}
