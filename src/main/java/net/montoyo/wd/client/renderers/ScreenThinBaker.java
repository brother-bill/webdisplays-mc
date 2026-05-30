package net.montoyo.wd.client.renderers;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.montoyo.wd.block.ScreenThinBlock;
import net.montoyo.wd.utilities.math.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Custom baked model for {@link ScreenThinBlock}.
 *
 * <p>For each block, bakes one front quad on the slab's outer face (the visible screen face)
 * and one back quad on the mount side. The front quad uses a sub-tile of the 16-tile
 * {@code screen0..screen15} atlas selected based on multiblock neighbor topology — corners,
 * edges, and inner blocks each get the appropriate piece so bezels don't show at seams.
 *
 * <h2>Viewer-perspective coordinate system</h2>
 *
 * Earlier versions used {@link net.montoyo.wd.utilities.data.BlockSide}'s {@code up/right}
 * vectors directly for both topology and rendering. Those are screen-local axes that don't
 * always match the viewer's left/up — and the texture has no rotation, so the mismatch
 * caused bezels to show on inner edges of vertical multiblock stacks even though they were
 * correctly suppressed on horizontal stacks (the 180-deg flip on horizontal happened to
 * cancel out for NORTH but not vertical). This version uses explicit viewer-perspective
 * right/up vectors per FACING — both topology bits AND UV mapping use the same axes, so
 * everything lines up regardless of facing.
 */
public class ScreenThinBaker implements BakedModel {

    private static final List<BakedQuad> noQuads = ImmutableList.of();

    private final TextureAtlasSprite[] texs = new TextureAtlasSprite[16];

    public static final ModelProperty<Integer> TOPOLOGY = new ModelProperty<>();

    public ScreenThinBaker(ModelState modelState,
                           Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> spriteGetter,
                           ItemOverrides overrides, ItemTransforms itemTransforms) {
        for (int i = 0; i < texs.length; i++) {
            texs[i] = spriteGetter.apply(ScreenModelLoader.MATERIALS_SIDES[i]);
        }
    }

    /** Viewer's right-hand axis in world coords when looking at the screen from outside. */
    private static Vector3i viewerRight(Direction facing) {
        return switch (facing) {
            case NORTH -> new Vector3i(1, 0, 0);    // facing N (-Z), viewer right = east (+X)
            case SOUTH -> new Vector3i(-1, 0, 0);   // facing S (+Z), viewer right = west (-X)
            case EAST  -> new Vector3i(0, 0, 1);    // facing E (+X), viewer right = south (+Z)
            case WEST  -> new Vector3i(0, 0, -1);   // facing W (-X), viewer right = north (-Z)
            case UP    -> new Vector3i(1, 0, 0);    // floor display, viewer looks down: right = east
            case DOWN  -> new Vector3i(1, 0, 0);    // ceiling display, viewer looks up:  right = east
        };
    }

    /** Viewer's up axis in world coords. */
    private static Vector3i viewerUp(Direction facing) {
        return switch (facing) {
            case NORTH, SOUTH, EAST, WEST -> new Vector3i(0, 1, 0);
            case UP   -> new Vector3i(0, 0, -1); // looking down, "up" in 2D is north (-Z)
            case DOWN -> new Vector3i(0, 0, 1);  // looking up,   "up" in 2D is south (+Z)
        };
    }

    private static void putVertex(int[] buf, int pos, float x, float y, float z,
                                   TextureAtlasSprite tex, float u, float v, Direction normal) {
        pos *= 8;
        buf[pos] = Float.floatToRawIntBits(x);
        buf[pos + 1] = Float.floatToRawIntBits(y);
        buf[pos + 2] = Float.floatToRawIntBits(z);
        buf[pos + 3] = 0xFFFFFFFF;
        buf[pos + 4] = Float.floatToRawIntBits(tex.getU(u / 16.0f));
        buf[pos + 5] = Float.floatToRawIntBits(tex.getV(v / 16.0f));
        int nx = (normal.getStepX() * 127) & 0xFF;
        int ny = (normal.getStepY() * 127) & 0xFF;
        int nz = (normal.getStepZ() * 127) & 0xFF;
        buf[pos + 7] = nx | (ny << 8) | (nz << 16);
    }

    /**
     * Compute the 4 corners (TL, BL, BR, TR in viewer perspective) of the slab's outer
     * (visible) face. Slab outer face = cell center + 0.4375 along FACING.opposite() axis
     * (= 7/16 from cell center toward the mount side, i.e. 1/16 inset from the mount face).
     */
    private float[][] frontFaceCorners(Direction facing) {
        Vector3i right = viewerRight(facing);
        Vector3i up = viewerUp(facing);
        // Outer face center: 0.4375 (=7/16) from cell center along FACING.opposite() axis.
        Direction opp = facing.getOpposite();
        float cx = 0.5f + 0.4375f * opp.getStepX();
        float cy = 0.5f + 0.4375f * opp.getStepY();
        float cz = 0.5f + 0.4375f * opp.getStepZ();
        // TL=(-r,+u), BL=(-r,-u), BR=(+r,-u), TR=(+r,+u), magnitude 0.5 each.
        float[][] c = new float[4][3];
        int[][] sign = {{-1, 1}, {-1, -1}, {1, -1}, {1, 1}};
        for (int i = 0; i < 4; i++) {
            c[i][0] = cx + 0.5f * sign[i][0] * right.x + 0.5f * sign[i][1] * up.x;
            c[i][1] = cy + 0.5f * sign[i][0] * right.y + 0.5f * sign[i][1] * up.y;
            c[i][2] = cz + 0.5f * sign[i][0] * right.z + 0.5f * sign[i][1] * up.z;
        }
        return c;
    }

