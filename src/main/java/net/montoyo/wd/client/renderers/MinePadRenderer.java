/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.compat.ImmediatelyFastCompat;
import net.montoyo.wd.client.compat.IrisCompat;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.item.ItemMinePad2;

import static com.mojang.math.Axis.*;

@OnlyIn(Dist.CLIENT)
public final class MinePadRenderer implements IItemRenderer {
	private static final float PI = (float) Math.PI;
	private final Minecraft mc = Minecraft.getInstance();
	private final ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("webdisplays", "textures/item/model/minepad.png");
	private final ModelMinePad model = new ModelMinePad();
	private final ClientProxy clientProxy = (ClientProxy) WebDisplays.PROXY;
	
	private float sinSqrtSwingProg1;
	private float sinSqrtSwingProg2;
	private float sinSwingProg1;
	private float sinSwingProg2;
	
	public static boolean renderAtSide(float handSideSign) {
		float relSide = handSideSign;
		if (Minecraft.getInstance().player.getMainArm() == HumanoidArm.LEFT) relSide *= -1;
		
		// by default, the player holds the device off to the side
		// if they are crouching, they hold it infront of them
		// however, if they are holding two at once, then it once again should just be held off to the side
		boolean sideHold = Minecraft.getInstance().player.isShiftKeyDown() != ClientConfig.sidePad;
		if (
				(relSide < 0 && Minecraft.getInstance().player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof ItemMinePad2) ||
						(relSide > 0 && Minecraft.getInstance().player.getItemInHand(InteractionHand.OFF_HAND).getItem() instanceof ItemMinePad2)
		) sideHold = true;
		
		return sideHold;
	}
	
	@Override
	public final boolean render(PoseStack stack, ItemStack is, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight) {
		//Pre-compute values
		float sqrtSwingProg = (float) Math.sqrt(swingProgress);
		sinSqrtSwingProg1 = (float) Math.sin(sqrtSwingProg * PI);
		sinSqrtSwingProg2 = (float) Math.sin(sqrtSwingProg * PI * 2.0f);
		sinSwingProg1 = (float) Math.sin(swingProgress * PI);
		sinSwingProg2 = (float) Math.sin(swingProgress * swingProgress * PI);
		
		boolean sideHold = renderAtSide(handSideSign);
		
		//Render arm
		stack.pushPose();
		renderArmFirstPerson(stack, multiBufferSource, packedLight, equipProgress, handSideSign);
		stack.popPose();
//		if (!sideHold && handSideSign == 1 && mc.player.getItemInHand(InteractionHand.OFF_HAND).isEmpty()) {
//			stack.pushPose();
//			renderArmFirstPerson(stack, multiBufferSource, packedLight, 0, -handSideSign);
//			stack.popPose();
//		}
		
		//Prepare minePad transform
		stack.pushPose();
		stack.translate(handSideSign * -0.4f * sinSqrtSwingProg1, 0.2f * sinSqrtSwingProg2, -0.2f * sinSwingProg1);
		stack.translate(handSideSign * 0.56f, -0.52f - equipProgress * 0.6f, -0.72f);
		stack.mulPose(YP.rotationDegrees(handSideSign * (45.0f - sinSwingProg2 * 20.0f)));
		stack.mulPose(ZP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * -20.0f));
		stack.mulPose(XP.rotationDegrees(sinSqrtSwingProg1 * -80.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * -45.0f));
		
		if (sideHold) {
			stack.translate(0.0f, 0.0f, -0.2f);
			stack.mulPose(YP.rotationDegrees(20.0f * -handSideSign));
			float total = 0.475f;
			float off = -0.025f; // gotta love magic numbers
			stack.translate(-(total - off) + (off * handSideSign), -0.1f, 0.0f);
			stack.mulPose(ZP.rotationDegrees(1.0f));
		} else if (handSideSign >= 0) // right hand
			stack.translate(-1.065f, 0.0f, 0.0f);
		else // left hand
			stack.translate(0.065f, 0.0f, 0.0f);
		
