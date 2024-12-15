package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.CoreShaders;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WaypointContainer {
    private final List<Waypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    public final MapSettingsManager options;

    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.wayPts.add(newWaypoint);
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.wayPts.remove(waypoint);
    }

    public void setHighlightedWaypoint(Waypoint highlightedWaypoint) {
        this.highlightedWaypoint = highlightedWaypoint;
    }

    private void sortWaypoints() {
        this.wayPts.sort(Collections.reverseOrder());
    }

    public void renderWaypoints(float partialTicks, Matrix4fStack matrixStack, boolean beacons, boolean signs, boolean withDepth, boolean withoutDepth) {
        this.sortWaypoints();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        double renderPosX = cameraPos.x;
        double renderPosY = cameraPos.y;
        double renderPosZ = cameraPos.z;
        OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        if (this.options.showBeacons && beacons) {
            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(false);
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 1);
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    LevelChunk chunk = VoxelConstants.getPlayer().level().getChunk(x >> 4, z >> 4);
                    if (chunk != null && !chunk.isEmpty() && VoxelConstants.getPlayer().level().hasChunk(x >> 4, z >> 4)) {
                        double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - renderPosY;
                        this.renderBeam(pt, x - renderPosX, bottomOfWorld, z - renderPosZ, matrixStack);
                    }
                }
            }

            OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
            OpenGL.glDepthMask(true);
        }

        if (this.options.showWaypoints && signs) {
            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            OpenGL.glBlendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 1, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    int y = pt.getY();
                    double distance = Math.sqrt(pt.getDistanceSqToCamera(camera));
                    if ((distance < this.options.maxWaypointDisplayDistance || this.options.maxWaypointDisplayDistance < 0 || pt == this.highlightedWaypoint) && !VoxelConstants.getMinecraft().options.hideGui) {
                        boolean isPointedAt = this.isPointedAt(pt, distance, camera);
                        String label = pt.name;
                        this.renderLabel(matrixStack, pt, distance, isPointedAt, label, false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, withDepth, withoutDepth);
                    }
                }
            }

            if (this.highlightedWaypoint != null && !VoxelConstants.getMinecraft().options.hideGui) {
                int x = this.highlightedWaypoint.getX();
                int z = this.highlightedWaypoint.getZ();
                int y = this.highlightedWaypoint.getY();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
                boolean isPointedAt = this.isPointedAt(this.highlightedWaypoint, distance, camera);
                this.renderLabel(matrixStack, this.highlightedWaypoint, distance, isPointedAt, "", true, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ, withDepth, withoutDepth);
            }

            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(true);
            OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
        }

    }

    private boolean isPointedAt(Waypoint waypoint, double distance, Camera camera) {
        Vec3 cameraPos = camera.getPosition();
        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = degrees * 0.0174533;
        double size = Math.sin(angle) * distance;
        Vector3f cameraPosPlusDirection = camera.getLookVector();
        Vec3 cameraPosPlusDirectionTimesDistance = cameraPos.add(cameraPosPlusDirection.x * distance, cameraPosPlusDirection.y * distance, cameraPosPlusDirection.z * distance);
        AABB axisalignedbb = new AABB((waypoint.getX() + 0.5F) - size, (waypoint.getY() + 1.65F) - size, (waypoint.getZ() + 0.5F) - size, (waypoint.getX() + 0.5F) + size, (waypoint.getY() + 1.5F) + size, (waypoint.getZ() + 0.5F) + size);
        Optional<Vec3> raytraceresult = axisalignedbb.clip(cameraPos, cameraPosPlusDirectionTimesDistance);
        if (axisalignedbb.contains(cameraPos)) {
            return distance >= 1.0;
        } else
            return raytraceresult.isPresent();
    }

    private void renderBeam(Waypoint par1EntityWaypoint, double baseX, double baseY, double baseZ, Matrix4f matrix4f) {
        Tesselator tessellator = Tesselator.getInstance();
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.06F;
        double topWidthFactor = 1.05;
        double bottomWidthFactor = 1.05;
        float r = par1EntityWaypoint.red;
        float b = par1EntityWaypoint.blue;
        float g = par1EntityWaypoint.green;

        for (int width = 0; width < 4; ++width) {
            BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            double d6 = 0.1 + width * 0.2;
            d6 *= topWidthFactor;
            double d7 = 0.1 + width * 0.2;
            d7 *= bottomWidthFactor;

            for (int side = 0; side < 5; ++side) {
                float vertX2 = (float) (baseX + 0.5 - d6);
                float vertZ2 = (float) (baseZ + 0.5 - d6);
                if (side == 1 || side == 2) {
                    vertX2 = (float) (vertX2 + d6 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ2 = (float) (vertZ2 + d6 * 2.0);
                }

                float vertX1 = (float) (baseX + 0.5 - d7);
                float vertZ1 = (float) (baseZ + 0.5 - d7);
                if (side == 1 || side == 2) {
                    vertX1 = (float) (vertX1 + d7 * 2.0);
                }

                if (side == 2 || side == 3) {
                    vertZ1 = (float) (vertZ1 + d7 * 2.0);
                }

                vertexBuffer.addVertex(matrix4f, vertX1, (float) baseY + 0.0F, vertZ1).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
                vertexBuffer.addVertex(matrix4f, vertX2, (float) baseY + height, vertZ2).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
            }

            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

    }

    private void renderLabel(Matrix4fStack matrixStack, Waypoint pt, double distance, boolean isPointedAt, String name, boolean target, double baseX, double baseY, double baseZ, boolean withDepth, boolean withoutDepth) {
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                isPointedAt = false;
            }
        }
        if (this.options.distanceUnitConversion && distance > 10000.0){
            name = name + " (" + Math.round(distance / 100.0) / 10.0 + "km)";
        }
        else {
            name = name + " (" + Math.round(distance * 10.0) / 10.0 + "m)";
        }
        double maxDistance = VoxelConstants.getMinecraft().options.simulationDistance().get() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float var14 = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F;
        matrixStack.pushMatrix();
        matrixStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        matrixStack.rotate(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getYRot()));
        matrixStack.rotate(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getXRot()));
        matrixStack.scale(-var14, -var14, -var14);
        Tesselator tessellator = Tesselator.getInstance();
        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        fade = Math.min(fade, !pt.enabled && !target ? 0.3F : 1.0F);
        float width = 10.0F;
        float r = target ? 1.0F : pt.red;
        float g = target ? 0.0F : pt.green;
        float b = target ? 0.0F : pt.blue;
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        OpenGL.Utils.disp2(textureAtlas.getId());
        if (withDepth) {
            OpenGL.glDepthMask(distance < maxDistance);
            OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertexBuffer.addVertex(matrixStack, -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
            vertexBuffer.addVertex(matrixStack, width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);
            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

        if (withoutDepth) {
            OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
            OpenGL.glDepthMask(false);
            BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            vertexBuffer.addVertex(matrixStack, -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
            vertexBuffer.addVertex(matrixStack, width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
            BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
        }

        Font fontRenderer = VoxelConstants.getMinecraft().font;
        if (isPointedAt && fontRenderer != null) {
            byte elevateBy = options.waypointNameBelowIcon ? (byte) 10 : (byte) -19;
            OpenGL.glEnable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            int halfStringWidth = fontRenderer.width(name) / 2;
            RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            if (withDepth) {
                OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
                OpenGL.glDepthMask(distance < maxDistance);
                OpenGL.glPolygonOffset(1.0F, 7.0F);
                BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                OpenGL.glPolygonOffset(1.0F, 5.0F);
                vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            }

            if (withoutDepth) {
                OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
                OpenGL.glDepthMask(false);
                OpenGL.glPolygonOffset(1.0F, 11.0F);
                BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                OpenGL.glPolygonOffset(1.0F, 9.0F);
                vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            }

            OpenGL.glDisable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            OpenGL.glDepthMask(false);
            MultiBufferSource.BufferSource vertexConsumerProvider = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
            if (withoutDepth) {
                int textColor = (int) (255.0F * fade) << 24 | 13421772;
                OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), elevateBy, textColor, false, matrixStack, vertexConsumerProvider, DisplayMode.SEE_THROUGH, 0, 15728880);
                vertexConsumerProvider.endBatch();
            }

            OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        }

        OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        matrixStack.popMatrix();
    }
}
