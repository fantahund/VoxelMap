package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.MapSettingsManager;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Font.DisplayMode;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WaypointContainer {
    public final MapSettingsManager options;
    public final Minecraft minecraft = Minecraft.getInstance();

    private final List<ExtendedWaypoint> wayPts = new ArrayList<>();
    private Waypoint highlightedWaypoint;

    private static final float INVALID_OFFSET = -1.0F;

    public static class ExtendedWaypoint implements Comparable<ExtendedWaypoint> {
        public Waypoint waypoint;
        public float offset;
        public boolean target;

        public ExtendedWaypoint(Waypoint waypoint) {
            this.waypoint = waypoint;
        }

        public int compareTo(ExtendedWaypoint o) {
            boolean skip1 = offset == INVALID_OFFSET || (!waypoint.enabled && !target) || !waypoint.inWorld || !waypoint.inDimension;
            boolean skip2 = o.offset == INVALID_OFFSET || (!o.waypoint.enabled && !o.target) || !o.waypoint.inWorld || !o.waypoint.inDimension;

            if (skip1 && !skip2) return 1;
            if (!skip1 && skip2) return -1;

            return Float.compare(offset, o.offset);
        }
    }


    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
    }

    public void addWaypoint(Waypoint newWaypoint) {
        this.wayPts.add(new ExtendedWaypoint(newWaypoint));
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.wayPts.removeIf(point -> point.waypoint == waypoint);
    }

    public void setHighlightedWaypoint(Waypoint highlightedWaypoint) {
        this.highlightedWaypoint = highlightedWaypoint;
    }

    private void sortWaypoints() {
        this.wayPts.sort(Collections.reverseOrder());
    }

    public void renderWaypoints(float gameTimeDeltaPartialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        this.renderWaypointsBeams(gameTimeDeltaPartialTick, poseStack, bufferSource, camera);
        this.renderWaypointsLabels(gameTimeDeltaPartialTick, poseStack, bufferSource, camera);
    }

    public void renderWaypointsBeams(float gameTimeDeltaPartialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        Vec3 cameraPos = camera.position();
        double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - cameraPos.y;

        if (!this.options.showBeacons) return;
        for (ExtendedWaypoint pt : this.wayPts) {
            boolean isHighlighted = pt.waypoint == this.highlightedWaypoint;
            boolean isEffectivelyActive = pt.waypoint.isActive();
            if (isHighlighted) isEffectivelyActive = true;

            if (!isEffectivelyActive) continue;

            int x = pt.waypoint.getX();
            int z = pt.waypoint.getZ();
            double distance = Math.sqrt(pt.waypoint.getDistanceSqToCamera(camera));

            this.renderBeam(poseStack, bufferSource, pt.waypoint, distance, x - cameraPos.x, bottomOfWorld, z - cameraPos.z);
        }
    }

    public void renderWaypointsLabels(float gameTimeDeltaPartialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        if (!this.options.showWaypoints) return;
        if (minecraft.options.hideGui) return;

        Vec3 cameraPos = camera.position();
        this.sortWaypoints();
        boolean shiftDown = minecraft.options.keyShift.isDown();
        int last = this.wayPts.size() - 1;
        for (int i = 0; i < wayPts.size(); i++) {
            ExtendedWaypoint pt = wayPts.get(i);

            boolean isHighlighted = pt.waypoint == this.highlightedWaypoint;
            pt.target = isHighlighted;

            boolean isEffectivelyActive = pt.waypoint.isActive();
            if (isHighlighted) isEffectivelyActive = true;

            if (!isEffectivelyActive) {
                pt.offset = INVALID_OFFSET;
                continue;
            }

            int x = pt.waypoint.getX();
            int z = pt.waypoint.getZ();
            int y = pt.waypoint.getY();
            double distance = Math.sqrt(pt.waypoint.getDistanceSqToCamera(camera));

            boolean isOutOfRange = this.options.maxWaypointDisplayDistance >= 0 && distance >= this.options.maxWaypointDisplayDistance;
            if (isOutOfRange) isEffectivelyActive = false;
            if (isHighlighted) isEffectivelyActive = true;

            if (!isEffectivelyActive) {
                pt.offset = INVALID_OFFSET;
                continue;
            }

            pt.offset = getCenterOffset(pt.waypoint, distance, camera);
            boolean isPointedAt = pt.offset != INVALID_OFFSET && (shiftDown || i == last);
            this.renderLabel(poseStack, bufferSource, pt.waypoint, distance, isPointedAt, false, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
        }

        if (this.highlightedWaypoint != null && !VoxelConstants.getMinecraft().options.hideGui) {
            int x = this.highlightedWaypoint.getX();
            int z = this.highlightedWaypoint.getZ();
            int y = this.highlightedWaypoint.getY();
            double distance = Math.sqrt(this.highlightedWaypoint.getDistanceSqToCamera(camera));
            boolean isPointedAt = this.getCenterOffset(this.highlightedWaypoint, distance, camera) != INVALID_OFFSET;
            this.renderLabel(poseStack, bufferSource, this.highlightedWaypoint, distance, isPointedAt, true, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
        }
    }

    private float getCenterOffset(Waypoint waypoint, double distance, Camera camera) {
        if (distance < 1.0) {
            return INVALID_OFFSET;
        }

        Vec3 cameraPos = camera.position();
        float dx = (waypoint.getX() + 0.5F) - (float) cameraPos.x();
        float dy = (waypoint.getY() + 1.5F) - (float) cameraPos.y();
        float dz = (waypoint.getZ() + 0.5F) - (float) cameraPos.z();

        float zo = camera.forwardVector().dot(dx, dy, dz);
        if (zo < 0.0F) {
            return INVALID_OFFSET;
        }

        float xo = camera.leftVector().dot(dx, dy, dz);
        float yo = camera.upVector().dot(dx, dy, dz);
        float centerOffset = (yo * yo) + (xo * xo);

        double degrees = 5.0 + Math.min(5.0 / distance, 5.0);
        double angle = degrees * Mth.DEG_TO_RAD;
        double size = Math.max(Math.sin(angle) * distance, 0.5) * this.options.waypointSignScale;

        if (centerOffset <= size * size) {
            return centerOffset;
        }

        return INVALID_OFFSET;
    }

    private void renderBeam(PoseStack poseStack, BufferSource bufferSource, Waypoint par1EntityWaypoint, double distance, double baseX, double baseY, double baseZ) {
        int height = VoxelConstants.getClientWorld().getHeight();

        float spentTime = minecraft.getCameraEntity().tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float texturePos = Mth.frac(spentTime * 0.2F - Mth.floor(spentTime * 0.1F));

        // Edited from minecraft, net.minecraft.client.renderer.blockentity.BeaconRenderer
        poseStack.pushPose();
        poseStack.translate(baseX + 0.5, baseY, baseZ + 0.5);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spentTime * 2.25F - 45.0F));

        float beamRadius = BeaconRenderer.SOLID_BEAM_RADIUS / 1.4142F;
        float beamMaxV = 1.0F - texturePos;
        float beamMinV = height * (0.5F / BeaconRenderer.SOLID_BEAM_RADIUS) + beamMaxV;
        int beamColor = par1EntityWaypoint.getUnifiedColor(1.0F);

        RenderType beamRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, false);
        VertexConsumer beamBuffer = bufferSource.getBuffer(beamRenderType);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -beamRadius : beamRadius;
            float z = (face < 2) ? -beamRadius : beamRadius;
            float x2 = (face < 2) ? -beamRadius : beamRadius;
            float z2 = (face == 1 || face == 2) ? -beamRadius : beamRadius;

            beamBuffer.addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);

        }
        bufferSource.endBatch(beamRenderType);

        poseStack.popPose();

        float glowRadius = BeaconRenderer.BEAM_GLOW_RADIUS;
        float glowMaxV = 1.0F - texturePos;
        float glowMinV = height + beamMaxV;
        int glowColor = par1EntityWaypoint.getUnifiedColor(0.125F);

        RenderType glowRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -glowRadius : glowRadius;
            float z = (face < 2) ? -glowRadius : glowRadius;
            float x2 = (face < 2) ? -glowRadius : glowRadius;
            float z2 = (face == 1 || face == 2) ? -glowRadius : glowRadius;

            glowBuffer.addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightCoordsUtil.FULL_BRIGHT);

        }
        bufferSource.endBatch(glowRenderType);

        poseStack.popPose();

    }

    private void renderLabel(PoseStack poseStack, BufferSource bufferSource, Waypoint pt, double distance, boolean isPointedAt, boolean target, double baseX, double baseY, double baseZ/* , boolean withDepth, boolean withoutDepth */) {
        String name = pt.name;
        if (target) {
            if (pt.red == 2.0F && pt.green == 0.0F && pt.blue == 0.0F) {
                name = "X:" + pt.getX() + ", Y:" + pt.getY() + ", Z:" + pt.getZ();
            } else {
                isPointedAt = false;
            }
        }

        double maxDistance = minecraft.gameRenderer.getDepthFar() - 8.0;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float scale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * this.options.waypointSignScale;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.xRot()));
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
        Sprite icon = target ? textureAtlas.getAtlasSprite("marker/target") : textureAtlas.getAtlasSprite("selectable/" + pt.imageSuffix);
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
        }

        RenderType renderType = VoxelMapRenderTypes.WAYPOINT_ICON_DEPTH_TEST.apply(icon.getIdentifier());
        VertexConsumer vertexIconDepthtest = bufferSource.getBuffer(renderType);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, fade);
        vertexIconDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, fade);
        bufferSource.endBatch(renderType);

        renderType = VoxelMapRenderTypes.WAYPOINT_ICON_NO_DEPTH_TEST.apply(icon.getIdentifier());
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
                boolean shouldConvertUnit = this.options.distanceUnitConversionMode != 0
                        && ((this.options.distanceUnitConversionMode == 1 && distance > 1000.0) || (this.options.distanceUnitConversionMode == 2 && distance > 10000.0));

                if (shouldConvertUnit) {
                    double converted = distance / 1000.0;
                    distanceString = (int) converted + "." + (int) ((converted - (int) converted) * 10) + "km";
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

            int textColor = (int) (255.0F * fade) << 24 | 0x00FFFFFF;
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
