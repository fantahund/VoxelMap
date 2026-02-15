package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.MinimapContext;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4fStack;

import java.util.HashMap;
import java.util.Properties;

public class Radar extends AbstractRadar {
    private final EntityMapImageManager entityMapImageManager;
    private final HashMap<Entity, MobIconConfig> iconConfigs = new HashMap<>();

    public Radar() {
        super();
        entityMapImageManager = new EntityMapImageManager();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        entityMapImageManager.reset();
    }

    @Override
    public void onTickInGame(Matrix4fStack matrixStack, MinimapContext minimapContext) {
        entityMapImageManager.onRenderTick(matrixStack);
        super.onTickInGame(matrixStack, minimapContext);
    }

    @Override
    protected void initContact(Contact contact) {
        if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
            contact.yFudge = 1;
        }

        String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
        if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player player) || player.isModelPartShown(PlayerModelPart.CAPE))) {
            contact.rotationFactor += 180;
        }

        if (contact.icon == null) {
            contact.icon = entityMapImageManager.requestImageForMob(contact.entity, 32, radarOptions.outlines);
        }

        if (radarOptions.showHelmetsPlayers && contact.category == VoxelMapMobCategory.PLAYER || radarOptions.showHelmetsMobs && contact.category != VoxelMapMobCategory.PLAYER) {
            contact.armorIcon = entityMapImageManager.requestImageForArmor(contact.entity, 32, radarOptions.outlines);
        }
    }

    @Override
    protected void updateContact(Contact contact) {
        if (contact.icon != null) {
            super.updateContact(contact);
        }
    }

    @Override
    public void renderMapMobs(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Contact.DisplayState displayState, int x, int y, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        // Draw mob icons
        RenderType iconRenderType = VoxelMapRenderTypes.GUI_TEXTURED_LEQUAL_DEPTH_TEST.apply(EntityMapImageManager.resourceTextureAtlasMarker);
        VertexConsumer iconBuffer = bufferSource.getBuffer(iconRenderType);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            if (contact.displayState != displayState) {
                continue;
            }

            try {
                matrixStack.pushMatrix();
                applyContactTransform(matrixStack, contact, x, y);

                int color;
                if (minimapContext.playerY - contact.y < 0) {
                    color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
                } else {
                    float brightness = Math.max(0.3F, contact.brightness);
                    color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
                }

                float zOffset = i * 0.01F;
                float yOffset = 0.0F;
                if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
                    yOffset = -4.0F;
                }

                float imageWidth = contact.icon.getIconWidth() / 8.0F;
                float imageHeight = contact.icon.getIconHeight() / 8.0F;
                RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, contact.icon, x - (imageWidth / 2), y + yOffset - (imageHeight / 2), zOffset, imageWidth, imageHeight, color);

                if (contact.armorIcon != null) {
                    MobIconConfig iconConfig = getIconConfig(contact);
                    float armorOffset = iconConfig.armorOffset();
                    float armorWidth = contact.armorIcon.getIconWidth() / 8.0F;
                    float armorHeight = contact.armorIcon.getIconHeight() / 8.0F;
                    RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, contact.armorIcon, x - (armorWidth / 2), y + yOffset + armorOffset - (armorHeight / 2), zOffset, armorWidth, armorHeight, color);
                }
            } catch (Exception e) {
                VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
            } finally {
                matrixStack.popMatrix();
            }
        }
        bufferSource.endBatch(iconRenderType);

        // Draw mob names
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            if (contact.displayState != displayState) {
                continue;
            }

            if (contact.name != null && ((radarOptions.showPlayerNames && contact.category == VoxelMapMobCategory.PLAYER) || (radarOptions.showMobNames && contact.category != VoxelMapMobCategory.PLAYER))) {
                try {
                    float scaleFactor = radarOptions.fontScale / 4.0F;
                    float zOffset = i * 0.01F;

                    matrixStack.pushMatrix();
                    applyContactTransform(matrixStack, contact, x, y);
                    matrixStack.scale(scaleFactor, scaleFactor, 1.0F);

                    RenderUtils.drawCenteredString(matrixStack, bufferSource, contact.name, x / scaleFactor, (y + 3) / scaleFactor, zOffset, 0xFFFFFFFF, false);
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob name! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
                } finally {
                    matrixStack.popMatrix();
                }
            }
        }

        matrixStack.popMatrix();
    }

    private MobIconConfig getIconConfig(Contact contact) {
        return iconConfigs.computeIfAbsent(contact.entity, key -> {
            Properties properties =  entityMapImageManager.getMobProperties(contact.entity);
            float armorOffset = Float.parseFloat(properties.getProperty("helmetOffset", "0.0"));

            return new MobIconConfig(armorOffset);
        });
    }

    public void onJoinServer() {
        entityMapImageManager.reset();
    }

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
    }

    private record MobIconConfig(float armorOffset) {
    }
}
