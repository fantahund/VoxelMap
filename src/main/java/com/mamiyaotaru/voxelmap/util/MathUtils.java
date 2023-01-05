package com.mamiyaotaru.voxelmap.util;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class MathUtils {
    public static Vector4f transform(Vector4f source, Matrix4f matrix) {
        Vector4f returns = new Vector4f();
        float x = source.x;
        float y = source.y;
        float z = source.z;
        float w = source.w;

        returns.x = matrix.m00() * x + matrix.m01() * y + matrix.m02() * z + matrix.m03() * w;
        returns.y = matrix.m10() * x + matrix.m11() * y + matrix.m12() * z + matrix.m13() * w;
        returns.z = matrix.m20() * x + matrix.m21() * y + matrix.m22() * z + matrix.m23() * w;
        returns.w = matrix.m30() * x + matrix.m31() * y + matrix.m32() * z + matrix.m33() * w;

        return returns;
    }

    public static Matrix4f projectionMatrix(float left, float right, float bottom, float top) {
        Matrix4f returns = new Matrix4f();
        float height = top - bottom;

        returns.m00(2.0f / left);
        returns.m11(2.0f / right);
        returns.m22(-2.0f / height);
        returns.m33(1.0f);
        returns.m03(-1.0f);
        returns.m13(1.0f);
        returns.m23(-(top + bottom) / height);
        return returns;
    }
}