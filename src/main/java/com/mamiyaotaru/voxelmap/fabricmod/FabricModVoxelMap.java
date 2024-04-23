package com.mamiyaotaru.voxelmap.fabricmod;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.VoxelmapSettingsChannelHandler;
import com.mamiyaotaru.voxelmap.VoxelmapWorldIdChannelHandler;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.Text;
import org.joml.Matrix4fStack;

public class FabricModVoxelMap implements ClientModInitializer {
    public static FabricModVoxelMap instance;
    private boolean initialized;

    public void onInitializeClient() {
        instance = this;
        new VoxelmapSettingsChannelHandler();
        new VoxelmapWorldIdChannelHandler();
    }

    public void lateInit() {
        this.initialized = true;
        VoxelConstants.getVoxelMapInstance().lateInit(true, false);
        Runtime.getRuntime().addShutdownHook(new Thread(FabricModVoxelMap.this::onShutDown));
    }

    public void clientTick() {
        if (!this.initialized) {
            boolean OK = VoxelConstants.getMinecraft().getResourceManager() != null && VoxelConstants.getMinecraft().getTextureManager() != null;

            if (OK) {
                this.lateInit();
            }
        }

        if (this.initialized) {
            VoxelConstants.getVoxelMapInstance().onTick();
        }

    }

    public void renderOverlay(DrawContext drawContext) {
        if (!this.initialized) {
            this.lateInit();
        }

        try {
            VoxelConstants.getVoxelMapInstance().onTickInGame(drawContext);
        } catch (RuntimeException exception) {
            VoxelConstants.getLogger().error(exception);
        }
    }

    public boolean onChat(Text chat, MessageIndicator indicator) {
        return CommandUtils.checkForWaypoints(chat, indicator);
    }

    public boolean onSendChatMessage(String message) {
        if (message.startsWith("newWaypoint")) {
            CommandUtils.waypointClicked(message);
            return false;
        } else if (message.startsWith("ztp")) {
            CommandUtils.teleport(message);
            return false;
        } else {
            return true;
        }
    }

    public static void onRenderHand(float partialTicks, long timeSlice, Matrix4fStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        try {
            VoxelConstants.getVoxelMapInstance().getWaypointManager().renderWaypoints(partialTicks, matrixStack, beacons, signs, withDepth, withoutDepth);
        } catch (RuntimeException exception) {
            VoxelConstants.getLogger().error(exception);
        }

    }

    public void onShutDown() {
        VoxelConstants.getLogger().info("Saving all world maps");
        VoxelConstants.getVoxelMapInstance().getPersistentMap().purgeCachedRegions();
        VoxelConstants.getVoxelMapInstance().getMapOptions().saveAll();
        BiomeRepository.saveBiomeColors();
        long shutdownTime = System.currentTimeMillis();

        while (ThreadManager.executorService.getQueue().size() + ThreadManager.executorService.getActiveCount() > 0 && System.currentTimeMillis() - shutdownTime < 10000L) {
            Thread.onSpinWait();
        }
    }

    //legacy world_id packet removed in 1.20.2
    /*public boolean handleCustomPayload(class_8710 packet) {
        if (packet != null && packet.id() != null) {
            PacketByteBuf buffer = packet.getData();
            String channelName = packet.getChannel().toString();
            if (channelName.equals("worldinfo:world_id")) {
                buffer.readByte(); // ignore
                int length;
                int b = buffer.readByte();
                if (b == 42) {
                    // "new" packet
                    length = buffer.readByte();
                } else if (b == 0) {
                    // length == 0 ?
                    VoxelConstants.getLogger().warn("Received unknown world_id packet");
                    return true;
                } else {
                    // probably "legacy" packet
                    VoxelConstants.getLogger().warn("Assuming legacy world_id packet. " +
                            "The support might be removed in the future versions.");
                    length = b;
                }
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                String subWorldName = new String(bytes, StandardCharsets.UTF_8);
                VoxelConstants.getVoxelMapInstance().newSubWorldName(subWorldName, true);
                return true;
            } else if (channelName.equals("voxelmap:settings")) {
                buffer.readByte(); // ignore
                @SuppressWarnings("unchecked")
                Map<String, Object> settings = new Gson().fromJson(buffer.readString(), Map.class);
                for (Map.Entry<String, Object> entry : settings.entrySet()) {
                    String setting = entry.getKey();
                    Object value = entry.getValue();
                    switch (setting) {
                        case "worldName" -> {
                            if (value != null) {
                                VoxelConstants.getVoxelMapInstance().newSubWorldName((String) value, true);
                            }
                        }
                        case "radarAllowed" ->
                                VoxelConstants.getVoxelMapInstance().getRadarOptions().radarAllowed = (Boolean) value;
                        case "radarMobsAllowed" ->
                                VoxelConstants.getVoxelMapInstance().getRadarOptions().radarMobsAllowed = (Boolean) value;
                        case "radarPlayersAllowed" ->
                                VoxelConstants.getVoxelMapInstance().getRadarOptions().radarPlayersAllowed = (Boolean) value;
                        case "cavesAllowed" ->
                                VoxelConstants.getVoxelMapInstance().getMapOptions().cavesAllowed = (Boolean) value;
                        case "teleportCommand" ->
                                VoxelConstants.getVoxelMapInstance().getMapOptions().serverTeleportCommand = (String) value;
                        default -> VoxelConstants.getLogger().warn("Unknown configuration option " + setting);
                    }
                }
                return true;
            }
        }

        return false;
    }*/

    public void playerRunTeleportCommand(double x, double y, double z) {
        MapSettingsManager mapSettingsManager = VoxelConstants.getVoxelMapInstance().getMapOptions();
        String cmd = mapSettingsManager.serverTeleportCommand == null ? mapSettingsManager.teleportCommand : mapSettingsManager.serverTeleportCommand;
        cmd = cmd.replace("%p", VoxelConstants.getPlayer().getName().getString()).replace("%x", String.valueOf(x)).replace("%y", String.valueOf(y)).replace("%z", String.valueOf(z));
        VoxelConstants.getPlayer().networkHandler.sendCommand(cmd);
    }
}
