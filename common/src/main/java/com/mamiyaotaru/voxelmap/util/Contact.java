package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class Contact {
    public enum DisplayState {
        HIDDEN,
        BELOW_FRAME
    }

    public final LivingEntity entity;
    public final VoxelMapMobCategory category;
    public final UUID uuid;

    public DisplayState displayState = DisplayState.HIDDEN;

    public Component name;
    public double x;
    public double z;
    public double y;
    public double distance;
    public int yFudge;
    public float angle;
    public float rotationFactor;
    public float brightness;

    public Sprite icon;
    public Sprite armorIcon;
    public int baseColor = -1;
    public int armorColor = -1;

    public Contact(LivingEntity entity, VoxelMapMobCategory category) {
        this.entity = entity;
        this.category = category;
        this.uuid = entity.getUUID();
        updateLocation();
    }

    public void updateLocation() {
        this.x = this.entity.xo + (this.entity.getX() - this.entity.xo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        this.y = this.entity.getY() + this.yFudge;
        this.z = this.entity.zo + (this.entity.getZ() - this.entity.zo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}