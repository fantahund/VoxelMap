package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.WorldIdPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NeoForgeWorldIdPacketHandler {

    public static void receive(final WorldIdPayload data, final IPayloadContext context) {
        WorldIdPayload.parsePacket(data);
    }
}
