/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.compat;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Compatibility helper for ImmediatelyFast mod.
 * ImmediatelyFast batches immediate mode rendering calls, which can interfere
 * with WebDisplays' direct RenderSystem texture rendering for browser views.
 * This class provides a way to force flush the batched buffers before our custom rendering.
 */
public class ImmediatelyFastCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(ImmediatelyFastCompat.class);
    private static boolean initialized = false;
    private static boolean available = false;
    private static Method tryForceDrawHudBuffersMethod = null;
    private static Method getHudBatchingVertexConsumersMethod = null;
    private static Method endBatchMethod = null;
    private static Field isHudBatchingField = null;

    /**
     * Initialize the compatibility layer by checking if ImmediatelyFast is loaded
     * and caching the reflection methods.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            if (ModList.get().isLoaded("immediatelyfast")) {
                Class<?> batchingBuffersClass = Class.forName("net.raphimc.immediatelyfast.feature.batching.BatchingBuffers");

                // Try to get the tryForceDrawHudBuffers method
                try {
                    tryForceDrawHudBuffersMethod = batchingBuffersClass.getMethod("tryForceDrawHudBuffers");
                } catch (NoSuchMethodException e) {
                    LOGGER.debug("tryForceDrawHudBuffers method not found");
                }

                // Try to get the isHudBatching field
                try {
                    isHudBatchingField = batchingBuffersClass.getDeclaredField("isHudBatching");
                    isHudBatchingField.setAccessible(true);
                } catch (NoSuchFieldException e) {
                    LOGGER.debug("isHudBatching field not found");
                }

                // Try to get the HudBatchingBufferSource and its endBatch method
                try {
                    getHudBatchingVertexConsumersMethod = batchingBuffersClass.getMethod("getHudBatchingVertexConsumers");
                    Class<?> hudBatchingClass = Class.forName("net.raphimc.immediatelyfast.feature.batching.HudBatchingBufferSource");
                    endBatchMethod = hudBatchingClass.getMethod("endBatch");
                } catch (NoSuchMethodException | ClassNotFoundException e) {
                    LOGGER.debug("HudBatchingBufferSource methods not found");
                }

                available = tryForceDrawHudBuffersMethod != null || endBatchMethod != null;
                if (available) {
                    LOGGER.info("ImmediatelyFast compatibility enabled - will flush batching buffers before browser rendering");
                }
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("ImmediatelyFast not found, skipping compatibility layer");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize ImmediatelyFast compatibility", e);
            available = false;
        }
    }

    /**
     * Check if ImmediatelyFast compatibility is available.
     */
    public static boolean isAvailable() {
        if (!initialized) init();
        return available;
    }

    /**
     * Check if ImmediatelyFast is currently batching HUD rendering.
     */
    public static boolean isHudBatching() {
        if (!isAvailable() || isHudBatchingField == null) return false;

        try {
            return (boolean) isHudBatchingField.get(null);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Force ImmediatelyFast to flush its batched HUD buffers.
     * This should be called before any custom RenderSystem rendering that
     * might conflict with ImmediatelyFast's batching.
     */
    public static void forceFlushBatching() {
        if (!isAvailable()) return;

        try {
            // First try the dedicated method
            if (tryForceDrawHudBuffersMethod != null) {
                tryForceDrawHudBuffersMethod.invoke(null);
            }

            // Also try to directly end the batch on HudBatchingBufferSource
            if (getHudBatchingVertexConsumersMethod != null && endBatchMethod != null) {
                Object hudBatching = getHudBatchingVertexConsumersMethod.invoke(null);
                if (hudBatching != null) {
                    endBatchMethod.invoke(hudBatching);
                }
            }
        } catch (Exception e) {
            // Log once for debugging, but don't spam
            LOGGER.debug("Failed to flush ImmediatelyFast batching: {}", e.getMessage());
        }
    }
}
