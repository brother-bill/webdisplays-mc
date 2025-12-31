/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.client.gui.GuiKeyboard;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.math.Vector3i;

public class KeyboardData extends GuiData {
    public Vector3i pos;
    public BlockSide side;
    public int kbX;
    public int kbY;
    public int kbZ;

    public KeyboardData() {
    }

    public KeyboardData(ScreenBlockEntity tes, BlockSide side, BlockPos kbPos) {
        pos = new Vector3i(tes.getBlockPos());
        this.side = side;
        kbX = kbPos.getX();
        kbY = kbPos.getY();
        kbZ = kbPos.getZ();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Screen createGui(Screen old, Level world) {
        BlockEntity te = world.getBlockEntity(pos.toBlock());
        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Log.error("TileEntity at %s is not a screen; can't open keyboard!", pos.toString());
            return null;
        }

        return new GuiKeyboard((ScreenBlockEntity) te, side, new BlockPos(kbX, kbY, kbZ));
    }

    @Override
    public String getName() {
        return "Keyboard";
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeInt(pos.x);
        buf.writeInt(pos.y);
        buf.writeInt(pos.z);
        buf.writeByte(side.ordinal());
        buf.writeInt(kbX);
        buf.writeInt(kbY);
        buf.writeInt(kbZ);
    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        this.pos = new Vector3i(buf.readInt(), buf.readInt(), buf.readInt());
        this.side = BlockSide.values()[buf.readByte()];
        this.kbX = buf.readInt();
        this.kbY = buf.readInt();
        this.kbZ = buf.readInt();
    }
}
