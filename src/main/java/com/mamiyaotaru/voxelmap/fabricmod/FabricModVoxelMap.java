package com.mamiyaotaru.voxelmap.fabricmod;

import com.google.gson.Gson;
import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.text.Text;

public class FabricModVoxelMap implements ClientModInitializer {
    public static FabricModVoxelMap instance;
    private boolean initialized = false;
    private VoxelMap master = null;

    public void onInitializeClient() {
        instance = this;
        this.master = new VoxelMap();
    }

    public void lateInit() {
        this.initialized = true;
        this.master.lateInit(true, false);
        Runtime.getRuntime().addShutdownHook(new Thread(FabricModVoxelMap.this::onShutDown));
    }

    public void clientTick(MinecraftClient client) {
        if (!this.initialized) {
            boolean OK = MinecraftClient.getInstance() != null && client.getResourceManager() != null && client.getTextureManager() != null;

            if (OK) {
                this.lateInit();
            }
        }

        if (this.initialized) {
            this.master.onTick(client);
        }

    }

    public void renderOverlay(MatrixStack matrixStack) {
        if (!this.initialized) {
            this.lateInit();
        }

        try {
            this.master.onTickInGame(matrixStack, MinecraftClient.getInstance());
        } catch (Exception ignore) {}
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

    public static void onRenderHand(float partialTicks, long timeSlice, MatrixStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        try {
            instance.master.getWaypointManager().renderWaypoints(partialTicks, matrixStack, beacons, signs, withDepth, withoutDepth);
        } catch (Exception ignored) {
        }

    }

    public void onShutDown() {
        System.out.print("Saving all world maps");
        instance.master.getPersistentMap().purgeCachedRegions();
        instance.master.getMapOptions().saveAll();
        BiomeRepository.saveBiomeColors();
        long shutdownTime = System.currentTimeMillis();

        while (ThreadManager.executorService.getQueue().size() + ThreadManager.executorService.getActiveCount() > 0 && System.currentTimeMillis() - shutdownTime < 10000L) {
            System.out.print(".");

            try {
                Thread.sleep(200L);
            } catch (InterruptedException ignored) {
            }
        }

        System.out.println();
    }

    public boolean handleCustomPayload(CustomPayloadS2CPacket packet) {
        if (packet != null && packet.getChannel() != null) {
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
                    VoxelMap.getLogger().warn("Received unknown world_id packet");
                    return true;
                } else {
                    // probably "legacy" packet
                    VoxelMap.getLogger().warn("Assuming legacy world_id packet. " +
                            "The support might be removed in the future versions.");
                    length = b;
                }
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                String subWorldName = new String(bytes, StandardCharsets.UTF_8);
                this.master.newSubWorldName(subWorldName, true);
                return true;
            } else if (channelName.equals("voxelmap:settings")) {
                buffer.readByte(); // ignore
                Map<String, Object> settings = new Gson().fromJson(buffer.readString(), Map.class);
                for (Entry<String, Object> entry : settings.entrySet()) {
                    String setting = entry.getKey();
                    Object value = entry.getValue();
                    switch (setting) {
                        case "worldName":
                            if (value != null) {
                                this.master.newSubWorldName((String) value, true);
                            }
                            break;
                        case "radarAllowed":
                            this.master.getRadarOptions().radarAllowed = ((Boolean) value).booleanValue();
                            break;
                        case "radarMobsAllowed":
                            this.master.getRadarOptions().radarMobsAllowed = ((Boolean) value).booleanValue();
                            break;
                        case "radarPlayersAllowed":
                            this.master.getRadarOptions().radarPlayersAllowed = ((Boolean) value).booleanValue();
                            break;
                        case "cavesAllowed":
                            this.master.getMapOptions().cavesAllowed = ((Boolean) value).booleanValue();
                            break;
                        default:
                            VoxelMap.getLogger().warn("Unknown configuration option " + setting);
                            break;
                    }
                }
                return true;
            }
        }

        return false;
    }
}
