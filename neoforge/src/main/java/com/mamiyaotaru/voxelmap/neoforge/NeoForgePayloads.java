package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelmapSettingsS2C;
import com.mamiyaotaru.voxelmap.packets.WorldIdS2C;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class NeoForgePayloads {
    private NeoForgePayloads() {
    }

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.commonToClient(VoxelmapSettingsS2C.PACKET_ID, VoxelmapSettingsS2C.PACKET_CODEC);
        registrar.commonBidirectional(WorldIdS2C.PACKET_ID, WorldIdS2C.PACKET_CODEC, NeoForgePayloads::handleWorldIdRequest);
    }

    private static void handleWorldIdRequest(final WorldIdS2C data, final IPayloadContext context) {
        // The legacy worldinfo request channel is optional. NeoForge only allows
        // one payload type per id, so the empty S2C payload is treated as a
        // serverbound request here.
    }
}
