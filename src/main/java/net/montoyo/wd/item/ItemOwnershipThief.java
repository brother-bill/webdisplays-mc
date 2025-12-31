/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemOwnershipThief extends Item implements WDItem {
    public ItemOwnershipThief(Properties properties) {
        super(properties
                        .stacksTo(1)
//                .tab(WebDisplays.CREATIVE_TAB)
        );
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer().isShiftKeyDown())
            return InteractionResult.PASS;

        if (context.getLevel().isClientSide)
            return InteractionResult.SUCCESS;

        if (CommonConfig.disableOwnershipThief) {
            Util.toast(context.getPlayer(), "otDisabled");
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getPlayer().getItemInHand(context.getHand());
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.isEmpty()) {
            if (tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ") && tag.contains("Side")) {
                BlockPos bp = new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"));
                BlockSide side = BlockSide.values()[tag.getByte("Side")];

                if (!(context.getLevel().getBlockState(bp).getBlock() instanceof ScreenBlock))
                    return InteractionResult.SUCCESS;

                BlockEntity te = context.getLevel().getBlockEntity(bp);
                if (te == null || !(te instanceof ScreenBlockEntity))
                    return InteractionResult.SUCCESS;

                ScreenBlockEntity tes = (ScreenBlockEntity) te;
                ScreenData scr = tes.getScreen(side);
                if(scr == null)
                    return InteractionResult.SUCCESS;

                Log.warning("Owner of screen at %d %d %d, side %s was changed from %s (UUID %s) to %s (UUID %s)", bp.getX(), bp.getY(), bp.getZ(), side.toString(), scr.owner.name, scr.owner.uuid.toString(), context.getPlayer().getName(), context.getPlayer().getGameProfile().getId().toString());
                context.getPlayer().setItemInHand(context.getHand(), ItemStack.EMPTY);
                tes.setOwner(side, context.getPlayer());
                Util.toast(context.getPlayer(), ChatFormatting.AQUA, "newOwner");
                return InteractionResult.SUCCESS;
            }
        }

        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof ScreenBlock))
            return InteractionResult.SUCCESS;

        Vector3i pos = new Vector3i(context.getClickedPos());
        BlockSide side = BlockSide.values()[context.getClickedFace().ordinal()];
        Multiblock.findOrigin(context.getLevel(), pos, side, null);

        BlockEntity te = context.getLevel().getBlockEntity(pos.toBlock());
        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Util.toast(context.getPlayer(), "turnOn");
            return InteractionResult.SUCCESS;
        }

        if (((ScreenBlockEntity) te).getScreen(side) == null)
            Util.toast(context.getPlayer(), "turnOn");
        else {
            CompoundTag newTag = new CompoundTag();
            newTag.putInt("PosX", pos.x);
            newTag.putInt("PosY", pos.y);
            newTag.putInt("PosZ", pos.z);
            newTag.putByte("Side", (byte) side.ordinal());

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
            Util.toast(context.getPlayer(), ChatFormatting.AQUA, "screenSet");
            Log.warning("Player %s (UUID %s) created an Ownership Thief item for screen at %d %d %d, side %s!", context.getPlayer().getName(), context.getPlayer().getGameProfile().getId().toString(), pos.x, pos.y, pos.z, side.toString());
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public String getWikiName(@Nonnull ItemStack is) {
        return "Ownership_Thief";
    }
}
