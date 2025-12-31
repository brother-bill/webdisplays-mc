/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;

public interface IItemRenderer {
	
	/**
	 * @param pose the pose stack
	 * @param stack the item stack
	 * @param handSideSign TODO:
	 * @param swingProgress TODO:
	 * @param equipProgress TODO:
	 * @param multiBufferSource the buffer source
	 * @param packedLight packed light
	 * @return whether or not to cancel vanilla rendering
	 */
	boolean render(PoseStack pose, ItemStack stack, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight);
	
}
