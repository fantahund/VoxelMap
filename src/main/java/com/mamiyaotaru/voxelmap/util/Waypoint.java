package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.entity.Entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.TreeSet;

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
        return -16777216 + ((int) (this.red * 255.0F) << 16) + ((int) (this.green * 255.0F) << 8) + (int) (this.blue * 255.0F);
    }

    public boolean isActive() {
        return this.enabled && this.inWorld && this.inDimension;
    }

    public int getX() {
        return (int) ((double) this.x / VoxelConstants.getMinecraft().player.world.getDimension().coordinateScale());
    }

    public int getZ() {
        return (int) ((double) this.z / VoxelConstants.getMinecraft().player.world.getDimension().coordinateScale());
    }

    public int getY() {
        return this.y;
    }

    public void setX(int x) {
        this.x = (int) ((double) x * VoxelConstants.getMinecraft().player.world.getDimension().coordinateScale());
    }

    public void setZ(int z) {
        this.z = (int) ((double) z * VoxelConstants.getMinecraft().player.world.getDimension().coordinateScale());
    }

    public void setY(int y) {
        this.y = y;
    }

    public int compareTo(Waypoint arg0) {
        double myDistance = this.getDistanceSqToEntity(VoxelConstants.getMinecraft().player);
        double comparedDistance = arg0.getDistanceSqToEntity(VoxelConstants.getMinecraft().player);
        return Double.compare(myDistance, comparedDistance);
    }

    public double getDistanceSqToEntity(Entity par1Entity) {
        double var2 = (double) this.getX() + 0.5 - par1Entity.getX();
        double var4 = (double) this.getY() + 0.5 - par1Entity.getY();
        double var6 = (double) this.getZ() + 0.5 - par1Entity.getZ();
        return var2 * var2 + var4 * var4 + var6 * var6;
    }

    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        } else if (!(otherObject instanceof Waypoint otherWaypoint)) {
            return false;
        } else {
            return this.name.equals(otherWaypoint.name) && this.imageSuffix.equals(otherWaypoint.imageSuffix) && this.world.equals(otherWaypoint.world) && this.x == otherWaypoint.x && this.y == otherWaypoint.y && this.z == otherWaypoint.z && this.red == otherWaypoint.red && this.green == otherWaypoint.green && this.blue == otherWaypoint.blue && this.dimensions.equals(otherWaypoint.dimensions);
        }
    }
}
