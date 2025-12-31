/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import com.mojang.serialization.MapCodec;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.item.ItemLaserPointer;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import org.jetbrains.annotations.NotNull;

public class ScreenBlock extends BaseEntityBlock {
    public static final MapCodec<ScreenBlock> CODEC = simpleCodec(ScreenBlock::new);
    public static final BooleanProperty hasTE = BooleanProperty.create("haste");
    public static final BooleanProperty emitting = BooleanProperty.create("emitting");
    private static final Property<?>[] properties = new Property<?>[]{hasTE, emitting};

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ScreenBlock(Properties properties) {
        super(properties.strength(1.5f, 10.f));
        this.registerDefaultState(this.defaultBlockState().setValue(hasTE, false).setValue(emitting, false));
    }

    @Override
    public void onRemove(BlockState p_60515_, Level p_60516_, BlockPos p_60517_, BlockState p_60518_, boolean p_60519_) {
        // TODO: make this also get called on client?
        if (p_60518_.getBlock() == p_60515_.getBlock()) return;

        for (BlockSide value : BlockSide.values()) {
            Vector3i vec = new Vector3i(p_60517_.getX(), p_60517_.getY(), p_60517_.getZ());
            Multiblock.findOrigin(p_60516_, vec, value, null);
            BlockPos bp = new BlockPos(vec.x, vec.y, vec.z);
            if (!bp.equals(p_60517_)) {
                p_60516_.removeBlockEntity(bp);
                p_60516_.setBlock(
                        bp, p_60516_.getBlockState(bp).setValue(hasTE, false),
                        11
                );
            }
        }

        super.onRemove(p_60515_, p_60516_, p_60517_, p_60518_, p_60519_);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItemStack, BlockState state, Level world, BlockPos position, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = heldItemStack;
        boolean isUpgrade = false;
        if (heldItem.isEmpty())
            heldItem = null; //Easier to work with
        else if (!(isUpgrade = heldItem.getItem() instanceof IUpgrade))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        else if (heldItem.getItem() instanceof ItemLaserPointer)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION; // laser pointer already handles stuff

        // handling the off hand leads to double clicking
        if (!isUpgrade && hand == InteractionHand.OFF_HAND)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (world.isClientSide)
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        boolean sneaking = player.isShiftKeyDown();
        Vector3i pos = new Vector3i(position);

        BlockSide side = BlockSide.values()[hit.getDirection().ordinal()];

        Multiblock.findOrigin(world, pos, side, null);
        ScreenBlockEntity te = (ScreenBlockEntity) world.getBlockEntity(pos.toBlock());

        if (te != null && te.getScreen(side) != null) {
            ScreenData scr = te.getScreen(side);

            if (sneaking) { //Right Click
                if ((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
                    Util.toast(player, "restrictions");
                else
                    (new SetURLData(pos, scr.side, scr.url)).sendTo((ServerPlayer) player);

                return ItemInteractionResult.SUCCESS;
            } else if (heldItem != null) {
                if (!te.hasUpgrade(side, heldItem)) {
                    if ((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0) {
                        Util.toast(player, "restrictions");
                        return ItemInteractionResult.CONSUME;
                    }

                    if (te.addUpgrade(side, heldItem, player, false)) {
                        if (!player.isCreative())
                            heldItem.shrink(1);

                        Util.toast(player, ChatFormatting.AQUA, "upgradeOk");
                        if (player instanceof ServerPlayer)
                            WebDisplays.CRITERION_UPGRADE_SCREEN.get().trigger((ServerPlayer) player);
                    } else
                        Util.toast(player, "upgradeError");

                    return ItemInteractionResult.CONSUME;
                }
            } else {
                if ((scr.rightsFor(player) & ScreenRights.INTERACT) == 0) {
                    Util.toast(player, "restrictions");
                    return ItemInteractionResult.CONSUME;
                }

                Vector2i tmp = new Vector2i();

                float hitX = ((float) hit.getLocation().x) - (float) te.getBlockPos().getX();
                float hitY = ((float) hit.getLocation().y) - (float) te.getBlockPos().getY();
                float hitZ = ((float) hit.getLocation().z) - (float) te.getBlockPos().getZ();

                if (hit2pixels(side, hit.getBlockPos(), new Vector3i(hit.getBlockPos()), scr, hitX, hitY, hitZ, tmp))
                    te.click(side, tmp);
                return ItemInteractionResult.CONSUME;
            }
        }
//        else if(sneaking) {
//            Util.toast(player, "turnOn");
//            return ItemInteractionResult.SUCCESS;
//        }

        Vector2i size = Multiblock.measure(world, pos, side);
        if (size.x < 2 && size.y < 2) {
            Util.toast(player, "tooSmall");
            return ItemInteractionResult.SUCCESS;
        }

        if (size.x > CommonConfig.Screen.maxScreenSizeX || size.y > CommonConfig.Screen.maxScreenSizeY) {
            Util.toast(player, "tooBig", CommonConfig.Screen.maxScreenSizeX, CommonConfig.Screen.maxScreenSizeY);
            return ItemInteractionResult.SUCCESS;
        }

        Vector3i err = Multiblock.check(world, pos, size, side);
        if (err != null) {
            Util.toast(player, "invalid", err.toString());
            return ItemInteractionResult.SUCCESS;
        }

        boolean created = false;
        Log.info("Player %s (UUID %s) created a screen at %s of size %dx%d", player.getName(), player.getGameProfile().getId().toString(), pos.toString(), size.x, size.y);

        if (te == null) {
            BlockPos bp = pos.toBlock();
            world.setBlockAndUpdate(bp, world.getBlockState(bp).setValue(hasTE, true));
            te = (ScreenBlockEntity) world.getBlockEntity(bp);
            created = true;
        }

        te.addScreen(side, size, null, player, true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos source,
                                boolean isMoving) {
        if (block != this && !world.isClientSide && !state.getValue(emitting)) {
            for (BlockSide side : BlockSide.values()) {
                Vector3i vec = new Vector3i(pos);
                Multiblock.findOrigin(world, vec, side, null);

                ScreenBlockEntity tes = (ScreenBlockEntity) world.getBlockEntity(vec.toBlock());
                if (tes != null && tes.hasUpgrade(side, DefaultUpgrade.REDINPUT)) {
                    Direction facing = Direction.from2DDataValue(side.reverse().ordinal()); //Opposite face
                    vec.sub(pos.getX(), pos.getY(), pos.getZ()).neg();
//                    tes.updateJSRedstone(side, new Vector2i(vec.dot(side.right), vec.dot(side.up)), world.getSignal(pos, facing));
                }
            }
        }
    }
    
    public static boolean hit2pixels(BlockSide side, BlockPos bpos, Vector3i pos, ScreenData scr, float hitX, float hitY, float hitZ, Vector2i dst) {
        if(side.right.x < 0)
            hitX -= 1.f;

        if(side.right.z < 0 || side == BlockSide.TOP || side == BlockSide.BOTTOM)
            hitZ -= 1.f;

        Vector3f rel = new Vector3f(hitX, hitY, hitZ);

        // this dot is acting as a "get distance from plane" where the plane is the edge of the screen
        float cx = rel.dot(side.right.toFloat()) - 2.f / 16.f;
        float cy = rel.dot(side.up.toFloat()) - 2.f / 16.f;
        float sw = ((float) scr.size.x) - 4.f / 16.f;
        float sh = ((float) scr.size.y) - 4.f / 16.f;

        cx /= sw;
        cy /= sh;

        if (cx >= 0.f && cx <= 1.0 && cy >= 0.f && cy <= 1.f) {
            if (side != BlockSide.BOTTOM)
                cy = 1.f - cy;

            switch (scr.rotation) {
                case ROT_90:
                    cy = 1.0f - cy;
                    break;

                case ROT_180:
                    cx = 1.0f - cx;
                    cy = 1.0f - cy;
                    break;

                case ROT_270:
                    cx = 1.0f - cx;
                    break;
            }

            cx *= (float) scr.resolution.x;
            cy *= (float) scr.resolution.y;

            if (scr.rotation.isVertical) {
                dst.x = (int) cy;
                dst.y = (int) cx;
            } else {
                dst.x = (int) cx;
                dst.y = (int) cy;
            }

            return true;
        }

        return false;
    }

    /************************************************* DESTRUCTION HANDLING *************************************************/

    private void onDestroy(Level world, BlockPos pos, Player ply) {
        if (!world.isClientSide) {
            Vector3i bp = new Vector3i(pos);
            Multiblock.BlockOverride override = new Multiblock.BlockOverride(bp, Multiblock.OverrideAction.SIMULATE);

            for (BlockSide bs : BlockSide.values())
                destroySide(world, bp.clone(), bs, override, ply);
        }
    }

    private void destroySide(Level world, Vector3i pos, BlockSide side, Multiblock.BlockOverride override, Player
            source) {
        Multiblock.findOrigin(world, pos, side, override);
        BlockPos bp = pos.toBlock();
        BlockEntity te = world.getBlockEntity(bp);

        if (te instanceof ScreenBlockEntity) {
            ((ScreenBlockEntity) te).onDestroy(source);
            world.setBlock(bp, world.getBlockState(bp).setValue(hasTE, false), Block.UPDATE_ALL_IMMEDIATE); //Destroy tile entity.
        }
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player,
                                       boolean willHarvest, FluidState fluid) {
        onDestroy(level, pos, player);
        return super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
    }

    @Override
    public void setPlacedBy(Level world, @NotNull BlockPos pos, @NotNull BlockState
            state, @org.jetbrains.annotations.Nullable LivingEntity whoDidThisShit, @NotNull ItemStack stack) {
        if (world.isClientSide)
            return;

        Multiblock.BlockOverride override = new Multiblock.BlockOverride(new Vector3i(pos), Multiblock.OverrideAction.IGNORE);
        Vector3i[] neighbors = new Vector3i[6];

        neighbors[0] = new Vector3i(pos.getX() + 1, pos.getY(), pos.getZ());
        neighbors[1] = new Vector3i(pos.getX() - 1, pos.getY(), pos.getZ());
        neighbors[2] = new Vector3i(pos.getX(), pos.getY() + 1, pos.getZ());
        neighbors[3] = new Vector3i(pos.getX(), pos.getY() - 1, pos.getZ());
        neighbors[4] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() + 1);
        neighbors[5] = new Vector3i(pos.getX(), pos.getY(), pos.getZ() - 1);

        for (Vector3i neighbor : neighbors) {
            if (world.getBlockState(neighbor.toBlock()).getBlock() instanceof ScreenBlock) {
                for (BlockSide bs : BlockSide.values())
                    destroySide(world, neighbor.clone(), bs, override, (whoDidThisShit instanceof Player) ? ((Player) whoDidThisShit) : null);
            }
        }
    }

    /************************************************* STUFF THAT'S UNLIKELY TO BE TOUCHED BUT NEEDS TO BE HERE *************************************************/

    @Override
    public @NotNull PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.IGNORE;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(emitting) ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(emitting);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(hasTE) ? new ScreenBlockEntity(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(properties);
    }
}
