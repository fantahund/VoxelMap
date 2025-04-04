package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;

public enum MobCategory {
    HOSTILE,
    NEUTRAL,
    TAMEABLE,
    PLAYER;

    public static MobCategory forEntity(Entity entity) {
        if (isHostile(entity)) {
            return HOSTILE;
        } else if (isPlayer(entity)) {
            return PLAYER;
        } else if (isOwnable(entity)) {
            return TAMEABLE;
        } else {
            return NEUTRAL;
        }
    }

    public static MobCategory forEntityType(EntityType<?> entityType) {
        if (entityType.getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
            return HOSTILE;
        } else if (entityType == EntityType.PLAYER) {
            return PLAYER;
        } else {
            return NEUTRAL;
        }
    }

    public static boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglin zombifiedPiglinEntity) {
            return zombifiedPiglinEntity.getPersistentAngerTarget() != null && zombifiedPiglinEntity.getPersistentAngerTarget().equals(VoxelConstants.getPlayer().getUUID());
        } else if (entity instanceof Enemy) {
            return true;
        } else if (entity instanceof Bee beeEntity) {
            return beeEntity.isAngry();
        } else {
            if (entity instanceof PolarBear polarBearEntity) {

                for (PolarBear object : polarBearEntity.level().getEntitiesOfClass(PolarBear.class, polarBearEntity.getBoundingBox().inflate(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof Rabbit rabbitEntity) {
                return rabbitEntity.getVariant() == Rabbit.Variant.EVIL;
            } else if (entity instanceof Wolf wolfEntity) {
                return wolfEntity.isAngry();
            } else {
                return false;
            }
        }
    }

    public static boolean isOwnable(Entity entity) {
        return entity instanceof OwnableEntity;
    }

    public static boolean isPlayer(Entity entity) {
        return entity instanceof RemotePlayer;
    }

    public static boolean isNeutral(Entity entity) {
        return !isPlayer(entity) && !isHostile(entity);
    }
}
