package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class Contact {
    public double x;
    public double z;
    public int y;
    public int yFudge;
    public float angle;
    public double distance;
    public float brightness;
    public EnumMobs type;
    public final boolean vanillaType;
    public boolean custom;
    public UUID uuid;
    public Component name;
    public int rotationFactor;
    public final LivingEntity entity;
    public Sprite icon;
    public Sprite armorIcon;
    public int armorColor = -1;

    public Contact(LivingEntity entity, EnumMobs type) {
        this.entity = entity;
        this.type = type;
        this.uuid = entity.getUUID();
        this.vanillaType = type != EnumMobs.GENERICNEUTRAL && type != EnumMobs.GENERICHOSTILE && type != EnumMobs.GENERICTAME && type != EnumMobs.UNKNOWN;
        this.name = entity.hasCustomName() || entity instanceof Player ? entity.getName() : null;
        updateLocation();
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public void setRotationFactor(int rotationFactor) {
        this.rotationFactor = rotationFactor;
    }

    public void setArmorColor(int armorColor) {
        this.armorColor = armorColor;
    }

    public void updateLocation() {
        this.x = this.entity.xo + (this.entity.getX() - this.entity.xo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        this.y = (int) this.entity.getY() + this.yFudge;
        this.z = this.entity.zo + (this.entity.getZ() - this.entity.zo) * VoxelConstants.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }
}