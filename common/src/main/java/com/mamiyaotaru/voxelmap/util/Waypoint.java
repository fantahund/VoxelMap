package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.TreeSet;
import net.minecraft.client.Camera;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Waypoint implements Serializable, Comparable<Waypoint> {
    @Serial
    private static final long serialVersionUID = 8136790917447997951L;
    public String name;
    public String imageSuffix;
    public String world;
    public final TreeSet<DimensionContainer> dimensions;
    public int x;
    public int z;
    public int y;
    public boolean enabled;
    public boolean inWorld = true;
    public boolean inDimension = true;
    public float red;
    public float green;
    public float blue;

    public Waypoint(String name, int x, int z, int y, boolean enabled, float red, float green, float blue, String suffix, String world, TreeSet<DimensionContainer> dimensions) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.y = y;
        this.enabled = enabled;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.imageSuffix = suffix.toLowerCase(Locale.ROOT);
        this.world = world;
        this.dimensions = dimensions;
    }

    public int getUnifiedColor() {
        return getUnifiedColor(1.0f);
    }

    public int getUnifiedColor(float alpha) {
        return ARGB.colorFromFloat(alpha, red, green, blue);
    }

    public boolean isActive() {
        return this.enabled && this.inWorld && this.inDimension;
    }

    public int getX() {
        return (int) (this.x / VoxelConstants.getPlayer().level().dimensionType().coordinateScale());
    }

    public int getZ() {
        return (int) (this.z / VoxelConstants.getPlayer().level().dimensionType().coordinateScale());
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = (int) (x * VoxelConstants.getPlayer().level().dimensionType().coordinateScale());
    }

    public void setZ(int z) {
        this.z = (int) (z * VoxelConstants.getPlayer().level().dimensionType().coordinateScale());
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int compareTo(Waypoint o) {
        double myDistance = this.getDistanceSqToEntity(VoxelConstants.getPlayer());
        double comparedDistance = o.getDistanceSqToEntity(VoxelConstants.getPlayer());
        return Double.compare(myDistance, comparedDistance);
    }

    public double getDistanceSqToEntity(Entity par1Entity) {
        double var2 = this.getX() + 0.5 - par1Entity.getX();
        double var4 = this.getY() + 0.5 - par1Entity.getY();
        double var6 = this.getZ() + 0.5 - par1Entity.getZ();
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    public double getDistanceSqToCamera(Camera par1Entity) {
        Vec3 pos = par1Entity.position();
        double var2 = this.getX() + 0.5 - pos.x;
        double var4 = this.getY() + 0.5 - pos.y;
        double var6 = this.getZ() + 0.5 - pos.z;
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof Waypoint otherWaypoint)) {
            return false;
        } else {
            return this.name.equals(otherWaypoint.name) && this.imageSuffix.equals(otherWaypoint.imageSuffix) && this.world.equals(otherWaypoint.world) && this.x == otherWaypoint.x && this.y == otherWaypoint.y && this.z == otherWaypoint.z && this.red == otherWaypoint.red && this.green == otherWaypoint.green && this.blue == otherWaypoint.blue && this.dimensions.equals(otherWaypoint.dimensions);
        }
    }
}
