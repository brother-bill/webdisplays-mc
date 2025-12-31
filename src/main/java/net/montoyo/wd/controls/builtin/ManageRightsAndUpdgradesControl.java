package net.montoyo.wd.controls.builtin;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

/**
 * TODO: I'm considering merging this with {@link ModifyFriendListControl} to make ManageScreenControl
 */
@Deprecated
public class ManageRightsAndUpdgradesControl extends ScreenControl {
	public static final ResourceLocation id = ResourceLocation.parse("webdisplays:mod_rights_upgrades");
	
	public enum ControlType {
		RIGHTS, UPGRADES
	}
	
	ControlType type;
	boolean adding;
	ItemStack toRemove;
	
	private int friendRights;
	private int otherRights;
	
	public ManageRightsAndUpdgradesControl(boolean adding, ItemStack toRemove) {
		super(id);
		this.adding = adding;
		type = ControlType.UPGRADES;
		this.toRemove = toRemove;
	}
	
	public ManageRightsAndUpdgradesControl(int friendRights, int otherRights) {
		super(id);
		type = ControlType.RIGHTS;
		this.friendRights = friendRights;
		this.otherRights = otherRights;
	}
	
	public ManageRightsAndUpdgradesControl(FriendlyByteBuf buf) {
		super(id);
		type = ControlType.values()[buf.readByte()];
		switch (type) {
			case UPGRADES -> {
				adding = buf.readBoolean();
				toRemove = net.montoyo.wd.utilities.serialization.Util.readItemStack(buf);
			}
			case RIGHTS -> {
				friendRights = buf.readInt();
				otherRights = buf.readInt();
			}
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeByte(type.ordinal());
		switch (type) {
			case UPGRADES -> {
				buf.writeBoolean(adding);
				net.montoyo.wd.utilities.serialization.Util.writeItemStack(buf, toRemove);
			}
			case RIGHTS -> {
				buf.writeInt(friendRights);
				buf.writeInt(otherRights);
			}
		}
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		ServerPlayer player = (ServerPlayer) ctx.player();
		switch (type) {
			case UPGRADES -> {
				checkPerms(ScreenRights.MANAGE_UPGRADES, permissionChecker, (ServerPlayer) ctx.player());
				if (adding)
					throw new RuntimeException("Cannot add an upgrade from the client");
				else tes.removeUpgrade(side, toRemove, player);
			}
			case RIGHTS -> {
				ScreenData scr = tes.getScreen(side);

				int fr = scr.owner.uuid.equals(player.getGameProfile().getId()) ? friendRights : scr.friendRights;
				int or = (scr.rightsFor(player) & ScreenRights.MANAGE_OTHER_RIGHTS) == 0 ? scr.otherRights : otherRights;

				if(scr.friendRights != fr || scr.otherRights != or)
					tes.setRights(player, side, fr, or);
			}
		}
	}
	
	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, IPayloadContext ctx) {
		net.minecraft.world.entity.player.Player player = ctx.player();
		switch (type) {
			case UPGRADES -> {
				if (adding)
					tes.addUpgrade(side, toRemove, player, true);
				else tes.removeUpgrade(side, toRemove, player);
			}
			case RIGHTS -> {
				ScreenData scr = tes.getScreen(side);

				int fr = friendRights;
				int or = otherRights;

				if(scr.friendRights != fr || scr.otherRights != or)
					tes.setRights(null, side, fr, or);  // On client, player not used
			}
		}
	}
}
