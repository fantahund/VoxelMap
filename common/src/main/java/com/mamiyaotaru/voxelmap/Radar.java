package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.AbstractRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.MinimapContext;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapRenderTypes;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Properties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4fStack;

public class Radar extends AbstractRadar {
    private static final int SUBMIT_ICON = 20;
    private static final int SUBMIT_TEXT = 30;
    private final EntityMapImageManager entityMapImageManager;
    private final HashMap<EntityType<?>, MobIconConfig> iconConfigs = new HashMap<>();

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
        if (contact.category == VoxelMapMobCategory.PLAYER || contact.entity.hasCustomName()) {
            contact.name = radarOptions.showFullEntityNames ? contact.entity.getDisplayName() : getSimplifiedName(contact);
        }

        if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
            contact.yFudge = 1;
        }

        String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
        if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player player) || player.isModelPartShown(PlayerModelPart.CAPE))) {
            contact.rotationFactor += 180;
        }

        if (contact.icon == null) {
            contact.icon = entityMapImageManager.requestImageForMob(contact.entity, 32, radarOptions.outlines);
            contact.baseColor = getBaseColor(contact);
        }

        if (radarOptions.showPlayerHelmets && contact.category == VoxelMapMobCategory.PLAYER || radarOptions.showMobHelmets && contact.category != VoxelMapMobCategory.PLAYER) {
            contact.armorIcon = entityMapImageManager.requestImageForArmor(contact.entity, 32, radarOptions.outlines);
            contact.armorColor = getArmorColor(contact);
        }
    }

    @Override
    protected void updateContact(Contact contact) {
        if (contact.icon != null) {
            super.updateContact(contact);
        }
    }

    private void applyContactTransform(Matrix4fStack matrixStack, Contact contact, int x, int y, int scScale) {
        float distance = (float) (contact.distance / minimapContext.zoomScaleAdjusted);
        if (radarOptions.filtering) {
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
    }

    @Override
    public void renderMapMobs(Matrix4fStack matrixStack, RenderUtils.SubmitContext context, Contact.DisplayState displayState, int x, int y, int scScale, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        // Draw mob icons
        RenderType iconRenderType = VoxelMapRenderTypes.GUI_TEXTURED_NO_DEPTH_TEST.apply(EntityMapImageManager.resourceTextureAtlasMarker);
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            if (contact.displayState != displayState) {
                continue;
            }

            try {
                matrixStack.pushMatrix();
                applyContactTransform(matrixStack, contact, x, y, scScale);

                int colorMult;
                if (minimapContext.playerY - contact.y < 0) {
                    colorMult = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
                } else {
                    float brightness = Math.max(0.3F, contact.brightness);
                    colorMult = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
                }

                float zOffset = i * 0.01F;
                float yOffset = 0.0F;
                if (contact.entity.getVehicle() != null && isEntityShown(contact.entity.getVehicle())) {
                    yOffset = -4.0F;
                }

                int baseColor = ARGB.multiply(colorMult, contact.baseColor);
                float imageWidth = contact.icon.getIconWidth() / 8.0F;
                float imageHeight = contact.icon.getIconHeight() / 8.0F;
                RenderUtils.submitTexturedModalRect(context.order(SUBMIT_ICON), matrixStack, iconRenderType, contact.icon, x - (imageWidth / 2), y + yOffset - (imageHeight / 2), zOffset, imageWidth, imageHeight, baseColor);

                if (contact.armorIcon != null) {
                    int armorColor = ARGB.multiply(colorMult, contact.armorColor);
                    MobIconConfig iconConfig = getIconConfig(contact);
                    float armorOffset = iconConfig.armorOffset();
                    float armorWidth = contact.armorIcon.getIconWidth() / 8.0F;
                    float armorHeight = contact.armorIcon.getIconHeight() / 8.0F;
                    RenderUtils.submitTexturedModalRect(context.order(SUBMIT_ICON), matrixStack, iconRenderType, contact.armorIcon, x - (armorWidth / 2), y + yOffset + armorOffset - (armorHeight / 2), zOffset, armorWidth, armorHeight, armorColor);
                }
            } catch (Exception e) {
                VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
            } finally {
                matrixStack.popMatrix();
            }
        }
        context.flush();

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
                    applyContactTransform(matrixStack, contact, x, y, scScale);
                    matrixStack.scale(scaleFactor, scaleFactor, 1.0F);

                    RenderUtils.submitCenteredString(context.order(SUBMIT_TEXT), matrixStack, contact.name, x / scaleFactor, (y + 3) / scaleFactor, zOffset, 0xFFFFFFFF, true);
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob name! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
                } finally {
                    matrixStack.popMatrix();
                }
            }
        }

        matrixStack.popMatrix();
    }

    private Component getSimplifiedName(Contact contact) {
        MutableComponent copy = contact.entity.getName().copy();
        copy.withColor(contact.entity.getTeamColor());

        return copy;
    }

    private int getBaseColor(Contact contact) {
        return 0xFFFFFFFF;
    }

    private int getArmorColor(Contact contact) {
        if (contact.entity instanceof Sheep sheep) {
            return sheep.getColor().getMapColor().col | 0xFF000000;
        }

        return 0xFFFFFFFF;
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
