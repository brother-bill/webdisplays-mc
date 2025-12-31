/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.item.ItemLaserPointer;
import org.joml.Matrix4f;

import static com.mojang.math.Axis.*;

@OnlyIn(Dist.CLIENT)
public final class LaserPointerRenderer implements IItemRenderer {

	public LaserPointerRenderer() {
	}

	public static boolean isOn() {
		if (Minecraft.getInstance().screen != null) return false;

		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null &&
				(
						ClientProxy.mouseOn ||
								ItemLaserPointer.isOn()
				) &&
				mc.player.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(ItemRegistry.LASER_POINTER.get()) &&
				(mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.BLOCK || mc.hitResult.getType() == HitResult.Type.MISS);
	}

	@Override
	public boolean render(PoseStack poseStack, ItemStack is, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight) {
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		float PI = (float) Math.PI;

		float sqrtSwingProg = (float) Math.sqrt(swingProgress);
		float sinSqrtSwingProg1 = (float) Math.sin(sqrtSwingProg * PI);

		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		var matrix0 = poseStack.last().pose();
		//Laser pointer
		poseStack.pushPose();
		poseStack.translate(handSideSign * -0.4f * sinSqrtSwingProg1, (float) (0.2f * Math.sin(sqrtSwingProg * PI * 2.0f)), (float) (-0.2f * Math.sin(swingProgress * PI)));
		poseStack.translate(handSideSign * 0.56f, -0.52f - equipProgress * 0.6f, -0.72f);
		poseStack.mulPose(YP.rotationDegrees((float) (handSideSign * (45.0f - Math.sin(swingProgress * swingProgress * PI) * 20.0f))));
		poseStack.mulPose(ZP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * -20.0f));
		poseStack.mulPose(XP.rotationDegrees(sinSqrtSwingProg1 * -80.0f));
		poseStack.mulPose(YP.rotationDegrees(handSideSign * -30.0f));
		poseStack.translate(0.0f, 0.2f, 0.0f);
		poseStack.mulPose(XP.rotationDegrees(10.0f));
		poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
		var matrix = poseStack.last().pose();

		BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 0.0f, -1.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);

		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, -1.0f, 0.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);

		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(0.5f, 0.5f, 0.5f, 1.0f);

		if (isOn()) drawLineBetween(bb, matrix0, matrix, new Vec3(0.5f, -0.5f, 0.5f), new Vec3(-40.0f, 4000.5f, -100.0f));

		BufferUploader.drawWithShader(bb.buildOrThrow());

		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.enableCull();
		poseStack.popPose();

		return true;
	}

	private static void drawLineBetween(BufferBuilder bb, Matrix4f matrix0, Matrix4f matrix, Vec3 local, Vec3 target) {
		//Calculate distance between points -> length of the line
		float distance = (float) local.distanceTo(target) / 2;
		float quarterWidth = 0.25f;
		float biggerWidth = 10;

		bb.addVertex(matrix, 0.25f, -0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix, quarterWidth + 0.25f, -0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, biggerWidth - 6f, 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, -6f, 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);

		bb.addVertex(matrix, 0.25f, -0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix, 0.25f, -quarterWidth - 0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, -6f, -biggerWidth + 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, -6f, 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);

		bb.addVertex(matrix, quarterWidth + 0.25f, -0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix, quarterWidth + 0.25f, -quarterWidth - 0.25f, 0.5f).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, biggerWidth - 6f, -biggerWidth + 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);
		bb.addVertex(matrix0, biggerWidth - 6f, 3f, -distance).setColor(0.5f, 0.0f, 0.0f, 1.0f);
	}
}