    /** Back face on the FACING-opposite cell face (against the mount block). */
    private float[][] backFaceCorners(Direction facing) {
        Vector3i right = viewerRight(facing);
        Vector3i up = viewerUp(facing);
        Direction opp = facing.getOpposite();
        // Back face center: at the FACING-opposite cell face (0.5 from center).
        float cx = 0.5f + 0.5f * opp.getStepX();
        float cy = 0.5f + 0.5f * opp.getStepY();
        float cz = 0.5f + 0.5f * opp.getStepZ();
        // Wound CCW from the back side (= viewed from FACING-opposite direction).
        // From the back side, right and up flip horizontally — so swap right sign per corner.
        float[][] c = new float[4][3];
        int[][] sign = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
        for (int i = 0; i < 4; i++) {
            c[i][0] = cx + 0.5f * sign[i][0] * right.x + 0.5f * sign[i][1] * up.x;
            c[i][1] = cy + 0.5f * sign[i][0] * right.y + 0.5f * sign[i][1] * up.y;
            c[i][2] = cz + 0.5f * sign[i][0] * right.z + 0.5f * sign[i][1] * up.z;
        }
        return c;
    }

    private BakedQuad bakeFrontFace(Direction facing, TextureAtlasSprite tex) {
        int[] data = new int[8 * 4];
        float[][] c = frontFaceCorners(facing);
        // CCW from viewer: TL(0,0) → BL(0,16) → BR(16,16) → TR(16,0).
        putVertex(data, 0, c[0][0], c[0][1], c[0][2], tex, 0,  0,  facing);
        putVertex(data, 1, c[1][0], c[1][1], c[1][2], tex, 0,  16, facing);
        putVertex(data, 2, c[2][0], c[2][1], c[2][2], tex, 16, 16, facing);
        putVertex(data, 3, c[3][0], c[3][1], c[3][2], tex, 16, 0,  facing);
        return new BakedQuad(data, 0xFFFFFFFF, facing, tex, true);
    }

    private BakedQuad bakeBackFace(Direction facing, TextureAtlasSprite tex) {
        int[] data = new int[8 * 4];
        Direction back = facing.getOpposite();
        float[][] c = backFaceCorners(facing);
        putVertex(data, 0, c[0][0], c[0][1], c[0][2], tex, 0,  0,  back);
        putVertex(data, 1, c[1][0], c[1][1], c[1][2], tex, 0,  16, back);
        putVertex(data, 2, c[2][0], c[2][1], c[2][2], tex, 16, 16, back);
        putVertex(data, 3, c[3][0], c[3][1], c[3][2], tex, 16, 0,  back);
        return new BakedQuad(data, 0xFFFFFFFF, back, tex, true);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
        return getQuads(state, side, random, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                              @NotNull RandomSource rand, @NotNull ModelData data,
                                              @Nullable RenderType renderType) {
        if (state == null) {
            // Item context: don't emit quads — the item model (models/item/screen_thin.json)
            // is a regular cube_all model that doesn't go through this baker.
            return noQuads;
        }
        Direction facing = state.getValue(ScreenThinBlock.FACING);
        if (side == null) {
            int topology = data.has(TOPOLOGY) ? data.get(TOPOLOGY) : 15;
            List<BakedQuad> ret = new ArrayList<>(2);
            ret.add(bakeFrontFace(facing, texs[topology]));
            ret.add(bakeBackFace(facing, texs[15]));
            return ret;
        }
        return noQuads;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos,
                                            @NotNull BlockState state, @NotNull ModelData modelData) {
        Direction facing = state.getValue(ScreenThinBlock.FACING);
        Vector3i right = viewerRight(facing);
        Vector3i up = viewerUp(facing);

        // Bit encoding matches the screen0..screen15 atlas convention used by ScreenBaker.
        final int BAR_BOTTOM = 1, BAR_RIGHT = 2, BAR_TOP = 4, BAR_LEFT = 8;
        int bars = 0;

        // For each direction in viewer's plane, a bar is present when there is NO same-FACING
        // neighbor on that side (= this block is on the edge in that direction).
        if (!sameThinNeighbor(level, pos, state, up))                                        bars |= BAR_TOP;
        if (!sameThinNeighbor(level, pos, state, new Vector3i(-up.x, -up.y, -up.z)))         bars |= BAR_BOTTOM;
        if (!sameThinNeighbor(level, pos, state, right))                                     bars |= BAR_RIGHT;
        if (!sameThinNeighbor(level, pos, state, new Vector3i(-right.x, -right.y, -right.z))) bars |= BAR_LEFT;

        return ModelData.builder().with(TOPOLOGY, bars).build();
    }

    private boolean sameThinNeighbor(BlockAndTintGetter level, BlockPos pos, BlockState state, Vector3i dir) {
        BlockState n = level.getBlockState(pos.offset(dir.x, dir.y, dir.z));
        if (!(n.getBlock() instanceof ScreenThinBlock)) return false;
        // Only count as a "screen neighbor" if it has the same FACING — otherwise it's a
        // separate visual unit and the bezel SHOULD appear at this edge.
        return n.getValue(ScreenThinBlock.FACING) == state.getValue(ScreenThinBlock.FACING);
    }

    @Override public boolean useAmbientOcclusion()    { return true; }
    @Override public boolean isGui3d()                { return true; }
    @Override public boolean usesBlockLight()         { return false; }
    @Override public boolean isCustomRenderer()       { return false; }
    @Override @Nonnull public TextureAtlasSprite getParticleIcon() { return texs[15]; }
    @Override @Nonnull public ItemTransforms getTransforms()       { return ItemTransforms.NO_TRANSFORMS; }
    @Override @Nonnull public ItemOverrides getOverrides()         { return ItemOverrides.EMPTY; }
}
