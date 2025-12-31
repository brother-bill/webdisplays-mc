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
import net.montoyo.wd.client.gui.GuiSetURL2;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.math.Vector3i;

public class SetURLData extends GuiData {
    public Vector3i pos;
    public BlockSide side;
    public String url;
    public boolean isRemote;
    public Vector3i remoteLocation;

    public SetURLData() {
    }

    public SetURLData(Vector3i pos, BlockSide side, String url) {
        this.pos = pos;
        this.side = side;
        this.url = url;
        isRemote = false;
        remoteLocation = new Vector3i();
    }

    public SetURLData(Vector3i pos, BlockSide side, String url, BlockPos rl) {
        this.pos = pos;
        this.side = side;
        this.url = url;
        isRemote = true;
        remoteLocation = new Vector3i(rl);
    }


    @OnlyIn(Dist.CLIENT)
    @Override
    public Screen createGui(Screen old, Level world) {
        BlockEntity te = world.getBlockEntity(pos.toBlock());
        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Log.error("TileEntity at %s is not a screen; can't open gui!", pos.toString());
            return null;
        }

        return new GuiSetURL2((ScreenBlockEntity) te, side, url, isRemote ? remoteLocation : null);
    }

    @Override
    public String getName() {
        return "SetURL";
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        BufferUtils.writeVec3i(buf, pos);
        BufferUtils.writeEnum(buf, side, (byte) 1);
        buf.writeUtf(url);
        buf.writeBoolean(isRemote);
        if (isRemote) BufferUtils.writeVec3i(buf, remoteLocation);
    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        pos = BufferUtils.readVec3i(buf);
        side = (BlockSide) BufferUtils.readEnum(buf, (v) -> BlockSide.values()[v], (byte) 1);
        url = buf.readUtf();
        isRemote = buf.readBoolean();
        if (isRemote) remoteLocation = BufferUtils.readVec3i(buf);
        else remoteLocation = new Vector3i();
    }
}
