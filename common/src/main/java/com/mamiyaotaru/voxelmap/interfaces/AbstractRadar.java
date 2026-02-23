package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.RadarSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.MinimapContext;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4fStack;

import java.util.ArrayList;

public abstract class AbstractRadar {
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final MapSettingsManager mapOptions;
    protected final RadarSettingsManager radarOptions;

    protected MinimapContext minimapContext;

    protected final ArrayList<Contact> contacts = new ArrayList<>(40);
    private int timer = 500;
    private int calculateMobsPart;

    public AbstractRadar() {
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        radarOptions = VoxelConstants.getVoxelMapInstance().getRadarOptions();
    }

    public abstract void onResourceManagerReload(ResourceManager resourceManager);

    public abstract void renderMapMobs(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Contact.DisplayState displayState, int x, int y, int scScale, float scaleProj);

    protected abstract void initContact(Contact contact);

    public void onTickInGame(Matrix4fStack matrixStack, MinimapContext minimapContext) {
        this.minimapContext = minimapContext;

        if (radarOptions.radarAllowed || radarOptions.radarMobsAllowed || radarOptions.radarPlayersAllowed) {
            if (radarOptions.isChanged()) {
                timer = 500;
            }

            if (timer > 15) {
                calculateMobs();
                timer = 0;
            }

            ++timer;

            for (Contact contact : contacts) {
                updateContact(contact);
            }
        }
    }

    private void calculateMobs() {
        calculateMobsPart = (calculateMobsPart + 1) & 7;
        contacts.removeIf(e -> (e.uuid.getLeastSignificantBits() & 7) == calculateMobsPart);

        Iterable<Entity> entities = VoxelConstants.getClientWorld().entitiesForRendering();

        for (Entity entity : entities) {
            if ((entity.getUUID().getLeastSignificantBits() & 7) != calculateMobsPart) {
                continue;
            }
            try {
                if (radarOptions.isMobEnabled(entity.getType()) && isEntityShown(entity)) {
                    double wayX = minimapContext.playerX - entity.getX();
                    double wayZ = minimapContext.playerZ - entity.getZ();
                    double wayY = minimapContext.playerY - entity.getY();

                    if (isInRange(entity, wayX, wayY, wayZ, 5.0)) {
                        Contact contact = new Contact((LivingEntity) entity, VoxelMapMobCategory.forEntity(entity));
                        initContact(contact);
                        contacts.add(contact);
                    }
                }
            } catch (Exception var16) {
                VoxelConstants.getLogger().error(var16.getLocalizedMessage(), var16);
            }
        }

        contacts.sort((c1, c2) -> {
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

    protected void updateContact(Contact contact) {
        contact.updateLocation();

        double wayX = minimapContext.playerX - contact.x;
        double wayZ = minimapContext.playerZ - contact.z;
        double wayY = minimapContext.playerY - contact.y;

        if (!isInRange(contact.entity, wayX, wayY, wayZ, 0.0F)) {
            contact.displayState = Contact.DisplayState.HIDDEN;
            return;
        } else {
            contact.displayState = Contact.DisplayState.BELOW_FRAME;
        }

        contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
        contact.angle += minimapContext.direction;

        contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ);

        double maxHeight = getEntityMaxHeight(contact.entity) * minimapContext.zoomScaleAdjusted;
        double adjustedDiff = maxHeight - Math.max(Math.abs(wayY), 0);
        contact.brightness = (float) Math.max(adjustedDiff / maxHeight, 0.0);
        contact.brightness *= contact.brightness;
    }

    protected boolean isEntityShown(Entity entity) {
        if (entity.isInvisibleTo(VoxelConstants.getPlayer()) || entity.equals(VoxelConstants.getPlayer()) || !(entity instanceof LivingEntity)) {
            return false;
        }

        boolean playersAllowed = radarOptions.radarAllowed || radarOptions.radarPlayersAllowed;
        boolean mobsAllowed = radarOptions.radarAllowed || radarOptions.radarMobsAllowed;

        return switch (VoxelMapMobCategory.forEntity(entity)) {
            case PLAYER -> playersAllowed;
            case HOSTILE -> mobsAllowed && radarOptions.showHostiles;
            case NEUTRAL -> mobsAllowed && radarOptions.showNeutrals;
        };
    }

    protected float getEntityMaxHeight(Entity entity) {
        if (entity.getType() == EntityType.PHANTOM) {
            return 64.0F;
        }

        return 32.0F;
    }

    protected boolean isInRange(Entity entity, double dx, double dy, double dz, double cullDist) {
        double scale = minimapContext.zoomScaleAdjusted;
        dx /= scale;
        dy /= scale;
        dz /= scale;

        if (Math.abs(dy) > getEntityMaxHeight(entity) + cullDist) {
            return false;
        }

        double maxDist = 32.0 + cullDist;
        if (!mapOptions.squareMap) {
            return (dx * dx + dz * dz) <= (maxDist * maxDist);
        } else {
            return Math.abs(dx) <= maxDist && Math.abs(dz) <= maxDist;
        }
    }

    public void onJoinServer() {
        contacts.clear();
    }
}
