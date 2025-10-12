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
import net.minecraft.util.Mth;
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
        if (this.options.showBeacons) {
            for (Waypoint pt : this.wayPts) {
                if (pt.isActive() || pt == this.highlightedWaypoint) {
                    int x = pt.getX();
                    int z = pt.getZ();
                    double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - renderPosY;
                    this.renderBeam(pt, x - renderPosX, bottomOfWorld, z - renderPosZ, poseStack, bufferSource);
                }
            }
        }

        if (this.options.showWaypoints) {
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
    }

    private boolean isPointedAt(Waypoint waypoint, double distance, Camera camera) {
        Vec3 cameraPos = camera.getPosition();
        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = degrees * Mth.DEG_TO_RAD;
        double size = Math.sin(angle) * distance;
        Vector3f cameraPosPlusDirection = camera.getLookVector();
        Vec3 cameraPosPlusDirectionTimesDistance = cameraPos.add(cameraPosPlusDirection.x * distance, cameraPosPlusDirection.y * distance, cameraPosPlusDirection.z * distance);
        AABB axisalignedbb = new AABB((waypoint.getX() + 0.5F) - size, (waypoint.getY() + 1.65F) - size, (waypoint.getZ() + 0.5F) - size, (waypoint.getX() + 0.5F) + size, (waypoint.getY() + 1.5F) + size, (waypoint.getZ() + 0.5F) + size);
        Optional<Vec3> raytraceresult = axisalignedbb.clip(cameraPos, cameraPosPlusDirectionTimesDistance);
        if (axisalignedbb.contains(cameraPos)) {
            return distance >= 1.0;
        } else {
            return raytraceresult.isPresent();
        }
    }

    private void renderBeam(Waypoint par1EntityWaypoint, double baseX, double baseY, double baseZ, PoseStack poseStack, BufferSource bufferSource) {
        int height = VoxelConstants.getClientWorld().getHeight();
        float brightness = 0.1F;
        double topWidthFactor = 1.05;
        double bottomWidthFactor = 1.05;
        float r = par1EntityWaypoint.red;
        float b = par1EntityWaypoint.blue;
        float g = par1EntityWaypoint.green;

        VertexConsumer vertexConsumerBeam = bufferSource.getBuffer(VoxelMapRenderTypes.WAYPOINT_BEAM);

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

        double maxDistance = minecraft.options.simulationDistance().get() * 16.0 * 0.99;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float scale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getYRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.getXRot()));
        poseStack.scale(-scale, -scale, -scale);

        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        if (!pt.enabled && !target) {
            fade *= 0.5F;
        }
        float fadeNoDepth = fade;
        if (!isPointedAt) {
            fadeNoDepth *= 0.5F;
        }

        float width = 10.0F;
        float r = target ? 1.0F : pt.red;
        float g = target ? 0.0F : pt.green;
        float b = target ? 0.0F : pt.blue;
        TextureAtlas textureAtlas = VoxelConstants.getVoxelMapInstance().getWaypointManager().getTextureAtlas();
        Sprite icon = target ? textureAtlas.getAtlasSprite("voxelmap:images/waypoints/target.png") : textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint" + pt.imageSuffix + ".png");
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite("voxelmap:images/waypoints/waypoint.png");
        }

        RenderType renderType = VoxelMapRenderTypes.WAYPOINT_ICON_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconDepthtest = bufferSource.getBuffer(renderType);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);
        bufferSource.endBatch(renderType);

        renderType = VoxelMapRenderTypes.WAYPOINT_ICON_NO_DEPTHTEST.apply(icon.getResourceLocation());
        VertexConsumer vertexIconNoDepthtest = bufferSource.getBuffer(renderType);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fadeNoDepth);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fadeNoDepth);
        bufferSource.endBatch(renderType);

        Font fontRenderer = minecraft.font;
        if (isPointedAt) {
            boolean moveLabelDown = this.options.waypointNamesLocation == 1;
            String distanceString = "";
            if (this.options.waypointNamesLocation == 0) {
                name = "";
            }
            if (this.options.waypointDistancesLocation != 0) {
                if (this.options.distanceUnitConversion && distance >= 10000.0) {
                    double converted = distance / 1000.0;
                    distanceString = (int) distance + "." + (int) ((converted - (int) converted) * 10) + "km";
                } else {
                    distanceString = (int) distance + "." + (int) ((distance - (int) distance) * 10) + "m";
                }
                if (name.isEmpty()) {
                    moveLabelDown = this.options.waypointDistancesLocation == 1;
                    name = distanceString;
                    distanceString = "";
                } else if (this.options.waypointDistancesLocation == 1) {
                    name += " (" + distanceString + ")";
                    distanceString = "";
                }
            }

            int textColor = (int) (255.0F * fade) << 24 | 0x00CCCCCC;
            byte elevateBy;
            int halfLabelWidth;

            if (!name.isEmpty()) {
                elevateBy = moveLabelDown ? (distanceString.isEmpty() ? (byte) -18 : (byte) -24) : (byte) 10;
                halfLabelWidth = fontRenderer.width(name) / 2;

                renderType = VoxelMapRenderTypes.WAYPOINT_TEXT_BACKGROUND;
                VertexConsumer vertexBackground = bufferSource.getBuffer(renderType);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                bufferSource.endBatch(renderType);

                fontRenderer.drawInBatch(Component.literal(name), (-fontRenderer.width(name) / 2f), elevateBy, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                bufferSource.endLastBatch();
            }

            if (!distanceString.isEmpty()) {
                float labelScale = 0.75F;

                elevateBy = moveLabelDown ? (byte) -20 : (byte) 26;
                halfLabelWidth = fontRenderer.width(distanceString) / 2;

                poseStack.pushPose();
                poseStack.scale(labelScale, labelScale, 1.0F);

//                renderType = VoxelMapRenderTypes.WAYPOINT_TEXT_BACKGROUND;
                VertexConsumer vertexBackground = bufferSource.getBuffer(renderType);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 2), (9 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 2), (-2 + elevateBy), 0.0F).setColor(pt.red, pt.green, pt.blue, 0.6F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (-halfLabelWidth - 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 1), (8 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                vertexBackground.addVertex(poseStack.last(), (halfLabelWidth + 1), (-1 + elevateBy), 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * fade);
                bufferSource.endBatch(renderType);

                fontRenderer.drawInBatch(Component.literal(distanceString), (-fontRenderer.width(distanceString) / 2f), elevateBy, textColor, false, poseStack.last().pose(), bufferSource, DisplayMode.SEE_THROUGH, 0, 0x00F000F0);
                bufferSource.endLastBatch();

                poseStack.popPose();
            }

        }
        poseStack.popPose();
    }
}
