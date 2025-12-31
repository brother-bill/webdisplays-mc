/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities.data;

import net.montoyo.wd.utilities.math.Vector3i;

public enum BlockSide {
    BOTTOM(new Vector3i(0, 0, -1), new Vector3i(1, 0, 0), new Vector3i(0, -1, 0)),
    TOP(new Vector3i(0, 0, -1), new Vector3i(1, 0, 0), new Vector3i(0, 1, 0)),
    NORTH(new Vector3i(0, 1, 0), new Vector3i(-1, 0, 0), new Vector3i(0, 0, -1)),
    SOUTH(new Vector3i(0, 1, 0), new Vector3i(1, 0, 0), new Vector3i(0, 0, 1)),
    WEST(new Vector3i(0, 1, 0), new Vector3i(0, 0, 1), new Vector3i(-1, 0, 0)),
    EAST(new Vector3i(0, 1, 0), new Vector3i(0, 0, -1), new Vector3i(1, 0, 0));

    public final Vector3i up, down;
    public final Vector3i left, right;
    public final Vector3i forward, backward;

    // mostly used for click coordinate calculations
    public final Vector3i horizontal, vertical;

    BlockSide(Vector3i u, Vector3i r, Vector3i f) {
        up = u;
        right = r;
        forward = f;
        down = u.clone().neg();
        left = r.clone().neg();
        backward = f.clone().neg();

        horizontal = new Vector3i(Math.abs(r.x), Math.abs(r.y), Math.abs(r.z));
        vertical = new Vector3i(Math.abs(u.x), Math.abs(u.y), Math.abs(u.z));
    }

    public BlockSide reverse() {
        int side = ordinal();
        int div = side / 2;
        int rest = 1 - side % 2;

        return values()[div * 2 + rest];
    }

    public static int reverse(int side) {
        int div = side / 2;
        int rest = 1 - side % 2;

        return div * 2 + rest;
    }

    public static BlockSide fromInt(int s) {
        BlockSide[] values = values();
        return (s < 0 || s >= values.length) ? null : values[s];
    }
}
