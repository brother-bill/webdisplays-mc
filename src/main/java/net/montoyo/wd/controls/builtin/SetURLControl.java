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
import net.montoyo.wd.utilities.math.Vector3i;

import java.util.function.Function;

public class SetURLControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:set_url");
	
	String url;
	Vector3i remoteLocation;
	
	public SetURLControl(String url, Vector3i remoteLocation) {
		super(id);
		this.url = url;
		this.remoteLocation = remoteLocation;
	}
	
	public SetURLControl(FriendlyByteBuf buf) {
		super(id);
		url = buf.readUtf();
		if (buf.readBoolean()) remoteLocation = new Vector3i(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(url);
		buf.writeBoolean(remoteLocation != null);
		if (remoteLocation != null) remoteLocation.writeTo(buf);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		// TODO: deal with remote
		checkPerms(ScreenRights.CHANGE_URL, permissionChecker, (net.minecraft.server.level.ServerPlayer) ctx.player());
		try {
			tes.setScreenURL(side, url);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		try {
			tes.setScreenURL(side, url);
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
}
