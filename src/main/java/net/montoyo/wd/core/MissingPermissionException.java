/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.server.level.ServerPlayer;

public class MissingPermissionException extends Exception {
    private final int permission;
    private final ServerPlayer player;

    public MissingPermissionException(int p, ServerPlayer ply) {
        super("Player " + ply.getName() + " is missing permission " + p);
        permission = p;
        player = ply;
    }

    public int getPermission() {
        return permission;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}
