package com.mamiyaotaru.voxelmap;

import com.mamiyaotaru.voxelmap.interfaces.IRadar;
import com.mamiyaotaru.voxelmap.interfaces.IVoxelMap;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.EnumMobs;
import com.mamiyaotaru.voxelmap.util.GLShim;
import com.mamiyaotaru.voxelmap.util.GLUtils;
import com.mamiyaotaru.voxelmap.util.GameVariableAccessShim;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mamiyaotaru.voxelmap.util.LayoutVariables;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ZombifiedPiglinEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3f;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.UUID;

public class RadarSimple implements IRadar {
    private LayoutVariables layoutVariables = null;
    public final MapSettingsManager minimapOptions;
    public final RadarSettingsManager options;
    private final TextureAtlas textureAtlas;
    private boolean completedLoading = false;
    private int timer = 500;
    private float direction = 0.0F;
    private final ArrayList<Contact> contacts = new ArrayList<>(40);
    final UUID devUUID = UUID.fromString("9b37abb9-2487-4712-bb96-21a1e0b2023c");

    public RadarSimple(IVoxelMap master) {
        this.minimapOptions = master.getMapOptions();
        this.options = master.getRadarOptions();
        this.textureAtlas = new TextureAtlas("pings");
        this.textureAtlas.setFilter(false, false);
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.loadTexturePackIcons();
    }

    private void loadTexturePackIcons() {
        this.completedLoading = false;

        try {
            this.textureAtlas.reset();
            BufferedImage contact = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/contact.png"), 0, 0, 32, 32, 32, 32);
            contact = ImageUtils.fillOutline(contact, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("contact", contact);
            BufferedImage facing = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/contact_facing.png"), 0, 0, 32, 32, 32, 32);
            facing = ImageUtils.fillOutline(facing, false, true, 32.0F, 32.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("facing", facing);
            BufferedImage glow = ImageUtils.loadImage(new Identifier("voxelmap", "images/radar/glow.png"), 0, 0, 16, 16, 16, 16);
            glow = ImageUtils.fillOutline(glow, false, true, 16.0F, 16.0F, 0);
            this.textureAtlas.registerIconForBufferedImage("glow", glow);
            this.textureAtlas.stitch();
            this.completedLoading = true;
        } catch (Exception var4) {
            VoxelConstants.getLogger().error("Failed getting mobs " + var4.getLocalizedMessage(), var4);
        }

    }

    @Override
    public void onTickInGame(MatrixStack matrixStack, LayoutVariables layoutVariables) {
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
                this.renderMapMobs(matrixStack, this.layoutVariables.mapX, this.layoutVariables.mapY);
            }

            GLShim.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

        }
    }

