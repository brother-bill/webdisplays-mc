/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

public abstract class ScreenRights {
    public static final int CHANGE_URL = 1; //Change URL AND run JavaScript
    /**
     * use {@link ScreenRights#INTERACT instead}
     */
    @Deprecated(forRemoval = true)
    public static final int CLICK = 2; //Click AND type
    public static final int INTERACT = 2; //Click AND type
    public static final int MANAGE_FRIEND_LIST = 4;
    public static final int MANAGE_OTHER_RIGHTS = 8;
    public static final int MANAGE_UPGRADES = 16; //Manage upgrades AND peripherals AND autoVolume
    @Deprecated(forRemoval = true)
    public static final int CHANGE_RESOLUTION = 32; //Change resolution AND rotation
    public static final int MODIFY_SCREEN = 32; //Change resolution AND rotation

    public static final int NONE = 0;
    public static final int ALL = 0xFF;
    public static final int DEFAULTS = CHANGE_URL | INTERACT | MANAGE_UPGRADES | MODIFY_SCREEN;
}
