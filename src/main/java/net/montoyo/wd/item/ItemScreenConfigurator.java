/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.math.Vector3i;
import org.jetbrains.annotations.NotNull;

public class ItemScreenConfigurator extends Item implements WDItem {
    public ItemScreenConfigurator(Properties properties) {
        super(properties
                        .stacksTo(1)
//                .tab(WebDisplays.CREATIVE_TAB)
        );
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer().isShiftKeyDown() || !(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof ScreenBlock))
            return InteractionResult.PASS;

        if (context.getLevel().isClientSide)
            return InteractionResult.SUCCESS;

        Vector3i origin = new Vector3i(context.getClickedPos());
        BlockSide side = BlockSide.values()[context.getClickedFace().ordinal()];

        Multiblock.findOrigin(context.getLevel(), origin, side, null);
        BlockEntity te = context.getLevel().getBlockEntity(origin.toBlock());

        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Util.toast(context.getPlayer(), "turnOn");
            return InteractionResult.SUCCESS;
        }

        ScreenData scr = ((ScreenBlockEntity) te).getScreen(side);
        if(scr == null)
            Util.toast(context.getPlayer(), "turnOn");
        else
            (new ScreenConfigData(origin, side, scr)).sendTo((ServerPlayer) context.getPlayer());

        return InteractionResult.SUCCESS;
    }

    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
