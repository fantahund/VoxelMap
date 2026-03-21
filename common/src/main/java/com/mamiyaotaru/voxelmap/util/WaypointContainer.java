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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;

public class WaypointContainer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final MapSettingsManager options;
    private final WaypointManager waypointManager;

    private final ArrayList<RenderableWaypoint> renderables = new ArrayList<>();
    private static final float INVALID_OFFSET = -1.0F;


    public WaypointContainer(MapSettingsManager options) {
        this.options = options;
        this.waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
    }

    public void refreshRenderables() {
        renderables.clear();

        for (Waypoint waypoint : waypointManager.getWaypoints()) {
            boolean highlighted = waypointManager.isWaypointHighlight(waypoint);
            RenderableWaypoint renderable = new RenderableWaypoint(waypoint, highlighted);

            renderables.add(renderable);
        }

        Waypoint highlighted = waypointManager.getHighlightedWaypoint();
        if (highlighted != null && waypointManager.isCoordinateHighlight(highlighted)) {
            RenderableWaypoint renderable = new RenderableWaypoint(highlighted, true);

            renderables.add(renderable);
        }
    }

    private void sortWaypoints() {
        renderables.sort(Collections.reverseOrder());
    }

    public void renderWaypoints(float partialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        if (waypointManager == null) return;
        if (renderables.isEmpty()) return;

        if (options.showWaypointBeacons) {
            renderWaypointBeams(partialTick, poseStack, bufferSource, camera);
        }
        if (options.showWaypointSigns) {
            renderWaypointSigns(partialTick, poseStack, bufferSource, camera);
        }
    }

    public void renderWaypointBeams(float partialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        Vec3 cameraPos = camera.position();
        double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - cameraPos.y;

        for (RenderableWaypoint renderable : renderables) {
            Waypoint waypoint = renderable.getWaypoint();
            boolean isEffectivelyActive = waypoint.isActive() || renderable.isHighlighted();

            if (!isEffectivelyActive) continue;

            int x = waypoint.getX();
            int z = waypoint.getZ();
            double distance = Math.sqrt(waypoint.getDistanceSqToCamera(camera));

            renderBeam(poseStack, bufferSource, waypoint, distance, x - cameraPos.x, bottomOfWorld, z - cameraPos.z);
        }
    }

    public void renderWaypointSigns(float partialTick, PoseStack poseStack, BufferSource bufferSource, Camera camera) {
        if (minecraft.options.hideGui) return;

        Vec3 cameraPos = camera.position();
        sortWaypoints();
        TextureAtlas textureAtlas = waypointManager.getTextureAtlas();
        boolean shiftDown = minecraft.options.keyShift.isDown();

        RenderableWaypoint last = renderables.getLast();
        for (RenderableWaypoint renderable : renderables) {
            Waypoint waypoint = renderable.getWaypoint();

            boolean isHighlighted = renderable.isHighlighted();
            boolean isEffectivelyActive = waypoint.isActive() || isHighlighted;
            if (!isEffectivelyActive) {
                renderable.setOffset(INVALID_OFFSET);
                continue;
            }

            int x = waypoint.getX();
            int z = waypoint.getZ();
            int y = waypoint.getY();
            double distance = Math.sqrt(waypoint.getDistanceSqToCamera(camera));

            boolean isOutOfRange = options.maxWaypointDisplayDistance >= 0 && distance >= options.maxWaypointDisplayDistance;
            isEffectivelyActive = !isOutOfRange || isHighlighted;
            if (!isEffectivelyActive) {
                renderable.setOffset(INVALID_OFFSET);
                continue;
            }

            float centerOffset = getCenterOffset(waypoint, distance, camera);
            renderable.setOffset(centerOffset);

            boolean isPointedAt = renderable.getOffset() != INVALID_OFFSET && (shiftDown || renderable == last);
            if (waypointManager.isWaypointHighlight(waypoint)) {
                // Render base waypoint
                renderSign(poseStack, bufferSource, waypoint, textureAtlas, isPointedAt, false, distance, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
            }
            renderSign(poseStack, bufferSource, waypoint, textureAtlas, isPointedAt, isHighlighted, distance, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
        }
    }

    private float getCenterOffset(Waypoint waypoint, double distance, Camera camera) {
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
        double size = Math.max(Math.sin(angle) * distance, 0.5) * options.waypointSignScale;

        if (centerOffset <= size * size) {
            return centerOffset;
        }

        return INVALID_OFFSET;
    }

    /**
     * Edited from {@link net.minecraft.client.renderer.blockentity.BeaconRenderer}
     */
    private void renderBeam(PoseStack poseStack, BufferSource bufferSource, Waypoint waypoint, double distance, double baseX, double baseY, double baseZ) {
        int height = VoxelConstants.getClientWorld().getHeight();

        float spentTime = minecraft.getCameraEntity().tickCount + minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float texturePos = Mth.frac(spentTime * 0.2F - Mth.floor(spentTime * 0.1F));

        poseStack.pushPose();
        poseStack.translate(baseX + 0.5, baseY, baseZ + 0.5);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(spentTime * 2.25F - 45.0F));

        float beamRadius = BeaconRenderer.SOLID_BEAM_RADIUS / 1.4142F;
        float beamMaxV = 1.0F - texturePos;
        float beamMinV = height * (0.5F / BeaconRenderer.SOLID_BEAM_RADIUS) + beamMaxV;
        int beamColor = waypoint.getUnifiedColor(1.0F);

        RenderType beamRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, false);
        VertexConsumer beamBuffer = bufferSource.getBuffer(beamRenderType);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -beamRadius : beamRadius;
            float z = (face < 2) ? -beamRadius : beamRadius;
            float x2 = (face < 2) ? -beamRadius : beamRadius;
            float z2 = (face == 1 || face == 2) ? -beamRadius : beamRadius;

            beamBuffer.addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            beamBuffer.addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);

        }
        bufferSource.endBatch(beamRenderType);

        poseStack.popPose();

        float glowRadius = BeaconRenderer.BEAM_GLOW_RADIUS;
        float glowMaxV = 1.0F - texturePos;
        float glowMinV = height + beamMaxV;
        int glowColor = waypoint.getUnifiedColor(0.125F);

        RenderType glowRenderType = RenderTypes.beaconBeam(BeaconRenderer.BEAM_LOCATION, true);
        VertexConsumer glowBuffer = bufferSource.getBuffer(glowRenderType);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -glowRadius : glowRadius;
            float z = (face < 2) ? -glowRadius : glowRadius;
            float x2 = (face < 2) ? -glowRadius : glowRadius;
            float z2 = (face == 1 || face == 2) ? -glowRadius : glowRadius;

            glowBuffer.addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            glowBuffer.addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);

        }
        bufferSource.endBatch(glowRenderType);

        poseStack.popPose();

    }

    private void renderSign(PoseStack poseStack, BufferSource bufferSource, Waypoint waypoint, TextureAtlas textureAtlas, boolean isPointedAt, boolean isHighlighted, double distance, double baseX, double baseY, double baseZ) {
        String mainLabel = waypoint.name;
        if (isHighlighted) {
            if (waypointManager.isCoordinateHighlight(waypoint)) {
                mainLabel = "X:" + waypoint.getX() + ", Y:" + waypoint.getY() + ", Z:" + waypoint.getZ();
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

        float scale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * options.waypointSignScale;
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(VoxelConstants.getMinecraft().getEntityRenderDispatcher().camera.xRot()));
        poseStack.scale(-scale, -scale, -scale);

        float alpha = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        float alphaBehindWall = alpha;
        if (!isPointedAt) {
            if (!waypoint.enabled && !isHighlighted) {
                alpha *= 0.3F;
            }
            alphaBehindWall *= 0.3F;
        }

        float width = 10.0F;
        float r = isHighlighted ? 1.0F : waypoint.red;
        float g = isHighlighted ? 0.0F : waypoint.green;
        float b = isHighlighted ? 0.0F : waypoint.blue;

        Sprite icon = isHighlighted ? textureAtlas.getAtlasSprite("marker/target") : textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite(WaypointManager.fallbackIconLocation);
        }

        RenderType renderType = VoxelMapRenderTypes.WAYPOINT_ICON_DEPTH_TEST.apply(icon.getIdentifier());
        VertexConsumer vertexIconDepthtest = bufferSource.getBuffer(renderType);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, alpha);
        vertexIconDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, alpha);
        vertexIconDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, alpha);
        vertexIconDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, alpha);
        bufferSource.endBatch(renderType);

        renderType = VoxelMapRenderTypes.WAYPOINT_ICON_NO_DEPTH_TEST.apply(icon.getIdentifier());
        VertexConsumer vertexIconNoDepthtest = bufferSource.getBuffer(renderType);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(r, g, b, alphaBehindWall);
        vertexIconNoDepthtest.addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(r, g, b, alphaBehindWall);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(r, g, b, alphaBehindWall);
        vertexIconNoDepthtest.addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(r, g, b, alphaBehindWall);
        bufferSource.endBatch(renderType);

        if (isPointedAt) {
            boolean moveLabelsDown = options.waypointNamesLocation == 2;
            String subLabel = "";
            if (options.waypointDistancesLocation != 0) {
                boolean shouldConvert = (options.waypointDistanceConversion == 1 && distance > 1000.0) || (options.waypointDistanceConversion == 2 && distance > 10000.0);
                if (shouldConvert) {
                    double converted = distance / 1000.0;
                    subLabel = (int) converted + "." + (int) ((converted - (int) converted) * 10) + "km";
                } else {
                    subLabel = (int) distance + "." + (int) ((distance - (int) distance) * 10) + "m";
                }
            }

            if (options.waypointNamesLocation == 0) {
                mainLabel = "";
            }

            if (!subLabel.isEmpty()) {
                if (mainLabel.isEmpty()) {
                    moveLabelsDown = options.waypointDistancesLocation == 2;
                    mainLabel = subLabel;
                    subLabel = "";
                } else if (options.waypointDistancesLocation == 1) {
                    mainLabel += " (" + subLabel + ")";
                    subLabel = "";
                }
            }

            boolean renderMainLabel = !mainLabel.isEmpty();
            boolean renderSubLabel = !subLabel.isEmpty();

            int halfWidthMainLabel = minecraft.font.width(mainLabel) / 2;
            int yPosMainLabel = moveLabelsDown ? 10 : (renderSubLabel ? -24 : -18);

            float subLabelScale = 0.75F;
            int halfWidthSubLabel = minecraft.font.width(subLabel) / 2;
            int yPosSubLabel = moveLabelsDown ? 26 : -20;

            // Render label backgrounds
            renderType = VoxelMapRenderTypes.WAYPOINT_TEXT_BACKGROUND;
            VertexConsumer vertexTextBackground = bufferSource.getBuffer(renderType);

            if (renderMainLabel) {
                vertexTextBackground.addVertex(poseStack.last(), -halfWidthMainLabel - 2, yPosMainLabel - 2, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), -halfWidthMainLabel - 2, yPosMainLabel + 9, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), halfWidthMainLabel + 2, yPosMainLabel + 9, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), halfWidthMainLabel + 2, yPosMainLabel - 2, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);

                vertexTextBackground.addVertex(poseStack.last(), -halfWidthMainLabel - 1, yPosMainLabel - 1, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), -halfWidthMainLabel - 1, yPosMainLabel + 8, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), halfWidthMainLabel + 1, yPosMainLabel + 8, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), halfWidthMainLabel + 1, yPosMainLabel - 1, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
            }

            if (renderSubLabel) {
                float left = (-halfWidthSubLabel - 2) * subLabelScale;
                float right = (halfWidthSubLabel + 2) * subLabelScale;
                float top = (yPosSubLabel - 2) * subLabelScale;
                float bottom = (yPosSubLabel + 9) * subLabelScale;
                vertexTextBackground.addVertex(poseStack.last(), left, top, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), left, bottom, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), right, bottom, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), right, top, 0.0F).setColor(waypoint.red, waypoint.green, waypoint.blue, 0.6F * alpha);

                left = (-halfWidthSubLabel - 1) * subLabelScale;
                right = (halfWidthSubLabel + 1) * subLabelScale;
                top = (yPosSubLabel - 1) * subLabelScale;
                bottom = (yPosSubLabel + 8) * subLabelScale;
                vertexTextBackground.addVertex(poseStack.last(), left, top, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), left, bottom, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), right, bottom, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
                vertexTextBackground.addVertex(poseStack.last(), right, top, 0.0F).setColor(0.0F, 0.0F, 0.0F, 0.15F * alpha);
            }

            bufferSource.endBatch(renderType);

            // Render labels
            int textColor = (int) (255.0F * alpha) << 24 | 0x00FFFFFF;

            if (renderMainLabel) {
                minecraft.font.drawInBatch(mainLabel, -halfWidthMainLabel, yPosMainLabel, textColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0x00000000, LightTexture.FULL_BRIGHT);
            }

            if (renderSubLabel) {
                poseStack.pushPose();
                poseStack.scale(subLabelScale, subLabelScale, 1.0F);
                minecraft.font.drawInBatch(subLabel, -halfWidthSubLabel, yPosSubLabel, textColor, false, poseStack.last().pose(), bufferSource, Font.DisplayMode.SEE_THROUGH, 0x00000000, LightTexture.FULL_BRIGHT);
                poseStack.popPose();
            }

            bufferSource.endLastBatch();
        }
        poseStack.popPose();
    }

    public static class RenderableWaypoint implements Comparable<RenderableWaypoint> {
        private final Waypoint waypoint;
        private final boolean highlighted;

        private float offset;

        public RenderableWaypoint(Waypoint waypoint, boolean highlighted) {
            this.waypoint = waypoint;
            this.highlighted = highlighted;
        }

        public Waypoint getWaypoint() {
            return waypoint;
        }

        public boolean isHighlighted() {
            return highlighted;
        }

        public float getOffset() {
            return offset;
        }

        public void setOffset(float offset) {
            this.offset = offset;
        }

        public int compareTo(RenderableWaypoint o) {
            boolean skip1 = offset == INVALID_OFFSET || (!waypoint.enabled && !highlighted) || !waypoint.inWorld || !waypoint.inDimension;
            boolean skip2 = o.offset == INVALID_OFFSET || (!o.waypoint.enabled && !o.highlighted) || !o.waypoint.inWorld || !o.waypoint.inDimension;

            if (skip1 && !skip2) return 1;
            if (!skip1 && skip2) return -1;

            return Float.compare(offset, o.offset);
        }
    }
}
