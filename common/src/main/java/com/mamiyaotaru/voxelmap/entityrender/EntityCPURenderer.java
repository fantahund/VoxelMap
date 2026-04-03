package com.mamiyaotaru.voxelmap.entityrender;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.ColorUtils;
import com.mamiyaotaru.voxelmap.util.ImageUtils;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class EntityCPURenderer extends AbstractEntityRenderer {
    @Override
    protected void setupMatrix() {
    }

    @Override
    public void render(TextureSet textureSet, Consumer<BufferedImage> resultConsumer) {
        NativeImage primaryTexture = textureSet.primaryTexture() == null ? null : getTextureFromIdentifier(textureSet.primaryTexture());
        NativeImage secondaryTexture = textureSet.secondaryTexture() == null ? null : getTextureFromIdentifier(textureSet.secondaryTexture());
        NativeImage tertiaryTexture = textureSet.tertiaryTexture() == null ? null : getTextureFromIdentifier(textureSet.tertiaryTexture());
        NativeImage quaternaryTexture = textureSet.quaternaryTexture() == null ? null : getTextureFromIdentifier(textureSet.quaternaryTexture());

        NativeImage output = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);

        for (ModelPart modelPart : modelParts) {
            if (primaryTexture != null) {
                drawModelPart(output, modelPart, primaryTexture, textureSet.primaryColor());
            }
            if (secondaryTexture != null) {
                drawModelPart(output, modelPart, secondaryTexture, textureSet.secondaryColor());
            }
            if (tertiaryTexture != null) {
                drawModelPart(output, modelPart, tertiaryTexture, textureSet.tertiaryColor());
            }
            if (quaternaryTexture != null) {
                drawModelPart(output, modelPart, quaternaryTexture, textureSet.quaternaryColor());
            }
        }

        for (BlockModelSet blockModel : blockModels) {
            for (BlockStateModelPart modelPart : blockModel.modelParts()) {
                if (primaryTexture != null) {
                    drawBlockModelPart(output, modelPart, primaryTexture, textureSet.primaryColor());
                }
                if (secondaryTexture != null) {
                    drawBlockModelPart(output, modelPart, secondaryTexture, textureSet.secondaryColor());
                }
                if (tertiaryTexture != null) {
                    drawBlockModelPart(output, modelPart, tertiaryTexture, textureSet.tertiaryColor());
                }
                if (quaternaryTexture != null) {
                    drawBlockModelPart(output, modelPart, quaternaryTexture, textureSet.quaternaryColor());
                }
            }
        }

        resultConsumer.accept(ImageUtils.bufferedImageFromNativeImage(output));
    }

    private NativeImage getTextureFromIdentifier(Identifier identifier) {
        if (identifier.equals(TextureAtlas.LOCATION_BLOCKS)) {
            BufferedImage terrain = VoxelConstants.getVoxelMapInstance().getColorManager().getTerrainImage();
            return terrain == null ? null : ImageUtils.nativeImageFromBufferedImage(terrain);
        }

        AbstractTexture texture = minecraft.getTextureManager().getTexture(identifier);
        if (texture instanceof DynamicTexture dynamicTexture) {
            return dynamicTexture.getPixels();
        }

        try {
            return TextureContents.load(minecraft.getResourceManager(), identifier).image();
        } catch (IOException ignored) {
        }

        return null;
    }

    private void drawModelPart(NativeImage target, ModelPart modelPart, NativeImage texture, int color) {
        ArrayList<RenderPolygon> polygons = new ArrayList<>();

        float centerX = target.getWidth() / 2.0F;
        float centerY = target.getHeight() / 2.0F;

        modelPart.visit(poseStack, (pose, id, i, cube) -> {
            for (ModelPart.Polygon polygon : cube.polygons) {
                polygons.add(new RenderPolygon(polygon, pose, centerX, centerY, 0.0F, 4.0F));
            }
        });

        drawPolygons(target, polygons, texture, color);
    }

    private void drawBlockModelPart(NativeImage target, BlockStateModelPart modelPart, NativeImage texture, int color) {
        ArrayList<RenderPolygon> polygons = new ArrayList<>();

        float centerX = target.getWidth() / 2.0F;
        float centerY = target.getHeight() / 2.0F;

        for (Direction direction : ALL_DIRECTIONS) {
            for (BakedQuad quad : modelPart.getQuads(direction)) {
                ModelPart.Vertex[] vertices = new ModelPart.Vertex[4];
                vertices[0] = createVertex(quad.position0(), quad.packedUV0());
                vertices[1] = createVertex(quad.position1(), quad.packedUV1());
                vertices[2] = createVertex(quad.position2(), quad.packedUV2());
                vertices[3] = createVertex(quad.position3(), quad.packedUV3());

                ModelPart.Polygon polygon = new ModelPart.Polygon(vertices, quad.direction().getUnitVec3f());

                polygons.add(new RenderPolygon(polygon, poseStack.last(), centerX, centerY, 0.0F, 64.0F));
            }
        }

        drawPolygons(target, polygons, texture, color);
    }

    private ModelPart.Vertex createVertex(Vector3fc pos, long packedUV) {
        float u = Float.intBitsToFloat((int) (packedUV >> 32));
        float v = Float.intBitsToFloat((int) (packedUV & 0xFFFFFFFFL));

        return new ModelPart.Vertex(pos.x(), pos.y(), pos.z(), u, v);
    }

    private void drawPolygons(NativeImage target, List<RenderPolygon> polygons, NativeImage texture, int color) {
        polygons.removeIf(x -> cullEnabled && x.normal.z() > 0.0F);
        polygons.sort((x, y) -> Float.compare(-x.getAverageDepth(), -y.getAverageDepth()));

        int targetWidth = target.getWidth();
        int targetHeight = target.getHeight();
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();

        for (RenderPolygon polygon : polygons) {
            float x0 = Float.MAX_VALUE;
            float x1 = Float.MIN_VALUE;
            float y0 = Float.MAX_VALUE;
            float y1 = Float.MIN_VALUE;

            for (ModelPart.Vertex vertex : polygon.vertices) {
                x0 = Math.min(x0, vertex.x());
                x1 = Math.max(x1, vertex.x());
                y0 = Math.min(y0, vertex.y());
                y1 = Math.max(y1, vertex.y());
            }

            int ix0 = Math.max(0, Math.round(x0));
            int ix1 = Math.min(targetWidth - 1, Math.round(x1));
            int iy0 = Math.max(0, Math.round(y0));
            int iy1 = Math.min(targetHeight - 1, Math.round(y1));

            for (int y = iy0; y < iy1; y++) {
                for (int x = ix0; x < ix1; x++) {
                    Vector2f uv = getUVCoordinates(polygon, x, y);
                    if (uv == null) continue;

                    int uCoord = (int) (uv.x() * texWidth);
                    int vCoord = (int) (uv.y() * texHeight);

                    if (uCoord >= 0 && vCoord >= 0 && uCoord < texWidth && vCoord < texHeight) {
                        int texColor = ColorUtils.colorMultiplier(texture.getPixel(uCoord, vCoord), color);
                        if ((texColor >> 24 & 255) == 255) {
                            target.setPixel(x, y, texColor);
                        } else {
                            int baseColor = target.getPixel(x, y);
                            target.setPixel(x, y, ColorUtils.colorAdder(texColor, baseColor));
                        }
                    }
                }
            }
        }
    }

    private Vector2f getUVCoordinates(RenderPolygon polygon, float x, float y) {
        ModelPart.Vertex[] v = polygon.vertices;

        if (v.length < 3) return null;

        Vector2f uv = getUvFromTriangle(polygon, x, y, 0, 1, 2);
        if (uv == null && v.length >= 4) {
            uv = getUvFromTriangle(polygon, x, y, 0, 2, 3);
        }

        return uv;
    }

    private Vector2f getUvFromTriangle(RenderPolygon polygon, float x, float y, int i0, int i1, int i2) {
        ModelPart.Vertex[] v = polygon.vertices;

        Vector3f p0 = new Vector3f(v[i0].x(), v[i0].y(), v[i0].z());
        Vector3f p1 = new Vector3f(v[i1].x(), v[i1].y(), v[i1].z());
        Vector3f p2 = new Vector3f(v[i2].x(), v[i2].y(), v[i2].z());
        Vector2f v0 = new Vector2f(p0.x - p2.x, p0.y - p2.y);
        Vector2f v1 = new Vector2f(p1.x - p2.x, p1.y - p2.y);
        Vector2f v2 = new Vector2f(x + 0.5F - p2.x, y + 0.5F - p2.y);

        float den = v0.x * v1.y - v1.x * v0.y;
        float a = (v2.x * v1.y - v1.x * v2.y) / den;
        float b = (v0.x * v2.y - v2.x * v0.y) / den;
        float c = 1.0F - b - a;

        float epsilon = -0.001F;
        if (a < epsilon || b < epsilon || c < epsilon) {
            return null;
        }

        return new Vector2f(
                a * polygon.vertices[i0].u() + b * polygon.vertices[i1].u() + c * polygon.vertices[i2].u(),
                a * polygon.vertices[i0].v() + b * polygon.vertices[i1].v() + c * polygon.vertices[i2].v()
        );
    }

    public static class RenderPolygon {
        public final ModelPart.Polygon polygon;
        public final ModelPart.Vertex[] vertices;
        public final Vector3f normal;

        public RenderPolygon(ModelPart.Polygon polygon, PoseStack.Pose pose, float x, float y, float z, float scale) {
            this.polygon = polygon;

            vertices = polygon.vertices().clone();
            for (int i = 0; i < vertices.length; i++) {
                ModelPart.Vertex v = vertices[i];
                float s = 16.0F;
                Vector4f pos = pose.pose().transform(new Vector4f(v.x() / s, v.y() / s, v.z() / s, 1.0F));

                vertices[i] = new ModelPart.Vertex(pos.x * s * scale + x, pos.y * s * scale + y, pos.z * s * scale + z, v.u(), v.v());
            }

            if (vertices.length < 3) {
                normal = new Vector3f(0.0F, 0.0F, 1.0F);
            } else {
                Vector3f p0 = new Vector3f(vertices[0].x(), vertices[0].y(), vertices[0].z());
                Vector3f p1 = new Vector3f(vertices[1].x(), vertices[1].y(), vertices[1].z());
                Vector3f p2 = new Vector3f(vertices[2].x(), vertices[2].y(), vertices[2].z());
                Vector3f v0 = new Vector3f(p0.x - p2.x, p0.y - p2.y, p0.z - p2.z);
                Vector3f v1 = new Vector3f(p1.x - p2.x, p1.y - p2.y, p1.z - p2.z);

                float nx = v0.y * v1.z - v0.z * v1.y;
                float ny = v0.z * v1.x - v0.x * v1.z;
                float nz = v0.x * v1.y - v0.y * v1.x;

                normal = new Vector3f(nx, ny, nz);

                float lengthSq = normal.x * normal.x + normal.y * normal.y + normal.z * normal.z;
                float epsilon = 1.0E-6F;
                if (lengthSq > epsilon) {
                    float invLength = 1.0F / (float) Math.sqrt(lengthSq);
                    normal.mul(invLength);
                }
            }
        }

        public float getAverageDepth() {
            float val = 0.0F;
            for (ModelPart.Vertex v : vertices) {
                val += v.z();
            }

            return val / vertices.length;
        }
    }
}
