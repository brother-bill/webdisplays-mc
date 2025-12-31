/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.client.gui.GuiServer;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.math.Vector3i;

public class ServerData extends GuiData {
    public Vector3i pos;
    public NameUUIDPair owner;

    public ServerData() {
    }

    public ServerData(BlockPos bp, NameUUIDPair owner) {
        pos = new Vector3i(bp);
        this.owner = owner;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Screen createGui(Screen old, Level world) {
        return new GuiServer(pos, owner);
    }

    @Override
    public String getName() {
        return "Server";
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        BufferUtils.writeVec3i(buf, pos);
        owner.writeTo(buf);
    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        pos = BufferUtils.readVec3i(buf);
        owner = new NameUUIDPair(buf);
    }
}
