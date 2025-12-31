/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public abstract class Log {
    private static final Logger logger = LogUtils.getLogger();

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
