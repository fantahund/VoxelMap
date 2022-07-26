package com.mamiyaotaru.voxelmap.fabricmod;

import com.mamiyaotaru.voxelmap.VoxelMap;
import com.mamiyaotaru.voxelmap.persistent.ThreadManager;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import com.mamiyaotaru.voxelmap.util.CommandUtils;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.text.Text;

import java.nio.charset.StandardCharsets;

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
            String channel = packet.getChannel().getPath();
            PacketByteBuf buffer = packet.getData();
            if (channel.equals("world_info") || channel.equals("world_id")) {
                buffer.readByte();
                byte length = buffer.readByte();
                byte[] bytes = new byte[length];
                buffer.readBytes(bytes);
                String subWorldName = new String(bytes, StandardCharsets.UTF_8);
                this.master.newSubWorldName(subWorldName, true);
                return true;
            }
        }

        return false;
    }
}
