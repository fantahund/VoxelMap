package com.mamiyaotaru.voxelmap.rendering;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class IrisCompat {
    private static final VarHandle IS_RENDERING_LEVEL = resolveIsRenderingLevel();

    private static VarHandle resolveIsRenderingLevel() {
        try {
            Class<?> immediateState = Class.forName("net.irisshaders.iris.vertices.ImmediateState");
            return MethodHandles.lookup().findStaticVarHandle(immediateState, "isRenderingLevel", boolean.class);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static boolean pushForceNotRenderingLevel() {
        if (IS_RENDERING_LEVEL == null) {
            return false;
        }
        try {
            boolean previous = (boolean) IS_RENDERING_LEVEL.get();
            IS_RENDERING_LEVEL.set(false);
            return previous;
        } catch (Throwable t) {
            VoxelConstants.getLogger().warn("IrisCompat: failed to clear isRenderingLevel: {}", t.toString());
            return false;
        }
    }

    public static void popForceNotRenderingLevel(boolean previous) {
        if (IS_RENDERING_LEVEL == null) {
            return;
        }
        IS_RENDERING_LEVEL.set(previous);
    }
}
