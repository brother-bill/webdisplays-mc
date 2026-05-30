/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public abstract class Log {
    private static final Logger logger = LogUtils.getLogger();

    // Debug flags for troubleshooting specific systems
    public static boolean DEBUG_KEYBOARD = false;
    public static boolean DEBUG_VIDEO_SYNC = false; // todo-bb

    // Global dev-only debug toggle. Enable with JVM arg: -Dwebdisplays.debug=true
    public static final boolean DEBUG = Boolean.getBoolean("webdisplays.debug");

    // Renderer-only diagnostic mode. Enable with JVM arg: -Dwebdisplays.diag=true
    // Causes ScreenRenderer to draw a solid GREEN quad instead of the real browser content.
    // Use only when actively debugging the screen-blank bug (e.g. capturing with RenderDoc).
    // Implies DEBUG-style logging but does not require it.
    public static final boolean RENDER_DIAG = Boolean.getBoolean("webdisplays.diag");

    // Spatial audio diagnostic mode. Enable with JVM arg: -Dwebdisplays.audiodiag=true
    // Causes ScreenBlockEntity.updateTrackDistance to log per-screen state (autoVolume,
    // videoType, browser, computed vol, JS payload) at INFO level every ~3 seconds.
    // Use to diagnose "audio not attenuating with distance" reports without enabling full DEBUG.
    public static final boolean AUDIO_DIAG = Boolean.getBoolean("webdisplays.audiodiag");

    public static void debug(String what, Object... data) {
        logger.debug(String.format(what, data));
    }

    public static void dbg(String tag, String fmt, Object... data) {
        if (!DEBUG) return;
        logger.info("[wd-dbg/{}] {}", tag, String.format(fmt, data));
    }

    public static void info(String what, Object... data) {
        logger.info(String.format(what, data));
    }

    public static void warning(String what, Object... data) {
        logger.warn(String.format(what, data));
    }

    public static void error(String what, Object... data) {
        logger.error(String.format(what, data));
    }

    public static void infoEx(String what, Throwable e, Object... data) {
        logger.info(String.format(what, data), e);
    }

    public static void warningEx(String what, Throwable e, Object... data) {
        logger.warn(String.format(what, data), e);
    }

    public static void errorEx(String what, Throwable e, Object... data) {
        logger.error(String.format(what, data), e);
    }
}
