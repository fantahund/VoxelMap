package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class WaypointContainer {
    private final List<Waypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;
    public final MapSettingsManager options;
    public final Minecraft minecraft = Minecraft.getInstance();

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

    public void renderWaypoints(float gameTimeDeltaPartialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        this.sortWaypoints();
        Vec3 cameraPos = camera.getPosition();
        double renderPosX = cameraPos.x;
        double renderPosY = cameraPos.y;
        double renderPosZ = cameraPos.z;
        // OpenGL.glEnable(OpenGL.GL11_GL_CULL_FACE);
        if (this.options.showBeacons) {
            // OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            // OpenGL.glDepthMask(false);
            // OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            // OpenGL.glBlendFunc(OpenGL.GL11_GL_SRC_ALPHA, 1);
            // RenderSystem.setShader(CoreShaders.POSITION_COLOR);

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    // LevelChunk chunk = VoxelConstants.getPlayer().level().getChunk(x >> 4, z >> 4);
                    // if (chunk != null && !chunk.isEmpty() && VoxelConstants.getPlayer().level().hasChunk(x >> 4, z >> 4)) {
                    double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - renderPosY;
                    this.renderBeam(pt, x - renderPosX, bottomOfWorld, z - renderPosZ, poseStack, bufferSource);
                    // }
                }
            }

            // OpenGL.glDisable(OpenGL.GL11_GL_BLEND);
            // OpenGL.glDepthMask(true);
        }

        if (this.options.showWaypoints) {
            // OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
            // OpenGL.glBlendFuncSeparate(OpenGL.GL11_GL_SRC_ALPHA, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA, 1, OpenGL.GL11_GL_ONE_MINUS_SRC_ALPHA);

            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    int y = pt.getY();
                    double distance = Math.sqrt(pt.getDistanceSqToCamera(camera));
                    if ((distance < this.options.maxWaypointDisplayDistance || this.options.maxWaypointDisplayDistance < 0 || pt == this.highlightedWaypoint) && !VoxelConstants.getMinecraft().options.hideGui) {
                        boolean isPointedAt = this.isPointedAt(pt, distance, camera);
                        String label = pt.name;
                        this.renderLabel(poseStack, bufferSource, pt, distance, isPointedAt, label, false, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ);
                    }
                }
            }

            if (this.highlightedWaypoint != null && !VoxelConstants.getMinecraft().options.hideGui) {
                int x = this.highlightedWaypoint.getX();
                int z = this.highlightedWaypoint.getZ();
                int y = this.highlightedWaypoint.getY();
                double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
                boolean isPointedAt = this.isPointedAt(this.highlightedWaypoint, distance, camera);
                this.renderLabel(poseStack, bufferSource, this.highlightedWaypoint, distance, isPointedAt, "", true, x - renderPosX, y - renderPosY + 1.12, z - renderPosZ);
            }
        }
        // bufferSource.endBatch(GLUtils.WAYPOINT_BEAM);
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

    private void renderBeam(Waypoint par1EntityWaypoint, double baseX, double baseY, double baseZ, PoseStack poseStack, BufferSource bufferSource) {
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.1F;
        double topWidthFactor = 1.05;
        double bottomWidthFactor = 1.05;
        float r = par1EntityWaypoint.red;
        float b = par1EntityWaypoint.blue;
        float g = par1EntityWaypoint.green;

        VertexConsumer vertexConsumerBeam = bufferSource.getBuffer(GLUtils.WAYPOINT_BEAM);

        for (int width = 0; width < 4; ++width) {
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

                vertexConsumerBeam.addVertex(poseStack.last(), vertX1, (float) baseY + 0.0F, vertZ1).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
                vertexConsumerBeam.addVertex(poseStack.last(), vertX2, (float) baseY + height, vertZ2).setColor(r * brightness, g * brightness, b * brightness, 0.8F);
            }
        }
    }

    private void renderLabel(PoseStack poseStack, BufferSource bufferSource, Waypoint pt, double distance, boolean isPointedAt, String name, boolean target, double baseX, double baseY, double baseZ/* , boolean withDepth, boolean withoutDepth */) {
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                isPointedAt = false;
            }
        }
        String distStr;
        if (this.options.distanceUnitConversion && distance > 10000.0) {
            distStr = (Math.round(distance / 100.0) / 10.0) + "km";
        } else if (distance >= 9999999.0F) {
            distStr = (int) distance + "m";
        } else {
            distStr = (Math.round(distance * 10.0) / 10.0) + "m";
        }
        if (!this.options.waypointDistanceBelowName) {
            name = name + " (" + distStr + ")";
        }
        double maxDistance = minecraft.options.simulationDistance().get() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float var14 = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getXRot()));
        poseStack.scale(-var14, -var14, -var14);
        // Tesselator tessellator = Tesselator.getInstance();
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

        // if (withDepth) {
        RenderType rt = GLUtils.WAYPOINT_ICON_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconDepthtest = bufferSource.getBuffer(rt);
        // OpenGL.glDepthMask(distance < maxDistance);
        // OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);

        // bufferSource.endBatch(rt);
        // }

        // if (withoutDepth) {
        rt = GLUtils.WAYPOINT_ICON_NO_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconNoDepthtest = bufferSource.getBuffer(rt);
        // OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
        // OpenGL.glDepthMask(false);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, 0.3F * fade);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, 0.3F * fade);
        // bufferSource.endBatch(rt);
        // }

        Font fontRenderer = minecraft.font;
        if (isPointedAt && fontRenderer != null) {
            byte elevateBy = this.options.waypointNameBelowIcon ? (byte) 10 : (byte) -19;
            byte elevateDistBy = this.options.waypointNameBelowIcon ? (byte) 30 : (byte) -39;
            float distTextScale = 0.65F;

            rt = GLUtils.WAYPOINT_TEXT_BACKGROUND;
            VertexConsumer vertexBackground = bufferSource.getBuffer(rt);

            // OpenGL.glEnable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            int halfStringWidth = fontRenderer.width(name) / 2;
            int halfDistStringWidth = fontRenderer.width(distStr) / 2;
            // RenderSystem.setShader(CoreShaders.POSITION_COLOR);
            // if (withDepth) {
            // OpenGL.glEnable(OpenGL.GL11_GL_DEPTH_TEST);
            // OpenGL.glDepthMask(distance < maxDistance);
            // OpenGL.glPolygonOffset(1.0F, 7.0F);
            // BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            vertexBackground.addVertex(poseStack.last(), (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
            vertexBackground.addVertex(poseStack.last(), (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
            vertexBackground.addVertex(poseStack.last(), (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
            vertexBackground.addVertex(poseStack.last(), (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
            // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            // OpenGL.glPolygonOffset(1.0F, 5.0F);
            // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            vertexBackground.addVertex(poseStack.last(), (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            vertexBackground.addVertex(poseStack.last(), (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            vertexBackground.addVertex(poseStack.last(), (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            vertexBackground.addVertex(poseStack.last(), (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            //
            if (this.options.waypointDistanceBelowName) {
                poseStack.pushPose();
                poseStack.scale(distTextScale, distTextScale, distTextScale);
                // OpenGL.glPolygonOffset(1.0F, 7.0F);
                // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBackground.addVertex(poseStack.last(), (-halfDistStringWidth - 2), (-2 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfDistStringWidth - 2), (9 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfDistStringWidth + 2), (9 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfDistStringWidth + 2), (-2 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                // OpenGL.glPolygonOffset(1.0F, 5.0F);
                // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                vertexBackground.addVertex(poseStack.last(), (-halfDistStringWidth - 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfDistStringWidth - 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfDistStringWidth + 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfDistStringWidth + 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                poseStack.popPose();
            }
            // }
            //
            // if (withoutDepth) {
            // OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
            // OpenGL.glDepthMask(false);
            // OpenGL.glPolygonOffset(1.0F, 11.0F);
            // BufferBuilder vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (halfStringWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
            // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            // OpenGL.glPolygonOffset(1.0F, 9.0F);
            // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            // vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (-halfStringWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            // vertexBuffer.addVertex(matrixStack, (halfStringWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
            // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
            //
            if (this.options.waypointDistanceBelowName) {
                // matrixStack.pushMatrix();
                // matrixStack.scale(distTextScale);
                // OpenGL.glPolygonOffset(1.0F, 11.0F);
                // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                // vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (-2 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 2), (9 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (9 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 2), (-2 + elevateDistBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.15F * fade);
                // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                // OpenGL.glPolygonOffset(1.0F, 9.0F);
                // vertexBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                // vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (-halfDistStringWidth - 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (8 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                // vertexBuffer.addVertex(matrixStack, (halfDistStringWidth + 1), (-1 + elevateDistBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                // BufferUploader.drawWithShader(vertexBuffer.buildOrThrow());
                // matrixStack.popMatrix();
            }
            // }
            //
            // OpenGL.glDisable(OpenGL.GL11_GL_POLYGON_OFFSET_FILL);
            // OpenGL.glDepthMask(false);
            // MultiBufferSource.BufferSource vertexConsumerProvider = VoxelConstants.getMinecraft().renderBuffers().bufferSource();
            // if (withoutDepth) {
            int textColor = (int) (255.0F * fade) << 24 | 0x00cccccc;
            // OpenGL.glDisable(OpenGL.GL11_GL_DEPTH_TEST);
            fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), elevateBy, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00f000f0);
            if (this.options.waypointDistanceBelowName) {
                poseStack.pushPose();
                poseStack.scale(distTextScale, distTextScale, distTextScale);
                fontRenderer.drawInBatch(Component.literal(distStr), (-fontRenderer.width(distStr) / 2f), elevateDistBy, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00f000f0);
                poseStack.popPose();
            }
            // vertexConsumerProvider.endBatch();
            // }
            //
            // OpenGL.glEnable(OpenGL.GL11_GL_BLEND);
        }
        // OpenGL.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