    public void calculateMobs() {
        this.contacts.clear();

        for (Entity entity : VoxelConstants.getMinecraft().world.getEntities()) {
            try {
                if (entity != null && !entity.isInvisibleTo(VoxelConstants.getMinecraft().player) && (this.options.showHostiles && (this.options.radarAllowed || this.options.radarMobsAllowed) && this.isHostile(entity) || this.options.showPlayers && (this.options.radarAllowed || this.options.radarPlayersAllowed) && this.isPlayer(entity) || this.options.showNeutrals && this.options.radarMobsAllowed && this.isNeutral(entity))) {
                    int wayX = GameVariableAccessShim.xCoord() - (int) entity.getPos().getX();
                    int wayZ = GameVariableAccessShim.zCoord() - (int) entity.getPos().getZ();
                    int wayY = GameVariableAccessShim.yCoord() - (int) entity.getPos().getY();
                    double hypot = wayX * wayX + wayZ * wayZ + wayY * wayY;
                    hypot /= this.layoutVariables.zoomScaleAdjusted * this.layoutVariables.zoomScaleAdjusted;
                    if (hypot < 961.0) {
                        Contact contact = new Contact(entity, this.getUnknownMobNeutrality(entity));
                        String unscrubbedName = contact.entity.getDisplayName().getString();
                        contact.setName(unscrubbedName);
                        contact.updateLocation();
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
            return !(entity instanceof TameableEntity) || !((TameableEntity) entity).isTamed() || !VoxelConstants.getMinecraft().isIntegratedServerRunning() && !((TameableEntity) entity).getOwner().equals(VoxelConstants.getMinecraft().player) ? EnumMobs.GENERICNEUTRAL : EnumMobs.GENERICTAME;
        }
    }

    private boolean isHostile(Entity entity) {
        if (entity instanceof ZombifiedPiglinEntity zombifiedPiglinEntity) {
            return zombifiedPiglinEntity.isAngryAt(VoxelConstants.getMinecraft().player);
        } else if (entity instanceof Monster) {
            return true;
        } else if (entity instanceof BeeEntity beeEntity) {
            return beeEntity.hasAngerTime();
        } else {
            if (entity instanceof PolarBearEntity polarBearEntity) {

                for (PolarBearEntity object : polarBearEntity.world.getNonSpectatingEntities(PolarBearEntity.class, polarBearEntity.getBoundingBox().expand(8.0, 4.0, 8.0))) {
                    if (object.isBaby()) {
                        return true;
                    }
                }
            }

            if (entity instanceof RabbitEntity rabbitEntity) {
                return rabbitEntity.getRabbitType() == 99;
            } else if (entity instanceof WolfEntity wolfEntity) {
                return wolfEntity.hasAngerTime();
            } else {
                return false;
            }
        }
    }

    private boolean isPlayer(Entity entity) {
        return entity instanceof OtherClientPlayerEntity;
    }

    private boolean isNeutral(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return false;
        } else {
            return !(entity instanceof PlayerEntity) && !this.isHostile(entity);
        }
    }

    public void renderMapMobs(MatrixStack matrixStack, int x, int y) {
        double max = this.layoutVariables.zoomScaleAdjusted * 32.0;
        GLUtils.disp2(this.textureAtlas.getGlId());

        for (Contact contact : this.contacts) {
            contact.updateLocation();
            double contactX = contact.x;
            double contactZ = contact.z;
            int contactY = contact.y;
            double wayX = GameVariableAccessShim.xCoordDouble() - contactX;
            double wayZ = GameVariableAccessShim.zCoordDouble() - contactZ;
            int wayY = GameVariableAccessShim.yCoord() - contactY;
            double adjustedDiff = max - (double) Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / max, 0.0);
            contact.brightness *= contact.brightness;
            contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
            contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ) / this.layoutVariables.zoomScaleAdjusted;
            GLShim.glBlendFunc(770, 771);
            if (wayY < 0) {
                GLShim.glColor4f(1.0F, 1.0F, 1.0F, contact.brightness);
            } else {
                GLShim.glColor3f(contact.brightness, contact.brightness, contact.brightness);
            }

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
                    matrixStack.push();
                    float contactFacing = contact.entity.getHeadYaw();
                    if (this.minimapOptions.rotates) {
                        contactFacing -= this.direction;
                    } else if (this.minimapOptions.oldNorth) {
                        contactFacing += 90.0F;
                    }

                    matrixStack.translate(x, y, 0.0);
                    matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(-contact.angle));
                    matrixStack.translate(0.0, -contact.distance, 0.0);
                    matrixStack.multiply(Vec3f.POSITIVE_Z.getDegreesQuaternion(contact.angle + contactFacing));
                    matrixStack.translate(-x, -y, 0.0);
                    RenderSystem.applyModelViewMatrix();
                    if (contact.uuid != null && contact.uuid.equals(this.devUUID)) {
                        Sprite icon = this.textureAtlas.getAtlasSprite("glow");
                        this.applyFilteringParameters();
                        GLUtils.drawPre();
                        GLUtils.setMap(icon, (float) x, (float) y, (float) ((int) ((float) icon.getIconWidth() / 2.0F)));
                        GLUtils.drawPost();
                    }

                    this.applyFilteringParameters();
                    GLUtils.drawPre();
                    GLUtils.setMap(this.textureAtlas.getAtlasSprite("contact"), (float) x, (float) y, 16.0F);
                    GLUtils.drawPost();
                    if (this.options.showFacing) {
                        this.applyFilteringParameters();
                        GLUtils.drawPre();
                        GLUtils.setMap(this.textureAtlas.getAtlasSprite("facing"), (float) x, (float) y, 16.0F);
                        GLUtils.drawPost();
                    }
                } catch (Exception e) {
                    VoxelConstants.getLogger().error("Error rendering mob icon! " + e.getLocalizedMessage() + " contact type " + contact.type, e);
                } finally {
                    matrixStack.pop();
                    RenderSystem.applyModelViewMatrix();
                }
            }
        }

    }

    private void applyFilteringParameters() {
        if (this.options.filtering) {
            GLShim.glTexParameteri(3553, 10241, 9729);
            GLShim.glTexParameteri(3553, 10240, 9729);
            GLShim.glTexParameteri(3553, 10242, 10496);
            GLShim.glTexParameteri(3553, 10243, 10496);
        } else {
            GLShim.glTexParameteri(3553, 10241, 9728);
            GLShim.glTexParameteri(3553, 10240, 9728);
        }

    }
}
