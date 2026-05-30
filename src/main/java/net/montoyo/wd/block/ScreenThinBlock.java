package net.montoyo.wd.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thin (1/16-deep) variant of {@link ScreenBlock}. Inherits all multiblock, BE,
 * interaction, and rendering behavior from its parent — the only differences are
 * visual: a {@code facing} blockstate property tracks which face the screen sits
 * on, and the VoxelShape is a thin slab against that face.
 *
 * <p>FACING is set at placement time from the face the player clicked: the clicked
 * face becomes the "back" (the surface the thin block is mounted against), so the
 * screen face — opposite — points outward. Users should place all thin blocks of
 * a multiblock with the same FACING and use the linker on the FACING-opposite
 * side; otherwise the screen quad (positioned at the standard z=0.52 by
 * {@code ScreenStageRenderer}) may not align with the thin geometry.
 *
 * <p>Multiblock connectivity inherits from {@link ScreenBlock} via
 * {@code Multiblock.isScreenBlock()}, so thin blocks can connect to other thin
 * blocks (and, as a V1 caveat, to thick screen blocks). FACING is not checked by
 * the multiblock logic; mismatched facings simply look broken visually.
 */
public class ScreenThinBlock extends ScreenBlock {
    public static final MapCodec<ScreenThinBlock> CODEC = simpleCodec(ScreenThinBlock::new);
    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.values());

    private static final VoxelShape SHAPE_DOWN  = Block.box(0, 15, 0, 16, 16, 16); // mounted on top of block below
    private static final VoxelShape SHAPE_UP    = Block.box(0,  0, 0, 16,  1, 16); // mounted on floor
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 0, 15, 16, 16, 16); // mounted on south wall (facing north)
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 0,  0, 16, 16,  1);
    private static final VoxelShape SHAPE_WEST  = Block.box(15, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST  = Block.box(0,  0, 0,  1, 16, 16);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ScreenThinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(hasTE, false)
                .setValue(emitting, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // The clicked face is what the block is mounted on — its opposite is where
        // the screen "looks." We store the screen-facing direction in FACING.
        Direction screenFace = ctx.getClickedFace();
        return this.defaultBlockState().setValue(FACING, screenFace);
    }

    @Override
    @NotNull
    public VoxelShape getShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return switch (state.getValue(FACING)) {
            case DOWN -> SHAPE_DOWN;
            case UP -> SHAPE_UP;
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
        };
    }

    @Override
    @NotNull
    public VoxelShape getCollisionShape(BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext ctx) {
        return getShape(state, level, pos, ctx);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(hasTE, emitting, FACING);
    }
}
