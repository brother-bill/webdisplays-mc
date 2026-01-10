/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class ModelMinePad {
	public void render(MultiBufferSource buffers, PoseStack stack) {
		// TODO: this needs completing
		// TODO: I'd like this to be able to load a model from a JSON if possible

		double x1 = 0.0;
		double y1 = 0.0;
		double x2 = 27.65 / 32.0 + 0.01;
		double y2 = 14.0 / 32.0 + 0.002;

		Matrix4f positionMatrix = stack.last().pose();

		// Black background quad - rendered behind browser to prevent Z-fighting
		float bgZ = -0.002f;
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		BufferBuilder vb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		vb.addVertex(positionMatrix, (float) x1, (float) y1, bgZ).setColor(0, 0, 0, 255);
		vb.addVertex(positionMatrix, (float) x2, (float) y1, bgZ).setColor(0, 0, 0, 255);
		vb.addVertex(positionMatrix, (float) x2, (float) y2, bgZ).setColor(0, 0, 0, 255);
		vb.addVertex(positionMatrix, (float) x1, (float) y2, bgZ).setColor(0, 0, 0, 255);
		BufferUploader.drawWithShader(vb.buildOrThrow());

		int width = 32;
		int height = 32;

		float padding = 1f / 23;
		float padding1 = 1f / 21;

		float z = -0.001f; // Push bezel back to prevent Z-fighting with browser view

		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutout(ResourceLocation.parse("webdisplays:textures/item/model/minepad_item.png")));

		consumer.addVertex(positionMatrix, (float) x1, (float) y1 - padding, z).setColor(255, 255, 255, 255).setUv(1f / width, 12f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x2, (float) y1 - padding, z).setColor(255, 255, 255, 255).setUv(19f / width, 12f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x2, (float) y2 + padding, z).setColor(255, 255, 255, 255).setUv(19f / width, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x1, (float) y2 + padding, z).setColor(255, 255, 255, 255).setUv(1f / width, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);

		consumer.addVertex(positionMatrix, (float) x1 - padding1, (float) y1, z).setColor(255, 255, 255, 255).setUv(0f / width, 10f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x2 + padding1, (float) y1, z).setColor(255, 255, 255, 255).setUv(20f / width, 10f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x2 + padding1, (float) y2, z).setColor(255, 255, 255, 255).setUv(20f / width, 1f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
		consumer.addVertex(positionMatrix, (float) x1 - padding1, (float) y2, z).setColor(255, 255, 255, 255).setUv(0f / width, 1f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.25f, 0.5f, 1);
	}
}
