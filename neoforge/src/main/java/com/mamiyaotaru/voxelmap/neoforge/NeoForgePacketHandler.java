package com.mamiyaotaru.voxelmap.neoforge;

import com.mamiyaotaru.voxelmap.packets.VoxelMapSettingsPayload;
import com.mamiyaotaru.voxelmap.packets.VoxelMapWorldIdPayload;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class NeoForgePacketHandler {
    private NeoForgePacketHandler() {
    }

    public static void initCommon(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").optional();
        registrar.commonToClient(VoxelMapSettingsPayload.PACKET_ID, VoxelMapSettingsPayload.PACKET_CODEC);
        registrar.commonBidirectional(VoxelMapWorldIdPayload.PACKET_ID, VoxelMapWorldIdPayload.PACKET_CODEC, (_, _) -> {
            // The legacy worldinfo request channel is optional.
            // NeoForge only allows one payload type per id,
            // so the empty S2C payload is treated as a serverbound request here.
        });
    }

    public static void initClient(final RegisterClientPayloadHandlersEvent event) {
        event.register(VoxelMapSettingsPayload.PACKET_ID, (payload, _) -> VoxelMapSettingsPayload.parse(payload));
        event.register(VoxelMapWorldIdPayload.PACKET_ID, (payload, _) -> VoxelMapWorldIdPayload.parse(payload));
    }
}
