package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public enum VoxelMapMobCategory {
    PLAYER,
    HOSTILE,
    NEUTRAL;
//    TAMEABLE,

    public static VoxelMapMobCategory forEntity(Entity entity) {
        if (isPlayer(entity)) {
            return PLAYER;
        } else if (isHostile(entity)) {
            return HOSTILE;
        } else {
            return NEUTRAL;
        }
    }

    public static VoxelMapMobCategory forEntityType(EntityType<?> entityType) {
        if (entityType == EntityType.PLAYER) {
            return PLAYER;
        } else if (entityType.getCategory() == MobCategory.MONSTER) {
            return HOSTILE;
        } else {
            return NEUTRAL;
        }
    }

    public static boolean isHostile(Entity entity) {
        switch (entity) {
            case Enemy enemy -> {
                return true;
            }
            case Rabbit rabbit -> {
                return rabbit.getVariant() == Rabbit.Variant.EVIL;
            }
            case NeutralMob neutralMob -> {
                if (neutralMob.getPersistentAngerTarget() != null) {
                    return neutralMob.getPersistentAngerTarget().getUUID().equals(VoxelConstants.getPlayer().getUUID());
                }
            }
            default -> {}
        }

        return false;
    }

    public static boolean isOwnable(Entity entity) {
        return entity instanceof OwnableEntity;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof Player;
    }

    public static boolean isNeutral(Entity entity) {
        return !isPlayer(entity) && !isHostile(entity);
    }
}
