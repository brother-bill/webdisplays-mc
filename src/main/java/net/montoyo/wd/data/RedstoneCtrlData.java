/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.client.gui.GuiRedstoneCtrl;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.utilities.math.Vector3i;

public class RedstoneCtrlData extends GuiData {
    public ResourceLocation dimension;
    public Vector3i pos;
    public String risingEdgeURL;
    public String fallingEdgeURL;

    public RedstoneCtrlData() {
        super();
    }

    public RedstoneCtrlData(ResourceLocation d, BlockPos p, String r, String f) {
        dimension = d;
        pos = new Vector3i(p);
        risingEdgeURL = r;
        fallingEdgeURL = f;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Screen createGui(Screen old, Level world) {
        return new GuiRedstoneCtrl(old.getTitle(), dimension, pos, risingEdgeURL, fallingEdgeURL); //TODO is getTitle() correct?
    }

    @Override
    public String getName() {
        return "RedstoneCtrl";
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(dimension.toString());
        BufferUtils.writeVec3i(buf, pos);
        buf.writeUtf(risingEdgeURL);
        buf.writeUtf(fallingEdgeURL);
    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        dimension = ResourceLocation.parse(buf.readUtf());
        pos = BufferUtils.readVec3i(buf);
        risingEdgeURL = buf.readUtf();
        fallingEdgeURL = buf.readUtf();
    }
}
