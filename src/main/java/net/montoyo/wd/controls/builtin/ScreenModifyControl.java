package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;

import java.util.function.Function;

public class ScreenModifyControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:mod_screen");
	
	public enum ControlType {
		RESOLUTION, ROTATION
	}
	
	ControlType type;
	Vector2i res;
	Rotation rotation;
	
	public ScreenModifyControl(Vector2i res) {
		super(id);
		this.type = ControlType.RESOLUTION;
		this.res = res;
	}
	
	public ScreenModifyControl(Rotation rotation) {
		super(id);
		this.type = ControlType.ROTATION;
		this.rotation = rotation;
	}
	
	public ScreenModifyControl(FriendlyByteBuf buf) {
		super(id);
		type = ControlType.values()[buf.readByte()];
		if (type.equals(ControlType.RESOLUTION))
			res = new Vector2i(buf);
		else rotation = Rotation.values()[buf.readByte()];
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeByte(type.ordinal());
		if (res != null) res.writeTo(buf);
		else if (rotation != null) buf.writeByte(rotation.ordinal());
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		checkPerms(ScreenRights.MODIFY_SCREEN, permissionChecker, (net.minecraft.server.level.ServerPlayer) ctx.player());
		switch (type) {
			case RESOLUTION -> tes.setResolution(side, res);
			case ROTATION -> tes.setRotation(side, rotation);
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		switch (type) {
			case RESOLUTION -> tes.setResolution(side, res);
			case ROTATION -> tes.setRotation(side, rotation);
		}
	}
}
