package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mamiyaotaru.voxelmap.util.MobCategory;
import com.mamiyaotaru.voxelmap.util.VoxelMapPipelines;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public class RadarSimple implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    public static final ResourceLocation resourceTextureAtlasMarker = new ResourceLocation("voxelmap", "atlas/radarsimple/marker");
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);

    public RadarSimple() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
        this.textureAtlas.setFilter(true, false);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            NativeImage contact = NativeImage.read(Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("voxelmap", "images/radar/contact.png")).get().open());
            contact = ImageUtils.fillOutline(contact, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = NativeImage.read(Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation("voxelmap", "images/radar/contact_facing.png")).get().open());
            facing = ImageUtils.fillOutline(facing, false, true, 32.0F, 32.0F, 0);
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

    public void calculateMobs() {
        this.contacts.clear();

        for (Entity entity : VoxelConstants.getClientWorld().entitiesForRendering()) {
            try {
                if (entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && MobCategory.isHostile(entity)
                        || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && MobCategory.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && MobCategory.isNeutral(entity))) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();

                    double scale = this.layoutVariables.zoomScaleAdjusted;
                    boolean inRange;
                    if (!this.minimapOptions.squareMap) {
                        inRange = (wayX * wayX + wayZ * wayZ) / (scale * scale) < 32.0 * 32.0;
                    } else {
                        inRange = Mth.abs(wayX) / scale < 32.0 || Mth.abs(wayZ) / scale < 32.0;
                    }
                    inRange = inRange && Mth.abs(wayY) / scale < 32.0;

                    if (inRange) {
                        Contact contact = new Contact((LivingEntity) entity, MobCategory.forEntity(entity));
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
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            double contactY = contact.y;
            double wayX = GameVariableAccessShim.xCoordDouble() - contactX;
            double wayZ = GameVariableAccessShim.zCoordDouble() - contactZ;
            double wayY = GameVariableAccessShim.yCoord() - contactY;
            double adjustedDiff = max - Math.abs(wayY);
            contact.brightness = (float) Math.max(adjustedDiff / max, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;

            int color = wayY < 0 ? ARGB.colorFromFloat(contact.brightness, 1, 1, 1) : ARGB.colorFromFloat(1, contact.brightness, contact.brightness, contact.brightness);

            if (this.minimapOptions.rotates) {
                contact.angle += this.direction;
            } else if (this.minimapOptions.oldNorth) {
                contact.angle -= 90.0F;
            }

            boolean inRange;
            if (!this.minimapOptions.squareMap) {
                inRange = contact.distance < 28.5;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= 28.5 && Math.abs(dispY) <= 28.5;
            }

            if (inRange) {
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
                    guiGraphics.pose().translate(0.0f, (float) -contact.distance);
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
