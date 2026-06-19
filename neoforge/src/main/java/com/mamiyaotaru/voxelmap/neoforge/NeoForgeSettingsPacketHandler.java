package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.SettingsPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NeoForgeSettingsPacketHandler {

    public static void receive(final SettingsPayload data, final IPayloadContext context) {
        SettingsPayload.parsePacket(data);
    }
}
