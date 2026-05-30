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
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

import static com.mojang.math.Axis.*;

/**
 * <h2>What this class does in production</h2>
 *
 * NOTHING. {@link ScreenStageRenderer} draws screens via {@code RenderLevelStageEvent} —
 * see that class's javadoc for why we don't use Minecraft's normal BlockEntityRenderer
 * iteration. This renderer is still registered with the BE dispatcher so MC's
 * texture-atlas and BE-type bookkeeping is happy, but {@link #render} early-returns
 * unless {@link Log#RENDER_DIAG} is set.
 *
 * <h2>What this class does in diagnostic mode</h2>
 *
 * Everything below {@code installGLDebugCallbackOnce()}, {@code logGLState()}, the
 * GREEN/RED diagnostic quads, and the per-state-transition / per-frame counters are
 * scaffolding from the screen-blank-at-angles investigation. Kept intact (gated on
 * {@link Log#DEBUG} / {@link Log#RENDER_DIAG}) because (a) it doesn't run in
 * production and (b) it's the toolkit we'd want again for any future GL-pipeline-level
 * regression. Don't strip it without a reason.
 *
 * Enable with: {@code gradlew runClient -Dwebdisplays.debug=true -Dwebdisplays.diag=true}
 */
public class ScreenRenderer implements BlockEntityRenderer<ScreenBlockEntity> {
	public ScreenRenderer() {
	}

	public static class ScreenRendererProvider implements BlockEntityRendererProvider<ScreenBlockEntity> {
		@Override
		public @NotNull BlockEntityRenderer<ScreenBlockEntity> create(@NotNull Context arg) {
			return new ScreenRenderer();
		}
	}

	@Override
	public boolean shouldRenderOffScreen(@NotNull ScreenBlockEntity be) {
		return true;
	}

	@Override
	public int getViewDistance() {
		return 256;
	}

