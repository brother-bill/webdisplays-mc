/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.controls.builtin.ClickControl;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageScreenCtrl;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemLaserPointer extends Item implements WDItem {
	
	public ItemLaserPointer(Properties properties) {
		super(properties
				.stacksTo(1)
//				.tab(WebDisplays.CREATIVE_TAB)
		);
	}
	
	//Laser pointer
	private static ScreenBlockEntity pointedScreen;
	private static BlockSide pointedScreenSide;
	private static long lastPointPacket;

	private static boolean left;
	private static boolean middle;
	private static boolean right;

	/**
	 * Get the currently held button for drag operations ;)
	 * Returns -1 if no button is held, 0 for left, 1 for right, 2 for middle.
	 */
	private static int getHeldButton() {
		if (left) return 0;
		if (right) return 1;
		if (middle) return 2;
		return -1;
	}
	
	public static void tick(Minecraft mc) {
		BlockHitResult result = ClientProxy.raycast(64.0); //TODO: Make that distance configurable
		
		BlockPos bpos = result.getBlockPos();
		
		if (result.getType() == HitResult.Type.BLOCK && mc.level.getBlockState(bpos).getBlock() == BlockRegistry.SCREEN_BLOCk.get()) {
			Vector3i pos = new Vector3i(result.getBlockPos());
			BlockSide side = BlockSide.values()[result.getDirection().ordinal()];
			
			Multiblock.findOrigin(mc.level, pos, side, null);
			ScreenBlockEntity te = (ScreenBlockEntity) mc.level.getBlockEntity(pos.toBlock());
			
			if (te != null && te.hasUpgrade(side, DefaultUpgrade.LASERMOUSE)) { //hasUpgrade returns false is there's no screen on side 'side'
				//Since rights aren't synchronized, let the server check them for us...
				ScreenData scr = te.getScreen(side);
				
				if (scr.browser != null) {
					float hitX = ((float) result.getLocation().x) - (float) pos.x;
					float hitY = ((float) result.getLocation().y) - (float) pos.y;
					float hitZ = ((float) result.getLocation().z) - (float) pos.z;
					Vector2i tmp = new Vector2i();
					
					if (ScreenBlock.hit2pixels(side, bpos, new Vector3i(result.getBlockPos()), scr, hitX, hitY, hitZ, tmp)) {
						laserClick(te, side, scr, tmp);
					}
				}
			}
		}
	}
	
	public static void deselect(Minecraft mc) {
		deselectScreen();
	}
	
	private static void laserClick(ScreenBlockEntity tes, BlockSide side, ScreenData scr, Vector2i hit) {
		int heldButton = getHeldButton();
		tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, heldButton);
		if (pointedScreen == tes && pointedScreenSide == side) {
			long t = System.currentTimeMillis();

			// Send more frequently during drag operations for smoother scrollbar movement
			int interval = (heldButton >= 0) ? 50 : 100;
			if (t - lastPointPacket >= interval) {
				lastPointPacket = t;
				WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserMove(tes, side, hit, heldButton));
			}
		} else {
			deselectScreen();
			pointedScreen = tes;
			pointedScreenSide = side;
		}
	}
	
	private static void deselectScreen() {
		if (pointedScreen != null && pointedScreenSide != null) {
			pointedScreen = null;
			pointedScreenSide = null;
		}
	}
	
	public static void press(boolean press, int button) {
		if (button <= 1 && ClientConfig.Input.switchButtons)
			button = 1 - button;

		if (button == 0) left = press;
		else if (button == 1) right = press;
		else if (button == 2) middle = press;
		
		Minecraft mc = Minecraft.getInstance();
		
		BlockHitResult result = ClientProxy.raycast(64.0); //TODO: Make that distance configurable
		Vector3i pos = new Vector3i(result.getBlockPos());
		BlockSide side = BlockSide.values()[result.getDirection().ordinal()];
		Multiblock.findOrigin(mc.level, pos, side, null);
		
		BlockEntity be = mc.level.getBlockEntity(pos.toBlock());
		if (!(be instanceof ScreenBlockEntity)) return;
		
		//noinspection PatternVariableCanBeUsed
		ScreenBlockEntity te = (ScreenBlockEntity) be;
		
		if (te.hasUpgrade(side, DefaultUpgrade.LASERMOUSE)) { //hasUpgrade returns false is there's no screen on side 'side'
			int finalButton = button;
			te.interact(result, (hit) -> {
				te.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
				te.handleMouseEvent(side, press ? ClickControl.ControlType.DOWN : ClickControl.ControlType.UP, hit, finalButton);

				if (press)
					WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserDown(te, side, hit, finalButton));
				else
					WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserUp(te, side, finalButton));
			});
		}
	}
	
	public static boolean isOn() {
		return left || right || middle;
	}
	
	@Nullable
	@Override
	public String getWikiName(@Nonnull ItemStack is) {
		return is.getItem().getName(is).getString();
	}
}
