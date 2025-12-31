/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities.math;

import net.minecraft.world.phys.AABB;

public final class MutableAABB extends AABB {
    private double mutableMinX;
    private double mutableMinY;
    private double mutableMinZ;
    private double mutableMaxX;
    private double mutableMaxY;
    private double mutableMaxZ;

    public MutableAABB() {
        super(0, 0, 0, 0, 0, 0);
        this.mutableMinX = 0;
        this.mutableMinY = 0;
        this.mutableMinZ = 0;
        this.mutableMaxX = 0;
        this.mutableMaxY = 0;
        this.mutableMaxZ = 0;
    }

    public MutableAABB(Vector3i pos) {
        super(pos.x, pos.y, pos.z, pos.x, pos.y, pos.z);
        this.mutableMinX = pos.x;
        this.mutableMinY = pos.y;
        this.mutableMinZ = pos.z;
        this.mutableMaxX = pos.x;
        this.mutableMaxY = pos.y;
        this.mutableMaxZ = pos.z;
    }

    public MutableAABB(Vector3i a, Vector3i b) {
        super(a.x, a.y, a.z, b.x, b.y, b.z);
        this.mutableMinX = a.x;
        this.mutableMinY = a.y;
        this.mutableMinZ = a.z;
        this.mutableMaxX = b.x;
        this.mutableMaxY = b.y;
        this.mutableMaxZ = b.z;
    }

    public MutableAABB(net.minecraft.world.phys.AABB bb) {
        super(bb.minX, bb.minY, bb.minZ, bb.maxX, bb.maxY, bb.maxZ);
        this.mutableMinX = bb.minX;
        this.mutableMinY = bb.minY;
        this.mutableMinZ = bb.minZ;
        this.mutableMaxX = bb.maxX;
        this.mutableMaxY = bb.maxY;
        this.mutableMaxZ = bb.maxZ;
    }

    public MutableAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        super(x1, y1, z1, x2, y2, z2);
        this.mutableMinX = x1;
        this.mutableMinY = y1;
        this.mutableMinZ = z1;
        this.mutableMaxX = x2;
        this.mutableMaxY = y2;
        this.mutableMaxZ = z2;
    }

    public double getMinX() {
        return mutableMinX;
    }

    public double getMinY() {
        return mutableMinY;
    }

    public double getMinZ() {
        return mutableMinZ;
    }

    public double getMaxX() {
        return mutableMaxX;
    }

    public double getMaxY() {
        return mutableMaxY;
    }

    public double getMaxZ() {
        return mutableMaxZ;
    }

    public MutableAABB expand(Vector3i vec) {
        if (vec.x > mutableMaxX)
            mutableMaxX = vec.x;
        else if (vec.x < mutableMinX)
            mutableMinX = vec.x;

        if (vec.y > mutableMaxY)
            mutableMaxY = vec.y;
        else if (vec.y < mutableMinY)
            mutableMinY = vec.y;

        if (vec.z > mutableMaxZ)
            mutableMaxZ = vec.z;
        else if (vec.z < mutableMinZ)
            mutableMinZ = vec.z;

        return this;
    }

    @Override
    public AABB move(double x, double y, double z) {
        mutableMinX += x;
        mutableMinY += y;
        mutableMinZ += z;
        mutableMaxX += x;
        mutableMaxY += y;
        mutableMaxZ += z;
        return this;
    }

    public net.minecraft.world.phys.AABB toMc() {
        return new AABB(mutableMinX, mutableMinY, mutableMinZ, mutableMaxX, mutableMaxY, mutableMaxZ);
    }

    public void setAndCheck(double x1, double y1, double z1, double x2, double y2, double z2) {
        mutableMinX = Math.min(x1, x2);
        mutableMinY = Math.min(y1, y2);
        mutableMinZ = Math.min(z1, z2);

        mutableMaxX = Math.max(x1, x2);
        mutableMaxY = Math.max(y1, y2);
        mutableMaxZ = Math.max(z1, z2);
    }

    public void expand(double x1, double y1, double z1, double x2, double y2, double z2) {
        mutableMinX = Math.min(mutableMinX, Math.min(x1, x2));
        mutableMinY = Math.min(mutableMinY, Math.min(y1, y2));
        mutableMinZ = Math.min(mutableMinZ, Math.min(z1, z2));

        mutableMaxX = Math.max(mutableMaxX, Math.max(x1, x2));
        mutableMaxY = Math.max(mutableMaxY, Math.max(y1, y2));
        mutableMaxZ = Math.max(mutableMaxZ, Math.max(z1, z2));
    }
}
