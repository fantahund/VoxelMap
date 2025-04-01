package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;

public class RadarSimple implements IRadar {
    private LayoutVariables layoutVariables;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    public static final ResourceLocation resourceTextureAtlasMarker = ResourceLocation.fromNamespaceAndPath("voxelmap", "atlas/radarsimple/marker");
    private boolean completedLoading;
    private int timer = 500;
    private float direction;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);

    public RadarSimple() {
        this.minimapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings", resourceTextureAtlasMarker);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            NativeImage contact = TextureContents.load(Minecraft.getInstance().getResourceManager(), ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact.png")).image();
            contact = ImageUtils.fillOutline(contact, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            NativeImage facing = TextureContents.load(Minecraft.getInstance().getResourceManager(), ResourceLocation.fromNamespaceAndPath("voxelmap", "images/radar/contact_facing.png")).image();
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
                if (entity != null && !entity.isInvisibleTo(VoxelConstants.getPlayer()) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity)
                        || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity))) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.position().x();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.position().z();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.position().y();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        Contact contact = new Contact((LivingEntity) entity, this.getUnknownMobNeutrality(entity));
                        this.contacts.add(contact);
                    }
                }
            } catch (Exception var11) {
                VoxelConstants.getLogger().error(var11.getLocalizedMessage(), var11);
            }
        }

        this.contacts.sort(Comparator.comparingInt(contact -> contact.y));
    }

    private EnumMobs getUnknownMobNeutrality(Entity entity) {
        if (this.isHostile(entity)) {
            return EnumMobs.GENERICHOSTILE;
        } else {
            return !(entity instanceof TamableAnimal) || !((TamableAnimal) entity).isTame() || !VoxelConstants.getMinecraft().hasSingleplayerServer() && !((TamableAnimal) entity).getOwner().equals(VoxelConstants.getPlayer()) ? EnumMobs.GENERICNEUTRAL : EnumMobs.GENERICTAME;
        }
    }

    private boolean isHostile(Entity entity) {
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

    private boolean isPlayer(Entity entity) {
        return entity instanceof RemotePlayer;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof Player) && !this.isHostile(entity);
        }
    }

    public void renderMapMobs(GuiGraphics guiGraphics, int x, int y, float scaleProj) {
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            int contactY = contact.y;
            double wayX = GameVariableAccessShim.xCoordDouble() - contactX;
            double wayZ = GameVariableAccessShim.zCoordDouble() - contactZ;
            int wayY = GameVariableAccessShim.yCoord() - contactY;
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
                inRange = contact.distance < 31.0;
            } else {
                double radLocate = Math.toRadians(contact.angle);
                double dispX = contact.distance * Math.cos(radLocate);
                double dispY = contact.distance * Math.sin(radLocate);
                inRange = Math.abs(dispX) <= 28.5 && Math.abs(dispY) <= 28.5;
            }

            if (inRange) {
                try {
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().scale(scaleProj, scaleProj, 1.0F);
                    float contactFacing = contact.entity.getYHeadRot();
                    if (this.minimapOptions.rotates) {
                        contactFacing -= this.direction;
                    } else if (this.minimapOptions.oldNorth) {
                        contactFacing += 90.0F;
                    }

                    guiGraphics.pose().translate(x, y, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-contact.angle));
                    guiGraphics.pose().translate(0.0f, (float) -contact.distance, 0.0f);
                    guiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(contact.angle + contactFacing));
                    guiGraphics.pose().translate(-x, -y, 0.0f);
                    guiGraphics.pose().translate(0, 0, 125);

                    this.textureAtlas.getAtlasSprite("contact").blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, x - 4, y - 4, 8, 8, color);
                    if (this.options.showFacing) {
                        this.textureAtlas.getAtlasSprite("facing").blit(guiGraphics, GLUtils.GUI_TEXTURED_LESS_OR_EQUAL_DEPTH, x - 4, y - 4, 8, 8, color);
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type, e);
                } finally {
                    guiGraphics.pose().popPose();
                }
            }
        }

    }
}
