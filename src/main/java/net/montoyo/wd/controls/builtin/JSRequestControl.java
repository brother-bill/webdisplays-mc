package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class JSRequestControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:js_req");
	
	int reqId;
	JSServerRequest reqType;
	Object[] data;
	
	public JSRequestControl(int reqId, JSServerRequest reqType, Object[] data) {
		super(id);
		this.reqId = reqId;
		this.reqType = reqType;
		this.data = data;
	}
	
	public JSRequestControl(FriendlyByteBuf buf) {
		super(id);
		reqId = buf.readInt();
		reqType = JSServerRequest.fromID(buf.readByte());
		
		if (reqType != null)
			data = reqType.deserialize(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeInt(reqId);
		buf.writeByte(reqType.ordinal());
		
		if (!reqType.serialize(buf, data))
			throw new RuntimeException("Could not serialize CTRL_JS_REQUEST " + reqType);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		ServerPlayer player = (ServerPlayer) ctx.player();
//		if (reqType == null || data == null) Log.warning("Caught invalid JS request from player %s (UUID %s)", player.getName(), player.getGameProfile().getId().toString());
//		else tes.handleJSRequest(player, side, reqId, reqType, data);
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		throw new RuntimeException("TODO");
	}
}
