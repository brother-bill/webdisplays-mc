/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

/**
 * Interface for WebDisplays player data.
 * In NeoForge 1.21+, this is implemented as a Data Attachment.
 */
public interface IWDDCapability {
    boolean isFirstRun();
    void clearFirstRun();
    void cloneTo(IWDDCapability dst);
}
