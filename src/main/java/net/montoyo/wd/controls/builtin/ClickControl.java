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
import net.montoyo.wd.utilities.math.Vector2i;

import java.util.function.Function;

public class ClickControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:click");
	
	public enum ControlType {
		CLICK, MOVE, DOWN, UP
	}
	
	ControlType type;
	Vector2i coord;
	
	public ClickControl(ControlType type, Vector2i coord) {
		this(type, coord, -1);
		this.type = type;
	}
	
	public ClickControl(ControlType type, Vector2i coord, int button) {
		super(id);
		this.coord = coord;
	}
	
	public ClickControl(FriendlyByteBuf buf) {
		super(id);
		type = ControlType.values()[buf.readByte()];
		coord = new Vector2i(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeByte(type.ordinal());
		coord.writeTo(buf);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		throw new RuntimeException("Cannot call click control on server");
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		if (coord != null)
			tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, coord, -1);
		
		tes.handleMouseEvent(side, type, coord, 1);
	}
}