	// Custom RenderType built like a Minecraft "decal" — uses VIEW_OFFSET_Z_LAYERING to push the
	// quad slightly toward the camera (same trick signs/paintings use). Goes through the proper
	// MultiBufferSource queue so it participates in Minecraft 1.21's render scheduling.
	private static final RenderType WD_DEBUG_QUAD_RT = RenderType.create(
		"wd_debug_quad",
		DefaultVertexFormat.POSITION_COLOR,
		VertexFormat.Mode.QUADS,
		256,
		false,
		false,
		RenderType.CompositeState.builder()
			.setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionColorShader))
			.setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
			.setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
			.setCullState(RenderStateShard.NO_CULL)
			.setLightmapState(RenderStateShard.NO_LIGHTMAP)
			.setOverlayState(RenderStateShard.NO_OVERLAY)
			.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
			.setWriteMaskState(RenderStateShard.COLOR_DEPTH_WRITE)
			.createCompositeState(false)
	);

	private final Vector3f mid = new Vector3f();
	private final Vector3i tmpi = new Vector3i();
	private final Vector3f tmpf = new Vector3f();

	private static final WeakHashMap<ScreenBlockEntity, String> lastRenderState = new WeakHashMap<>();
	private static final WeakHashMap<ScreenBlockEntity, int[]> frameCounters = new WeakHashMap<>();
	private static long lastFrameLogMs = 0;

	private static void logStateChange(ScreenBlockEntity te, String stateKey) {
		if (!Log.DEBUG) return;
		String prev = lastRenderState.put(te, stateKey);
		if (prev == null || !prev.equals(stateKey)) {
			Log.dbg("render", "pos=%s state=%s", te.getBlockPos(), stateKey);
		}
	}

	private static void tickFrameCounter(ScreenBlockEntity te, boolean drewQuad) {
		if (!Log.DEBUG) return;
		int[] counts = frameCounters.computeIfAbsent(te, k -> new int[2]);
		counts[0]++;
		if (drewQuad) counts[1]++;
		long now = System.currentTimeMillis();
		if (now - lastFrameLogMs >= 1000) {
			lastFrameLogMs = now;
			for (var e : frameCounters.entrySet()) {
				int[] c = e.getValue();
				Log.dbg("render-fps", "pos=%s calls=%d drew=%d (last 1s)", e.getKey().getBlockPos(), c[0], c[1]);
				c[0] = 0; c[1] = 0;
			}
		}
	}

	// Enable GL driver-level debug callbacks the first time we render. The driver will fire
	// our callback on every GL error/warning/perf-warning, dumping to our log. If something is
	// silently failing during our draw, the driver will tell us.
	private static boolean glDebugInstalled = false;
	private static void installGLDebugCallbackOnce() {
		if (glDebugInstalled || !Log.RENDER_DIAG) return;
		glDebugInstalled = true;
		try {
			org.lwjgl.opengl.GL43.glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT);
			org.lwjgl.opengl.GL43.glEnable(org.lwjgl.opengl.GL43.GL_DEBUG_OUTPUT_SYNCHRONOUS);
			org.lwjgl.opengl.GL43.glDebugMessageCallback(
				(source, type, id, severity, length, messagePtr, userParam) -> {
					// Skip super-spammy NOTIFICATION-level messages
					if (severity == org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_NOTIFICATION) return;
					String msg = org.lwjgl.opengl.GLDebugMessageCallback.getMessage(length, messagePtr);
					String sevStr = switch (severity) {
						case org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_HIGH -> "HIGH";
						case org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_MEDIUM -> "MED";
						case org.lwjgl.opengl.GL43.GL_DEBUG_SEVERITY_LOW -> "LOW";
						default -> "?";
					};
					String typeStr = switch (type) {
						case org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_ERROR -> "ERROR";
						case org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "DEPRECATED";
						case org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "UNDEFINED";
						case org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_PORTABILITY -> "PORTABILITY";
						case org.lwjgl.opengl.GL43.GL_DEBUG_TYPE_PERFORMANCE -> "PERF";
						default -> "?";
					};
					Log.dbg("gl-driver", "sev=%s type=%s id=%d: %s", sevStr, typeStr, id, msg);
				},
				0L
			);
			Log.dbg("gl-driver", "OpenGL debug callback installed");
		} catch (Throwable t) {
			Log.dbg("gl-driver", "Failed to install GL debug callback: %s", t.toString());
		}
	}

	// Dump the full OpenGL pipeline state at the moment our quad submits. Compare visible vs
	// blanked frames in the log to find any state difference that's killing the draw.
	private static long lastGlStateLogMs = 0;
	private static void logGLState(ScreenBlockEntity te) {
		if (!Log.DEBUG) return;
		long now = System.currentTimeMillis();
		if (now - lastGlStateLogMs < 1000) return;
		lastGlStateLogMs = now;
		try {
			int[] vp = new int[4];
			org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, vp);
			int[] sc = new int[4];
			org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX, sc);
			boolean scissorEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
			boolean depthTest = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_DEPTH_TEST);
			boolean cullFace = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_CULL_FACE);
			boolean blend = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
			int depthFunc = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_DEPTH_FUNC);
			int drawFbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_DRAW_FRAMEBUFFER_BINDING);
			int readFbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_READ_FRAMEBUFFER_BINDING);
			int prog = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL20.GL_CURRENT_PROGRAM);
			java.nio.ByteBuffer cmBuf = org.lwjgl.BufferUtils.createByteBuffer(4);
			org.lwjgl.opengl.GL11.glGetBooleanv(org.lwjgl.opengl.GL11.GL_COLOR_WRITEMASK, cmBuf);
			boolean cmR = cmBuf.get(0) != 0;
			boolean cmG = cmBuf.get(1) != 0;
			boolean cmB = cmBuf.get(2) != 0;
			boolean cmA = cmBuf.get(3) != 0;
			java.nio.ByteBuffer dmBuf = org.lwjgl.BufferUtils.createByteBuffer(1);
			org.lwjgl.opengl.GL11.glGetBooleanv(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK, dmBuf);
			boolean depthMask = dmBuf.get(0) != 0;
			Log.dbg("gl", "pos=%s vp=[%d,%d,%dx%d] scissor=%s[%d,%d,%dx%d] depthTest=%b depthFunc=0x%x depthMask=%b cull=%b blend=%b colorMask=[%b,%b,%b,%b] drawFBO=%d readFBO=%d shader=%d",
					te.getBlockPos(),
					vp[0], vp[1], vp[2], vp[3],
					scissorEnabled, sc[0], sc[1], sc[2], sc[3],
					depthTest, depthFunc, depthMask,
					cullFace, blend,
					cmR, cmG, cmB, cmA,
					drawFbo, readFbo, prog);
		} catch (Throwable t) {
			Log.dbg("gl", "GL state query failed: %s", t.toString());
		}
	}

	@Override
	public void render(ScreenBlockEntity te, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		installGLDebugCallbackOnce(); // no-op after first call; only runs if RENDER_DIAG=true
		// Bug fix: Minecraft's BE renderer iteration skips this method at certain camera positions,
		// despite shouldRenderOffScreen=true. ScreenStageRenderer (RenderLevelStageEvent.AFTER_LEVEL)
		// now does the actual drawing by iterating loaded chunks directly. This BE renderer is kept
		// only for diagnostic logging — its actual render output is redundant with the stage renderer.
		boolean drewSomething = false;
		try {
		if (!te.isLoaded()) {
			logStateChange(te, "EARLY:not-loaded");
			return;
		}
		// In production / non-diag mode, skip duplicating the draw — ScreenStageRenderer handles it.
		if (!Log.RENDER_DIAG) return;

		RenderSystem.disableBlend();

		for (int i = 0; i < te.screenCount(); i++) {
			ScreenData scr = te.getScreen(i);
			if (scr.browser == null) {
				double dist = WebDisplays.PROXY.distanceTo(te, Minecraft.getInstance().getEntityRenderDispatcher().camera.getPosition());
				logStateChange(te, "EARLY:browser-null side=" + scr.side + " dist=" + ((int)dist));
				// Same units as ClientProxy now — both compare squared dist against loadDistance²
				// (port-era `* 16` removed; was causing browsers to be re-created at 480 blocks
				// while ClientProxy kills them at 32, producing audio-instance pileup).
				if (dist <= WebDisplays.INSTANCE.loadDistance2)
					scr.createBrowser(te, true);
				continue;
			}

			if (!(scr.browser instanceof MCEFBrowser)) {
				logStateChange(te, "EARLY:not-MCEFBrowser side=" + scr.side);
				continue;
			}
			MCEFBrowser mcefBrowser = (MCEFBrowser) scr.browser;
			if (mcefBrowser.getRenderer() == null || mcefBrowser.getRenderer().getTextureID() == 0) {
				logStateChange(te, "EARLY:texture-not-ready side=" + scr.side);
				continue;
			}
			logStateChange(te, "RENDER side=" + scr.side + " texId=" + mcefBrowser.getRenderer().getTextureID());

			tmpi.set(scr.side.right);
			tmpi.mul(scr.size.x);
			tmpi.addMul(scr.side.up, scr.size.y);
			tmpf.set(tmpi);
			mid.set(0.5, 0.5, 0.5);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.left);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.down);
			mid.addMul(tmpf, 0.5f);

			poseStack.pushPose();
			poseStack.translate(mid.x, mid.y, mid.z);

			switch (scr.side) {
				case BOTTOM: poseStack.mulPose(XP.rotation(90.f + 49.8f)); break;
				case TOP: poseStack.mulPose(XN.rotation(90.f + 49.8f)); break;
				case NORTH: poseStack.mulPose(YN.rotationDegrees(180.f)); break;
				case SOUTH: break;
				case WEST: poseStack.mulPose(YN.rotationDegrees(90.f)); break;
				case EAST: poseStack.mulPose(YP.rotationDegrees(90.f)); break;
			}

			if (scr.doTurnOnAnim) {
				long lt = System.currentTimeMillis() - scr.turnOnTime;
				float ft = ((float) lt) / 100.0f;
				if (ft >= 1.0f) {
					ft = 1.0f;
					scr.doTurnOnAnim = false;
				}
				if (!Log.RENDER_DIAG) poseStack.scale(ft, ft, 1.0f);
			}

			if (!scr.rotation.isNull)
				poseStack.mulPose(ZP.rotationDegrees(scr.rotation.angle));

			float sw = ((float) scr.size.x) * 0.5f - 2.f / 16.f;
			float sh = ((float) scr.size.y) * 0.5f - 2.f / 16.f;

			if (scr.rotation.isVertical) {
				float tmp = sw;
				sw = sh;
				sh = tmp;
			}

			if (Log.RENDER_DIAG) {
				logGLState(te); // dumps GL pipeline state once per second
				// DIAGNOSTIC: TWO quads via the same RenderType:
				//   GREEN at the screen's normal position
				//   RED  offset 8 blocks "up" in local frame (above the screen in world for most orientations)
				// If GREEN vanishes but RED stays visible → bug is location-specific (something
				// at the screen's exact spatial position overwrites our pixels). If both vanish in
				// sync → bug is general for our render submission regardless of location.
				var consumer = bufferSource.getBuffer(WD_DEBUG_QUAD_RT);
				var pose = poseStack.last().pose();
				// GREEN at normal position
				consumer.addVertex(pose, -sw, -sh, 0.52f).setColor(0.0f, 1.0f, 0.2f, 1.0f);
				consumer.addVertex(pose,  sw, -sh, 0.52f).setColor(0.0f, 1.0f, 0.2f, 1.0f);
				consumer.addVertex(pose,  sw,  sh, 0.52f).setColor(0.0f, 1.0f, 0.2f, 1.0f);
				consumer.addVertex(pose, -sw,  sh, 0.52f).setColor(0.0f, 1.0f, 0.2f, 1.0f);
				// RED offset +8 in local Y (for NORTH/SOUTH/EAST/WEST = +8 world Y = 8 blocks above)
				float off = 8.0f;
				consumer.addVertex(pose, -sw, -sh + off, 0.52f).setColor(1.0f, 0.1f, 0.1f, 1.0f);
				consumer.addVertex(pose,  sw, -sh + off, 0.52f).setColor(1.0f, 0.1f, 0.1f, 1.0f);
				consumer.addVertex(pose,  sw,  sh + off, 0.52f).setColor(1.0f, 0.1f, 0.1f, 1.0f);
				consumer.addVertex(pose, -sw,  sh + off, 0.52f).setColor(1.0f, 0.1f, 0.1f, 1.0f);
				if (bufferSource instanceof MultiBufferSource.BufferSource buf) {
					buf.endBatch(WD_DEBUG_QUAD_RT);
				}
			} else {
				Tesselator tesselator = Tesselator.getInstance();
				RenderSystem.enableDepthTest();
				RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
				RenderSystem.setShaderTexture(0, mcefBrowser.getRenderer().getTextureID());
				float brightness = (float) ClientConfig.screenBrightness;
				RenderSystem.setShaderColor(brightness, brightness, brightness, 1.0f);
				BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
				builder.addVertex(poseStack.last().pose(), -sw, -sh, 0.505f).setUv(0.f, 1.f).setColor(1.f, 1.f, 1.f, 1.f);
				builder.addVertex(poseStack.last().pose(), sw, -sh, 0.505f).setUv(1.f, 1.f).setColor(1.f, 1.f, 1.f, 1.f);
				builder.addVertex(poseStack.last().pose(), sw, sh, 0.505f).setUv(1.f, 0.f).setColor(1.f, 1.f, 1.f, 1.f);
				builder.addVertex(poseStack.last().pose(), -sw, sh, 0.505f).setUv(0.f, 0.f).setColor(1.f, 1.f, 1.f, 1.f);
				BufferUploader.drawWithShader(builder.buildOrThrow());
				RenderSystem.disableDepthTest();
			}
			drewSomething = true;

			poseStack.popPose();
		}

		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		} finally {
			tickFrameCounter(te, drewSomething);
		}
	}
}
