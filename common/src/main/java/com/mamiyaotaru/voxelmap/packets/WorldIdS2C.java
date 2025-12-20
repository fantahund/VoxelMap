package com.mamiyaotaru.voxelmap.packets;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.nio.charset.StandardCharsets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldIdS2C(String worldName) {
    public static final ResourceLocation PACKET_ID = new ResourceLocation("worldinfo", "world_id");

    public WorldIdS2C(FriendlyByteBuf buf) {
        this(parse(buf));
    }

    private static String parse(FriendlyByteBuf buf) {
        buf.readByte(); // ignore
        int length;
        int b = buf.readUnsignedByte();
        if (b == 42) {
            // "new" packet
            length = buf.readUnsignedByte();
        } else if (b == 0) {
            // length == 0 ?
            VoxelConstants.getLogger().warn("Received unknown world_id packet");
            return null;
        } else {
            // probably "legacy" packet
            VoxelConstants.getLogger().warn("Assuming legacy world_id packet. " +
                    "The support might be removed in the future versions.");
            length = b;
        }
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeByte(0);
        buf.writeByte(42);
        buf.writeByte(0);
    }

    public static void updateWorld(WorldIdS2C packet) {
        String worldName = packet.worldName;
        VoxelConstants.getLogger().info("Received world_id: " + worldName);
        if (worldName != null) {
            VoxelConstants.getVoxelMapInstance().newSubWorldName(worldName, true);
        }
    }
}
