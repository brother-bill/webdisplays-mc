package net.montoyo.wd.block.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.block.KeyboardBlockLeft;
import net.montoyo.wd.registry.BlockRegistry;

public class KeyboardItem extends BlockItem {
    public KeyboardItem(Block arg, Properties arg2) {
        super(arg, arg2);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext arg, BlockState arg2) {
        Direction facing = arg.getHorizontalDirection();
        arg2 = arg2.setValue(KeyboardBlockLeft.FACING, facing);

        Direction d = KeyboardBlockLeft.mapDirection(facing);

        if (isValid(arg.getClickedPos(), arg.getLevel(), arg2, d)) {
            Block kbRight = BlockRegistry.blockKbRight.get();
            BlockState rightState = kbRight.defaultBlockState();

            rightState = rightState.setValue(KeyboardBlockLeft.FACING, facing);
            if (!arg.getLevel().setBlock(
                    arg.getClickedPos().relative(d),
                    rightState,
                    11
            )) return false;
            return arg.getLevel().setBlock(arg.getClickedPos(), arg2, 11);// 161
        } else if (isValid(arg.getClickedPos().relative(d.getOpposite(), 2), arg.getLevel(), arg2, d)) {
            Block kbRight = BlockRegistry.blockKbRight.get();
            BlockState rightState = kbRight.defaultBlockState();

            rightState = rightState.setValue(KeyboardBlockLeft.FACING, facing);
            if (!arg.getLevel().setBlock(
                    arg.getClickedPos(),
                    rightState,
                    11
            )) return false;
            return arg.getLevel().setBlock(arg.getClickedPos().relative(d.getOpposite()), arg2, 11);// 161
        }
        return false;
    }

    private boolean isValid(BlockPos pos, Level level, BlockState state, Direction d) {
        return level.getBlockState(pos.relative(d)).isAir();
    }
}
