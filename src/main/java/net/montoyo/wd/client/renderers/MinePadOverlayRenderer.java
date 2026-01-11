/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders MinePad browser content as a GUI overlay.
 * This allows the browser to display correctly even when shaders are active,
 * since GUI rendering happens after world shaders complete.
 *
 * Captures the full 3D transformation matrices during item rendering and
 * replays them in the GUI pass to achieve proper 3D perspective rendering.
 */
@OnlyIn(Dist.CLIENT)
public class MinePadOverlayRenderer {

    /**
     * Data for a pending browser overlay render.
     * Stores the complete transformation matrices to replay 3D rendering in GUI pass.
     */
    public static class PendingRender {
        public final Matrix4f modelViewMatrix;
        public final Matrix4f projectionMatrix;
        public final float x1, y1, x2, y2;
        public final int textureId;
        public final boolean valid;

        public PendingRender(Matrix4f modelViewMatrix, Matrix4f projectionMatrix,
                           float x1, float y1, float x2, float y2, int textureId, boolean valid) {
            this.modelViewMatrix = modelViewMatrix;
            this.projectionMatrix = projectionMatrix;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.textureId = textureId;
            this.valid = valid;
        }
    }

    // Pending renders for the current frame
    private static final List<PendingRender> pendingRenders = new ArrayList<>();

    /**
     * Queue a browser render for the GUI overlay pass.
     * Called from MinePadRenderer during item rendering.
     * Captures the full 3D matrices to replay the exact same transformation later.
     */
    public static void queueRender(PoseStack poseStack, double x1, double y1, double x2, double y2, int textureId) {
        // Capture the complete transformation state
        Matrix4f modelViewMatrix = new Matrix4f(poseStack.last().pose());
        Matrix4f projectionMatrix = new Matrix4f(RenderSystem.getProjectionMatrix());

        // Combine pose matrix with the RenderSystem's model view matrix
        Matrix4f combinedModelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        combinedModelView.mul(modelViewMatrix);

        pendingRenders.add(new PendingRender(
            combinedModelView,
            projectionMatrix,
            (float) x1, (float) y1, (float) x2, (float) y2,
            textureId,
            true
        ));
    }

    /**
     * Event handler for GUI rendering - renders queued browser overlays.
     * Replays the captured 3D transformation matrices to render with proper perspective.
     */
    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (pendingRenders.isEmpty()) {
            return;
        }

        // Render all pending browser overlays
        for (PendingRender render : pendingRenders) {
            if (!render.valid) {
                continue;
            }

            // Save current matrix state
            Matrix4f savedProjection = RenderSystem.getProjectionMatrix();
            Matrix4f savedModelView = RenderSystem.getModelViewMatrix();

            // Set up the captured 3D transformation matrices
            RenderSystem.setProjectionMatrix(render.projectionMatrix, VertexSorting.ORTHOGRAPHIC_Z);
            RenderSystem.getModelViewStack().pushMatrix();
            RenderSystem.getModelViewStack().set(render.modelViewMatrix);
            RenderSystem.applyModelViewMatrix();

            // Set up rendering state
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.setShaderTexture(0, render.textureId);

            // Draw the quad using the original 3D coordinates - the matrices handle perspective
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            // Render the quad with the same coordinates as the non-shader path
            buffer.addVertex(render.x1, render.y1, 0.0f).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
            buffer.addVertex(render.x2, render.y1, 0.0f).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
            buffer.addVertex(render.x2, render.y2, 0.0f).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
            buffer.addVertex(render.x1, render.y2, 0.0f).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);

            BufferUploader.drawWithShader(buffer.buildOrThrow());

            // Restore rendering state
            RenderSystem.disableBlend();
            RenderSystem.enableDepthTest();

            // Restore matrix state
            RenderSystem.getModelViewStack().popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(savedProjection, VertexSorting.ORTHOGRAPHIC_Z);
        }

        // Clear pending renders for next frame
        pendingRenders.clear();
    }

    /**
     * Clear any pending renders (called at start of frame if needed).
     */
    public static void clearPendingRenders() {
        pendingRenders.clear();
    }
}
