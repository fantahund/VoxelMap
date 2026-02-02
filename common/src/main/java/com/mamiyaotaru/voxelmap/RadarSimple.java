package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Comparator;

public class RadarSimple implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    public static final Identifier resourceTextureAtlasMarker = Identifier.fromNamespaceAndPath("voxelmap", "atlas/radarsimple/marker");
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);

    public RadarSimple() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);

        this.loadTexturePackIcons();
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            NativeImage contact = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath("voxelmap", "images/radar/contact.png")).image();
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = TextureContents.load(Minecraft.getInstance().getResourceManager(), Identifier.fromNamespaceAndPath("voxelmap", "images/radar/contact_facing.png")).image();
            this.textureAtlas.registerIconForBufferedImage("facing", facing);
            this.textureAtlas.stitch();
            this.completedLoading = true;
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Failed getting mobs " + var4.getLocalizedMessage(), var4);
        }

    }

    @Override
    public void onTickInGame(GuiGraphics guiGraphics, LayoutVariables layoutVariables, float scaleProj) {
        if (this.options.radarAllowed || this.options.radarMobsAllowed || this.options.radarPlayersAllowed) {
            this.layoutVariables = layoutVariables;
            if (this.options.isChanged()) {
                this.timer = 500;
            }

            this.direction = GameVariableAccessShim.rotationYaw() + 180.0F;

            while (this.direction >= 360.0F) {
                this.direction -= 360.0F;
            }

            while (this.direction < 0.0F) {
                this.direction += 360.0F;
            }

            if (this.completedLoading && this.timer > 95) {
                this.calculateMobs();
                this.timer = 0;
            }

            ++this.timer;
            if (this.completedLoading) {
                this.renderMapMobs(guiGraphics, this.layoutVariables.mapX, this.layoutVariables.mapY, scaleProj);
            }
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
        this.contacts.clear();

        for (Entity entity : VoxelConstants.getClientWorld().entitiesForRendering()) {
            try {
                if (isEntityShown(entity)) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();

                    if (this.isInRange(entity, wayX, wayY, wayZ, 5.0)) {
                        Contact contact = new Contact((LivingEntity) entity, VoxelMapMobCategory.forEntity(entity));
                        this.contacts.add(contact);
                    }
                }
            } catch (Exception var11) {
                VoxelConstants.getLogger().error(var11.getLocalizedMessage(), var11);
            }
        }

        this.contacts.sort(Comparator.comparingDouble(contact -> contact.y));
    }

    public void renderMapMobs(GuiGraphics guiGraphics, int x, int y, float scaleProj) {
        double zoomScale = this.layoutVariables.zoomScaleAdjusted;

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            double contactY = contact.y;
            double wayX = GameVariableAccessShim.xCoordDouble() - contactX;
            double wayZ = GameVariableAccessShim.zCoordDouble() - contactZ;
            double wayY = GameVariableAccessShim.yCoord() - contactY;

            double maxHeight = this.getEntityMaxHeight(contact.entity) * zoomScale;
            double adjustedDiff = maxHeight - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / maxHeight, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ);

            int color = wayY < 0 ? ARGB.colorFromFloat(contact.brightness, 1, 1, 1) : ARGB.colorFromFloat(1, contact.brightness, contact.brightness, contact.brightness);
            switch (contact.category) {
                case HOSTILE -> color = ARGB.multiply(color, 0xFFFF8080);
                case NEUTRAL -> color = ARGB.multiply(color, 0xFF80FF80);
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
                    guiGraphics.pose().scale(scaleProj, scaleProj);
                    float contactFacing = contact.entity.getYHeadRot();
                    if (this.minimapOptions.rotates) {
                        contactFacing -= this.direction;
                    } else if (this.minimapOptions.oldNorth) {
                        contactFacing += 90.0F;
                    }

                    guiGraphics.pose().translate(x, y);
                    guiGraphics.pose().rotate(-contact.angle * Mth.DEG_TO_RAD);
                    guiGraphics.pose().translate(0.0f, (float) -scaledDistance);
                    guiGraphics.pose().rotate((contact.angle + contactFacing) * Mth.DEG_TO_RAD);
                    guiGraphics.pose().translate(-x, -y);

                    this.textureAtlas.getAtlasSprite("contact").blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - 4, y - 4, 8, 8, color);
                    if (this.options.showFacing) {
                        this.textureAtlas.getAtlasSprite("facing").blit(guiGraphics, VoxelMapPipelines.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH_PIPELINE, x - 4, y - 4, 8, 8, color);
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + BuiltInRegistries.ENTITY_TYPE.getKey(contact.entity.getType()));
                } finally {
                    guiGraphics.pose().popMatrix();
                }
            }
        }

    }
}