		//Render model
		stack.translate(0.063f, 0.28f, 0.001f);
		model.render(multiBufferSource, stack);
		stack.translate(-0.063f, -0.28f, -0.001f);

		// Force flush all batched content before rendering browser
		// This is critical for ImmediatelyFast compatibility
		if (multiBufferSource instanceof MultiBufferSource.BufferSource bufferSource) {
			bufferSource.endBatch();
		}

		net.minecraft.nbt.CompoundTag tag = is.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
		if (tag != null && tag.contains("PadID")) {
			ClientProxy.PadData pd = clientProxy.getPadByID(tag.getUUID("PadID"));

			//Render web view
			if (pd != null) {
				double x1 = 0.0;
				double y1 = 0.0;
				double x2 = 27.65 / 32.0 + 0.01;
				double y2 = 14.0 / 32.0 + 0.002;

				stack.translate(0.063f, 0.28f, 0.001f);

				// Force ImmediatelyFast to flush its batched buffers before our custom rendering
				ImmediatelyFastCompat.forceFlushBatching();

				int textureId = ((MCEFBrowser) pd.view).getRenderer().getTextureID();

				// Check if shaders are active
				boolean shadersActive = IrisCompat.isShaderPackInUse();

				if (shadersActive) {
					// Queue for GUI overlay rendering (happens after shaders complete)
					MinePadOverlayRenderer.queueRender(stack, x1, y1, x2, y2, textureId);
				} else {
					// Standard rendering (no shaders active)
					RenderSystem.disableDepthTest();
					RenderSystem.enableBlend();
					RenderSystem.defaultBlendFunc();
					RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
					RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
					RenderSystem.setShaderTexture(0, textureId);
					BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
					buffer.addVertex(stack.last().pose(), (float) x1, (float) y1, 0.0f).setUv(0.0F, 1.0F).setColor(255, 255, 255, 255);
					buffer.addVertex(stack.last().pose(), (float) x2, (float) y1, 0.0f).setUv(1.0F, 1.0F).setColor(255, 255, 255, 255);
					buffer.addVertex(stack.last().pose(), (float) x2, (float) y2, 0.0f).setUv(1.0F, 0.0F).setColor(255, 255, 255, 255);
					buffer.addVertex(stack.last().pose(), (float) x1, (float) y2, 0.0f).setUv(0.0F, 0.0F).setColor(255, 255, 255, 255);
					BufferUploader.drawWithShader(buffer.buildOrThrow());
					RenderSystem.disableBlend();
					RenderSystem.enableDepthTest();
				}
			}
		}
		
		stack.popPose();
		RenderSystem.enableCull();
		
		return true;
	}
	
	private void renderArmFirstPerson(PoseStack stack, MultiBufferSource buffer, int combinedLight, float equipProgress, float handSideSign) {
		float tx = -0.3f * sinSqrtSwingProg1;
		float ty = 0.4f * sinSqrtSwingProg2;
		float tz = -0.4f * sinSwingProg1;
		
		stack.translate(handSideSign * (tx + 0.64000005f), ty - 0.6f - equipProgress * 0.6f, tz - 0.71999997f);
		stack.mulPose(YP.rotationDegrees(handSideSign * 45.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * 70.0f));
		stack.mulPose(ZP.rotationDegrees(handSideSign * sinSwingProg2 * -20.0f));
		stack.translate(-handSideSign, 3.6f, 3.5f);
		stack.mulPose(ZP.rotationDegrees(handSideSign * 120.0f));
		stack.mulPose(XP.rotationDegrees(200.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * -135.0f));
		stack.translate(handSideSign * 5.6f, 0.0f, 0.0f);
		
		PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
		RenderSystem.setShaderTexture(0, mc.player.getSkin().texture());
		
		if (handSideSign >= 0.0f)
			playerRenderer.renderRightHand(stack, buffer, combinedLight, mc.player);
		else
			playerRenderer.renderLeftHand(stack, buffer, combinedLight, mc.player);
	}
}
