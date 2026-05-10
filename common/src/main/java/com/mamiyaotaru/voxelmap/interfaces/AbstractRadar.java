package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.MapPermissionsManager;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.RadarOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumRadar;
import com.mamiyaotaru.voxelmap.util.Contact;
import com.mamiyaotaru.voxelmap.util.MinimapContext;
import com.mamiyaotaru.voxelmap.util.VoxelMapMobCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix4fStack;

import java.util.ArrayList;

public abstract class AbstractRadar implements IReloadListener {
    protected final Minecraft minecraft = Minecraft.getInstance();
    protected final MapPermissionsManager permissions;
    protected final MapOptions mapOptions;
    protected final RadarOptions radarOptions;

    protected MinimapContext minimapContext;

    protected final ArrayList<Contact> contacts = new ArrayList<>(40);
    private int timer = 500;
    private int calculateMobsPart;

    public AbstractRadar() {
        permissions = VoxelConstants.getVoxelMapInstance().getPermissionsManager();
        mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        radarOptions = VoxelConstants.getVoxelMapInstance().getRadarOptions();

        VoxelConstants.getVoxelMapInstance().addReloadListener(this);
    }

    public abstract void renderMapMobs(Matrix4fStack matrixStack, MultiBufferSource.BufferSource bufferSource, Contact.DisplayState displayState, int x, int y, int scScale, float scaleProj);

    protected abstract void initContact(Contact contact);

    public void onTickInGame(Matrix4fStack matrixStack, MinimapContext minimapContext) {
        this.minimapContext = minimapContext;

        if (permissions.anyAllowed(MapPermissionsManager.RADAR_ALLOWED, MapPermissionsManager.RADAR_MOBS_ALLOWED, MapPermissionsManager.RADAR_PLAYERS_ALLOWED)) {
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

        if (!isInRange(contact.entity, wayX, wayY, wayZ, 0.0F)
            || (radarOptions.hideSneaking.get() && contact.entity instanceof Player player && player.isCrouching())
            || (radarOptions.hideInvisible.get() && contact.entity.isInvisibleTo(VoxelConstants.getPlayer()))
        ) {
            contact.displayState = Contact.DisplayState.HIDDEN;
            return;
        }

        contact.displayState = Contact.DisplayState.BELOW_FRAME;

        contact.angle = (float) Math.toDegrees(Math.atan2(wayX, wayZ));
        contact.angle += minimapContext.direction;

        contact.distance = Math.sqrt(wayX * wayX + wayZ * wayZ);

        if (!radarOptions.showElevation.get()) {
            contact.brightness = 1.0F;
        } else {
            double maxHeight = getEntityMaxHeight(contact.entity) * minimapContext.zoomScaleAdjusted;
            double adjustedDiff = maxHeight - Math.max(Math.abs(wayY), 0);
            contact.brightness = (float) Math.max(adjustedDiff / maxHeight, 0.0);
            contact.brightness *= contact.brightness;
        }
    }

    protected boolean isHostilesShown() {
        return radarOptions.showMobs.get() == OptionEnumRadar.ShowMobs.HOSTILES || radarOptions.showMobs.get() == OptionEnumRadar.ShowMobs.BOTH;
    }

    protected boolean isNeutralsShown() {
        return radarOptions.showMobs.get() == OptionEnumRadar.ShowMobs.NEUTRALS || radarOptions.showMobs.get() == OptionEnumRadar.ShowMobs.BOTH;
    }

    protected boolean isEntityShown(Entity entity) {
        if (!(entity instanceof LivingEntity) || entity.equals(VoxelConstants.getPlayer())) {
            return false;
        }

        boolean playersAllowed = permissions.anyAllowed(MapPermissionsManager.RADAR_ALLOWED, MapPermissionsManager.RADAR_PLAYERS_ALLOWED);
        boolean mobsAllowed = permissions.anyAllowed(MapPermissionsManager.RADAR_ALLOWED, MapPermissionsManager.RADAR_MOBS_ALLOWED);

        return switch (VoxelMapMobCategory.forEntity(entity)) {
            case PLAYER -> playersAllowed;
            case HOSTILE -> mobsAllowed && isHostilesShown();
            case NEUTRAL -> mobsAllowed && isNeutralsShown();
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

        double maxHeight = getEntityMaxHeight(entity) + cullDist;
        if (radarOptions.showElevation.get() && Math.abs(dy) > maxHeight) {
            return false;
        }

        double maxDist = 32.0 + cullDist;
        if (!mapOptions.squareMap.get()) {
            return (dx * dx + dz * dz) <= (maxDist * maxDist);
        } else {
            return Math.abs(dx) <= maxDist && Math.abs(dz) <= maxDist;
        }
    }

    public void onJoinServer() {
        contacts.clear();
    }
}
