package com.mamiyaotaru.voxelmap;

public class DebugRenderState {

    public static int checkChunkX;
    public static int checkChunkZ;
    public static int blockX;
    public static int blockY;
    public static int blockZ;
    public static int chunksChanged;
    public static int chunksTotal;

    public static void print() {
        VoxelConstants.getLogger().error("Voxelmap:DebugRenderState -> Chunk: " + checkChunkX + " " + checkChunkZ + " Block: " + blockX + " " + blockY + " " + blockZ);
        VoxelConstants.getLogger().error("Voxelmap:DebugRenderState -> Changed: " + chunksChanged + "/" + chunksTotal);
    }
}
