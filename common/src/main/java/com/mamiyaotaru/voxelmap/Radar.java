package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.entityrender.EntityMapImageManager;
import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;

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
    public void onTickInGame(GuiGraphics drawContext, LayoutVariables layoutVariables, float scaleProj) {
        entityMapImageManager.onRenderTick(drawContext);
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

            if (this.timer > 15) {
                // long t0 = System.nanoTime();
                this.calculateMobs();
                // long t1 = System.nanoTime();
                // VoxelConstants.getLogger().info("Calculate Mobs " + calculateMobsPart + " took " + ((t1 - t0) / 1000) + " micros");
                this.timer = 0;
            }

            ++this.timer;
            this.renderMapMobs(drawContext, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj);
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

    public void renderMapMobs(GuiGraphics guiGraphics, int x, int y, float scaleProj) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(scaleProj, scaleProj);

        double zoomScale = this.layoutVariables.zoomScaleAdjusted;
        double lastX = GameVariableAccessShim.xCoordDouble();
        double lastZ = GameVariableAccessShim.zCoordDouble();
        double lastY = GameVariableAccessShim.yCoordDouble();

        for (Contact contact : this.contacts) {
            if (contact.icon == null) {
                continue;
            }

            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            double contactY = contact.y;
            double wayX = lastX - contactX;
            double wayZ = lastZ - contactZ;
            double wayY = lastY - contactY;

            double maxHeight = this.getEntityMaxHeight(contact.entity) * zoomScale;
            double adjustedDiff = maxHeight - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / maxHeight, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ);

            int color;
            if (wayY < 0) {
                color = ARGB.colorFromFloat(contact.brightness, 1.0F, 1.0F, 1.0F);
            } else {
                if (contact.brightness < 0.3f) {
                    contact.brightness = 0.3f;
                }
                color = ARGB.colorFromFloat(1.0f, contact.brightness, contact.brightness, contact.brightness);
            }

            if (this.minimapOptions.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            double scaledDistance = contact.distance / zoomScale;
            if (this.isInRange(contact.entity, wayX, wayY, wayZ, 0.0)) {
                try {
                    guiGraphics.pose().pushMatrix();
                    if (this.options.filtering) {
                        guiGraphics.pose().translate(x, y);
                        guiGraphics.pose().rotate(-contact.angle * Mth.DEG_TO_RAD);
                        guiGraphics.pose().translate(0.0f, (float) -scaledDistance);
                        guiGraphics.pose().rotate((contact.angle + contact.rotationFactor) * Mth.DEG_TO_RAD);
                        guiGraphics.pose().translate(-x, -y);
                    } else {
                        wayZ = Math.cos(Math.toRadians(contact.angle)) * scaledDistance;
                        wayX = Math.sin(Math.toRadians(contact.angle)) * scaledDistance;
                        guiGraphics.pose().translate((float) Math.round(-wayX * this.layoutVariables.scScale) / this.layoutVariables.scScale, (float) Math.round(-wayZ * this.layoutVariables.scScale) / this.layoutVariables.scScale);
                    }

                    float yOffset = 0.0F;
                    if (contact.entity.getVehicle() != null && this.isEntityShown(contact.entity.getVehicle())) {
                        yOffset = -4.0F;
                    }

                    float imageWidth = contact.icon.getIconWidth() / 8.0F;
                    float imageHeight = contact.icon.getIconHeight() / 8.0F;
                    contact.icon.blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - (imageWidth / 2), y + yOffset - (imageHeight / 2), imageWidth, imageHeight, color);

                    if (contact.armorIcon != null) {
                        float helmetWidth = contact.armorIcon.getIconWidth() / 8.0F;
                        float helmetHeight = contact.armorIcon.getIconHeight() / 8.0F;
                        float helmetOffset = Float.parseFloat(this.entityMapImageManager.getMobProperties(contact.entity).getProperty("helmetOffset", "0.0"));

                        contact.armorIcon.blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - (helmetWidth / 2), y + yOffset + helmetOffset - (helmetHeight / 2), helmetWidth, helmetWidth, color);
                    }

                    if (contact.name != null && ((this.options.showPlayerNames && contact.category == VoxelMapMobCategory.PLAYER) || (this.options.showMobNames && contact.category != VoxelMapMobCategory.PLAYER && contact.entity.hasCustomName()))) {
                        float scaleFactor = this.options.fontScale / 4.0F;
                        guiGraphics.pose().scale(scaleFactor, scaleFactor);

                        int m = minecraft.font.width(contact.name) / 2;

                        guiGraphics.pose().pushMatrix();
                        guiGraphics.drawString(minecraft.font, contact.name, (int) (x / scaleFactor - m), (int) ((y + 3) / scaleFactor), 0xFFFFFFFF, false);
                        guiGraphics.pose().popMatrix();
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()), e);
                } finally {
                    guiGraphics.pose().popMatrix();
                }
            }
        }
        guiGraphics.pose().popMatrix();
    }

    public void onJoinServer() {
        entityMapImageManager.reset();
    }

    public EntityMapImageManager getEntityMapImageManager() {
        return entityMapImageManager;
    }
}
