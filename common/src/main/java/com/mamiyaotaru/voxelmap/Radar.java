package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.render.RenderUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.render.VoxelMapPipelines;
import com.mojang.math.Axis;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.Properties;

public class Radar extends AbstractRadar {
    private final EntityMapImageManager entityMapImageManager;
    private final HashMap<EntityType<?>, MobIconConfig> iconConfigs = new HashMap<>();

    public Radar() {
        super();
        entityMapImageManager = VoxelConstants.getVoxelMapInstance().getEntityMapImageManager();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
    }

    @Override
    protected void initContact(Contact contact) {
        if (contact.category == VoxelMapMobCategory.PLAYER || contact.entity.hasCustomName()) {
            contact.name = radarOptions.showFullNames.get() ? contact.entity.getDisplayName() : getSimplifiedName(contact);
        }

        if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
            contact.yFudge = 1;
        }

        String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
        if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player player) || player.isModelPartShown(PlayerModelPart.CAPE))) {
            contact.rotationFactor += 180;
        }

        if (contact.icon == null) {
            contact.icon = entityMapImageManager.requestImageForMob(contact.entity, 32, radarOptions.outlines.get());
        }

        if (radarOptions.showPlayerHelmets.get() && contact.category == VoxelMapMobCategory.PLAYER || radarOptions.showMobHelmets.get() && contact.category != VoxelMapMobCategory.PLAYER) {
            contact.armorIcon = entityMapImageManager.requestImageForArmor(contact.entity, 32, radarOptions.outlines.get());
        }
    }

    @Override
    protected void updateContact(Contact contact) {
        if (contact.icon != null) {
            super.updateContact(contact);
        }
    }

    @Override
    public void renderMapMobs(Matrix4fStack matrixStack, Contact.DisplayState displayState, int x, int y, int scScale, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        RenderUtils.beginBatch(VoxelMapPipelines.GUI_TEXTURED_LEQUAL_DEPTH_TEST, EntityMapImageManager.resourceTextureAtlasMarker);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            if (contact.displayState != displayState) {
                continue;
            }

            try {
                matrixStack.pushMatrix();

                float distance = (float) (contact.distance / minimapContext.zoomScaleAdjusted);
                if (radarOptions.filtering.get()) {
                    matrixStack.translate(x, y, 0.0F);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
                    matrixStack.translate(0.0F, -distance, 0.0F);
                    matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
                    matrixStack.translate(-x, -y, 0.0F);
                } else {
                    double wayZ = Math.cos(Math.toRadians(contact.angle)) * distance;
                    double wayX = Math.sin(Math.toRadians(contact.angle)) * distance;
                    matrixStack.translate((float) Math.round(-wayX * scScale) / scScale, (float) Math.round(-wayZ * scScale) / scScale, 0.0F);
                }

                int color;
                if (minimapContext.playerY - contact.y < 0) {
                    color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
                } else {
                    float brightness = Math.max(0.3F, contact.brightness);
                    color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
                }

                float zOffset = (i % 1000.0F) * 0.1F;
                float yOffset = 0.0F;
                if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
                    yOffset = -4.0F;
                }

                float imageWidth = contact.icon.getIconWidth() / 8.0F;
                float imageHeight = contact.icon.getIconHeight() / 8.0F;
                RenderUtils.drawSpriteRect(matrixStack, contact.icon, x - (imageWidth / 2), y + yOffset - (imageHeight / 2), zOffset, imageWidth, imageHeight, color);

                if (contact.armorIcon != null) {
                    MobIconConfig iconConfig = getIconConfig(contact);
                    float armorOffset = iconConfig.armorOffset();
                    float armorWidth = contact.armorIcon.getIconWidth() / 8.0F;
                    float armorHeight = contact.armorIcon.getIconHeight() / 8.0F;
                    RenderUtils.drawSpriteRect(matrixStack, contact.armorIcon, x - (armorWidth / 2), y + yOffset + armorOffset - (armorHeight / 2), zOffset, armorWidth, armorHeight, color);
                }

                if (contact.name != null) {
                    float fontScale = radarOptions.fontScale.get() / 4.0F;
                    matrixStack.scale(fontScale, fontScale, 1.0F);
                    RenderUtils.drawCenteredString(matrixStack, contact.name, x / fontScale, (y + 3) / fontScale, zOffset, 0xFFFFFFFF, true);
                }
            } catch (Exception e) {
                VoxelConstants.getLogger().error("Error rendering mob icon! {} contact type {}", e.getLocalizedMessage(), BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
            } finally {
                matrixStack.popMatrix();
            }
        }
        RenderUtils.endBatch();

        matrixStack.popMatrix();
    }

    private Component getSimplifiedName(Contact contact) {
        MutableComponent copy = contact.entity.getName().copy();
        copy.withColor(contact.entity.getTeamColor());

        return copy;
    }

    private MobIconConfig getIconConfig(Contact contact) {
        return iconConfigs.computeIfAbsent(contact.entity.getType(), key -> {
            Properties properties = entityMapImageManager.getCustomMobProperties(key);
            float armorOffset = Float.parseFloat(properties.getProperty("helmetOffset", "0.0"));

            return new MobIconConfig(armorOffset);
        });
    }

    @Override
    public void onJoinServer() {
        super.onJoinServer();
        entityMapImageManager.reset();
    }

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
    }

    private record MobIconConfig(float armorOffset) {
    }
}
