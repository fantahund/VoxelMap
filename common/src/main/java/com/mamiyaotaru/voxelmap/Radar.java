package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.RenderUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mamiyaotaru.voxelmap.util.VoxelMapRenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.joml.Matrix4fStack;

import java.util.ArrayList;

public class Radar implements IRadar {
    private final MapSettingsManager minimapOptions;
    private final RadarSettingsManager options;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    private final EntityMapImageManager entityMapImageManager;
    private final Minecraft minecraft = Minecraft.getInstance();

    private LayoutVariables layoutVariables;
    private int timer = 500;
    private float direction;
    private double lastX;
    private double lastY;
    private double lastZ;
    private boolean lastOutlines = true;
    private int calculateMobsPart;

    public Radar() {
        entityMapImageManager = new EntityMapImageManager();
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        entityMapImageManager.reset();
    }

    @Override
    public void onTickInGame(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, LayoutVariables layoutVariables, float scaleProj) {
        entityMapImageManager.onRenderTick(matrixStack);
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
            if (this.options.isChanged()) {
                this.timer = 500;
                if (this.options.outlines != this.lastOutlines) {
                    this.lastOutlines = this.options.outlines;
//                    entityMapImageManager.reset();
                }
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            lastX = GameVariableAccessShim.xCoordDouble();
            lastY = GameVariableAccessShim.yCoordDouble();
            lastZ = GameVariableAccessShim.zCoordDouble();

            if (this.timer > 15) {
                // long t0 = System.nanoTime();
                this.calculateMobs();
                // long t1 = System.nanoTime();
                // VoxelConstants.getLogger().info("Calculate Mobs " + calculateMobsPart + " took " + ((t1 - t0) / 1000) + " micros");
                this.timer = 0;
            }

            ++this.timer;

            updateContacts();
            renderMapMobs(matrixStack, bufferSource, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj);
        }
    }

    private boolean isEntityShown(Entity entity) {
        if (entity.isInvisibleTo(VoxelConstants.getPlayer()) || entity.equals(VoxelConstants.getPlayer()) || !(entity instanceof LivingEntity)) {
            return false;
        }

        boolean playersAllowed = this.options.radarAllowed || this.options.radarPlayersAllowed;
        boolean mobsAllowed = this.options.radarAllowed || this.options.radarMobsAllowed;

        return switch (VoxelMapMobCategory.forEntity(entity)) {
            case PLAYER -> playersAllowed;
            case HOSTILE -> mobsAllowed && this.options.showHostiles;
            case NEUTRAL -> mobsAllowed && this.options.showNeutrals;
        };
    }

    private float getEntityMaxHeight(Entity entity) {
        if (entity.getType() == EntityType.PHANTOM) {
            return 64.0F;
        }

        return 32.0F;
    }

    private boolean isInRange(Entity entity, double dx, double dy, double dz, double cullDist) {
        double scale = layoutVariables.zoomScaleAdjusted;
        dx /= scale;
        dy /= scale;
        dz /= scale;

        if (Math.abs(dy) > getEntityMaxHeight(entity) + cullDist) {
            return false;
        }

        double maxDist = 28.5 + cullDist;
        if (!minimapOptions.squareMap) {
            return (dx * dx + dz * dz) <= (maxDist * maxDist);
        } else {
            return Math.abs(dx) <= maxDist && Math.abs(dz) <= maxDist;
        }
    }

