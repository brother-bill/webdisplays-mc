/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.compat;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Compatibility helper for Iris Shaders mod.
 * When Iris shaders are active, the vanilla shaders are replaced which can
 * cause custom texture rendering (like browser views) to not display correctly.
 * This class provides methods to detect shader state and work around issues.
 */
public class IrisCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(IrisCompat.class);
    private static boolean initialized = false;
    private static boolean available = false;
    private static Object irisApiInstance = null;
    private static Method isShaderPackInUseMethod = null;

    /**
     * Initialize the compatibility layer by checking if Iris is loaded
     * and caching the API methods.
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            if (ModList.get().isLoaded("iris") || ModList.get().isLoaded("oculus")) {
                // Try to get the IrisApi instance
                Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                Method getInstanceMethod = irisApiClass.getMethod("getInstance");
                irisApiInstance = getInstanceMethod.invoke(null);

                // Get the isShaderPackInUse method
                isShaderPackInUseMethod = irisApiClass.getMethod("isShaderPackInUse");

                available = true;
                LOGGER.info("Iris/Oculus compatibility enabled - will detect shader state for browser rendering");
            }
        } catch (ClassNotFoundException e) {
            LOGGER.debug("Iris API not found, skipping compatibility layer");
            available = false;
        } catch (Exception e) {
            LOGGER.warn("Failed to initialize Iris compatibility", e);
            available = false;
        }
    }

    /**
     * Check if Iris/Oculus compatibility is available.
     */
    public static boolean isAvailable() {
        if (!initialized) init();
        return available;
    }

    /**
     * Check if a shader pack is currently in use (loaded and compiled successfully).
     * Returns false if Iris is not loaded or no shader pack is active.
     */
    public static boolean isShaderPackInUse() {
        if (!isAvailable() || irisApiInstance == null || isShaderPackInUseMethod == null) {
            LOGGER.debug("[IrisCompat] isShaderPackInUse: not available (available={}, instance={}, method={})",
                available, irisApiInstance != null, isShaderPackInUseMethod != null);
            return false;
        }

        try {
            boolean result = (boolean) isShaderPackInUseMethod.invoke(irisApiInstance);
            LOGGER.debug("[IrisCompat] isShaderPackInUse: {}", result);
            return result;
        } catch (Exception e) {
            LOGGER.warn("[IrisCompat] Failed to check shader pack state: {}", e.getMessage());
            return false;
        }
    }
}
