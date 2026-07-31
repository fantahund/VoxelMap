package com.mamiyaotaru.voxelmap.multiloader;

import com.mamiyaotaru.voxelmap.VoxelConstants;

public final class MultiLoaderManager {
    private static Events events;
    private static PacketBridge packetBridge;
    private static ModApiBridge modApiBridge;
    private static PackRegistrar packRegistrar;

    private MultiLoaderManager() {
    }

    public static Events getEvents() {
        return events;
    }

    public static void setEvents(Events events) {
        MultiLoaderManager.events = events;
        VoxelConstants.getVoxelMapInstance().onEventsSet(events);
    }

    public static PacketBridge getPacketBridge() {
        return packetBridge;
    }

    public static void setPacketBridge(PacketBridge packetBridge) {
        MultiLoaderManager.packetBridge = packetBridge;
    }

    public static ModApiBridge getModApiBridge() {
        return modApiBridge;
    }

    public static void setModApiBride(ModApiBridge modApiBridge) {
        MultiLoaderManager.modApiBridge = modApiBridge;
    }

    public static PackRegistrar getPackRegistrar() {
        return packRegistrar;
    }

    public static void setPackRegistrar(PackRegistrar packRegistrar) {
        MultiLoaderManager.packRegistrar = packRegistrar;
        VoxelConstants.getVoxelMapInstance().registerPacks(packRegistrar);
    }
}