    public void calculateMobs() {
        calculateMobsPart = (calculateMobsPart + 1) & 7;
        this.contacts.removeIf(e -> (e.uuid.getLeastSignificantBits() & 7) == calculateMobsPart);
        // this.contacts.clear();

        Iterable<Entity> entities = VoxelConstants.getClientWorld().entitiesForRendering();

        for (Entity entity : entities) {
            if ((entity.getUUID().getLeastSignificantBits() & 7) != calculateMobsPart) {
                continue;
            }
            try {
                if (this.isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();

                    if (this.isInRange(entity, wayX, wayY, wayZ, 5.0)) {
                        Contact contact = new Contact((LivingEntity) entity, VoxelMapMobCategory.forEntity(entity));
                        if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                            contact.yFudge = 1;
                        }
                        if (VoxelMap.radarOptions.isMobEnabled(contact.entity)) {
                            if (contact.icon == null) {
                                contact.icon = entityMapImageManager.requestImageForMob(contact.entity, 32, options.outlines);
                            }

                            String scrubbedName = TextUtils.scrubCodes(contact.entity.getName().getString());
                            if ((scrubbedName.equals("Dinnerbone") || scrubbedName.equals("Grumm")) && (!(contact.entity instanceof Player) || ((Player) contact.entity).isModelPartShown(PlayerModelPart.CAPE))) {
                                contact.setRotationFactor(contact.rotationFactor + 180);
                            }

                            if (this.options.showHelmetsPlayers && contact.category == VoxelMapMobCategory.PLAYER || this.options.showHelmetsMobs && contact.category != VoxelMapMobCategory.PLAYER) {
                                contact.armorIcon = entityMapImageManager.requestImageForArmor(contact.entity, 32, options.outlines);
                            }

                            this.contacts.add(contact);
                        }
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        this.contacts.sort((c1, c2) -> {
            double dy = c1.y - c2.y;
            if (dy != 0) {
                return dy > 0 ? 1 : -1;
            }
            double dx = c1.x - c2.x;
            if (dx != 0) {
                return dx > 0 ? 1 : -1;
            }
            double dz = c1.z - c2.z;
            if (dz != 0) {
                return dz > 0 ? 1 : -1;
            }
            return 0;
        });
    }

    private void updateContacts() {
        for (Contact contact : this.contacts) {
            if (contact.icon == null) {
                continue;
            }

            contact.updateLocation();

            double wayX = lastX - contact.x;
            double wayZ = lastZ - contact.z;
            double wayY = lastY - contact.y;

            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            if (minimapOptions.rotates) {
                contact.angle += direction;
            } else if (minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ);

            double maxHeight = getEntityMaxHeight(contact.entity) * layoutVariables.zoomScaleAdjusted;
            double adjustedDiff = maxHeight - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / maxHeight, 0.0);
            contact.brightness *= contact.brightness;
        }
    }

    public void renderMobIcons(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, int x, int y, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        RenderType iconRenderType = VoxelMapRenderTypes.GUI_TEXTURED_LEQUAL_DEPTH_TEST.apply(EntityMapImageManager.resourceTextureAtlasMarker);
        VertexConsumer iconBuffer = bufferSource.getBuffer(iconRenderType);

        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            float distance = (float) (contact.distance / layoutVariables.zoomScaleAdjusted);
            int color;
            if (lastY - contact.y < 0) {
                color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
            } else {
                float brightness = Math.max(0.3F, contact.brightness);
                color = ARGB.colorFromFloat(1.0F, brightness, brightness, brightness);
            }

            matrixStack.pushMatrix();
            matrixStack.translate(x, y, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
            matrixStack.translate(0.0F, -distance, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
            matrixStack.translate(-x, -y, 0.0F);

            float zOffset = i * 0.01F;
            float yOffset = 0.0F;
            if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                yOffset = -4.0F;
            }

            float imageWidth = contact.icon.getIconWidth() / 8.0F;
            float imageHeight = contact.icon.getIconHeight() / 8.0F;
            RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, contact.icon, x - (imageWidth / 2), y + yOffset - (imageHeight / 2), zOffset, imageWidth, imageHeight, color);

            if (contact.armorIcon != null) {
                float armorOffset = Float.parseFloat(entityMapImageManager.getMobProperties(contact.entity).getProperty("helmetOffset", "0.0"));
                float armorWidth = contact.armorIcon.getIconWidth() / 8.0F;
                float armorHeight = contact.armorIcon.getIconHeight() / 8.0F;
                RenderUtils.drawTexturedModalRect(matrixStack, iconBuffer, contact.armorIcon, x - (armorWidth / 2), y + yOffset + armorOffset - (armorHeight / 2), zOffset, armorWidth, armorHeight, color);
            }
            matrixStack.popMatrix();
        }

        bufferSource.endBatch(iconRenderType);

        matrixStack.popMatrix();
    }

    public void renderMobNames(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, int x, int y, float scaleProj) {
        matrixStack.pushMatrix();
        matrixStack.scale(scaleProj, scaleProj, 1.0F);

        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);

            float distance = (float) (contact.distance / layoutVariables.zoomScaleAdjusted);

            matrixStack.pushMatrix();
            matrixStack.translate(x, y, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(-contact.angle));
            matrixStack.translate(0.0F, -distance, 0.0F);
            matrixStack.rotate(Axis.ZP.rotationDegrees(contact.angle + contact.rotationFactor));
            matrixStack.translate(-x, -y, 0.0F);

            float zOffset = i * 0.01F;

            if (contact.name != null && ((this.options.showPlayerNames && contact.category == VoxelMapMobCategory.PLAYER) || (this.options.showMobNames && contact.category != VoxelMapMobCategory.PLAYER))) {
                float scaleFactor = this.options.fontScale / 4.0F;

                matrixStack.pushMatrix();
                matrixStack.scale(scaleFactor, scaleFactor, 1.0F);
                RenderUtils.drawCenteredString(matrixStack, bufferSource, contact.name, x / scaleFactor, (y + 3) / scaleFactor, zOffset, 0xFFFFFFFF, false);

                matrixStack.popMatrix();
            }

            matrixStack.popMatrix();
        }

        matrixStack.popMatrix();
    }

    public void renderMapMobs(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, int x, int y, float scaleProj) {
        renderMobIcons(matrixStack, bufferSource, x, y, scaleProj);
        renderMobNames(matrixStack, bufferSource, x, y, scaleProj);

    }

    public void onJoinServer() {
        entityMapImageManager.reset();
    }

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
    }
}
