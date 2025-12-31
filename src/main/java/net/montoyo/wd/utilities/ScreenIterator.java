/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraft.core.BlockPos;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.Iterator;

public final class ScreenIterator implements Iterator<BlockPos> {
    private final Vector3i from, to;
    private final BlockSide side;
    private final Vector2i size;
    private final BlockPos.MutableBlockPos blockPos;
    private int x = 0;
    private int y = 0;
    private boolean hasNext = true;

    public ScreenIterator(BlockPos pos, BlockSide side, Vector2i size) {
        from = new Vector3i(pos);
        to = from.clone();
        this.side = side;
        this.size = size;
        blockPos = new BlockPos.MutableBlockPos();
    }

    @Override
    public boolean hasNext() {
        return hasNext;
    }

    @Override
    public BlockPos next() {
        to.toBlock(blockPos);

        if (++x >= size.x) {
            if (++y >= size.y)
                hasNext = false;
            else {
                x = 0;
                to.set(from.add(side.up));
            }
        } else
            to.add(side.right);

        return blockPos;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getIndex() {
        return y * size.x + x;
    }
}
