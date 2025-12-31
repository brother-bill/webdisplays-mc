/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.montoyo.wd.client.gui.GuiScreenConfig;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.math.Vector3i;

public class ScreenConfigData extends GuiData {
    public boolean onlyUpdate;
    public Vector3i pos;
    public BlockSide side;
    public NameUUIDPair[] friends;
    public int friendRights;
    public int otherRights;

    public ScreenConfigData() {
    }

	public ScreenConfigData(Vector3i pos, BlockSide side, ScreenData scr) {
		this.pos = pos;
		this.side = side;
		friends = scr.friends.toArray(new NameUUIDPair[0]);
		friendRights = scr.friendRights;
		otherRights = scr.otherRights;
		onlyUpdate = false;
	}
	
	@OnlyIn(Dist.CLIENT)
	@Override
	public Screen createGui(Screen old, Level world) {
		if (old != null && old instanceof GuiScreenConfig) {
			GuiScreenConfig gsc = (GuiScreenConfig) old;
			
			if (gsc.isForBlock(pos.toBlock(), side)) {
				gsc.updateFriends(friends);
				gsc.updateFriendRights(friendRights);
				gsc.updateOtherRights(otherRights);
				gsc.updateMyRights();
				
				return null;
			}
		}
		
		if (onlyUpdate)
			return null;
		
		BlockEntity te = world.getBlockEntity(pos.toBlock());
		if (te == null || !(te instanceof ScreenBlockEntity)) {
			Log.error("TileEntity at %s is not a screen; can't open gui!", pos.toString());
			return null;
		}
		
		return new GuiScreenConfig(Component.nullToEmpty(""), (ScreenBlockEntity) te, side, friends, friendRights, otherRights);
	}
	
	@Override
	public String getName() {
		return "ScreenConfig";
	}
	
	public ScreenConfigData updateOnly() {
		onlyUpdate = true;
		return this;
	}
	
	public void sendToNear(Level level, net.minecraft.core.BlockPos pos) {
		WDNetworkRegistry.sendToNear(level, pos, 64.0, new S2CMessageOpenGui(this));
	}
	
	@Override
	public void serialize(FriendlyByteBuf buf) {
		buf.writeBoolean(onlyUpdate);
		BufferUtils.writeVec3i(buf, pos);
		BufferUtils.writeEnum(buf, side, (byte) 1);
		BufferUtils.writeArray(buf, friends, (nameUUIDPair) -> nameUUIDPair.writeTo(buf));
		buf.writeInt(friendRights);
		buf.writeInt(otherRights);
	}
	
	@Override
	public void deserialize(FriendlyByteBuf buf) {
		onlyUpdate = buf.readBoolean();
		pos = BufferUtils.readVec3i(buf);
		side = (BlockSide) BufferUtils.readEnum(buf, (v) -> BlockSide.values()[v], (byte) 1);
		friends = BufferUtils.readArray(buf, new NameUUIDPair[0], () -> new NameUUIDPair(buf));
		friendRights = buf.readInt();
		otherRights = buf.readInt();
	}
}
