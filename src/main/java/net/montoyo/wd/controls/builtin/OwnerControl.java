package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.function.Function;

public class OwnerControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:set_owner");
	
	NameUUIDPair owner;
	
	public OwnerControl(NameUUIDPair pair) {
		super(id);
		this.owner = pair;
	}
	
	public OwnerControl(FriendlyByteBuf buf) {
		super(id);
		owner = new NameUUIDPair(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		owner.writeTo(buf);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		throw new RuntimeException("Cannot handle ownership theft packet from server");
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		tes.getScreen(side).owner = owner;
	}
}
