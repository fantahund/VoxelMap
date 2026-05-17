package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.options.containers.MapOptions;
import com.mamiyaotaru.voxelmap.options.containers.WaypointOptions;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumMinimap;
import com.mamiyaotaru.voxelmap.options.enums.OptionEnumWaypoint;
import com.mamiyaotaru.voxelmap.textures.Sprite;
import com.mamiyaotaru.voxelmap.textures.TextureAtlas;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Collections;

public class WaypointContainer {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final MapOptions mapOptions;
    private final WaypointOptions options;
    private final WaypointManager waypointManager;

    private final PoseStack poseStack = new PoseStack();
    private final ArrayList<RenderableWaypoint> renderables = new ArrayList<>();
    private static final float INVALID_OFFSET = -1.0F;

    public WaypointContainer() {
        this.mapOptions = VoxelConstants.getVoxelMapInstance().getMapOptions();
        this.options = VoxelConstants.getVoxelMapInstance().getWaypointOptions();
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

    private boolean isBeaconsShown() {
        return mapOptions.inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.BEACONS || mapOptions.inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.BOTH;
    }

    private boolean isSignsShown() {
        return mapOptions.inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.SIGNS || mapOptions.inGameWaypoints.get() == OptionEnumMinimap.InGameWaypoints.BOTH;
    }

    public void renderWaypoints(float partialTick, Matrix4f matrix, Camera camera) {
        if (waypointManager == null) return;
        if (renderables.isEmpty()) return;

        poseStack.setIdentity();
        poseStack.mulPose(matrix);

        if (isBeaconsShown()) {
            renderWaypointBeams(partialTick, poseStack, camera);
        }
        if (isSignsShown()) {
            renderWaypointSigns(partialTick, poseStack, camera);
        }
    }

    public void renderWaypointBeams(float partialTick, PoseStack poseStack, Camera camera) {
        Vec3 cameraPos = camera.position();
        double bottomOfWorld = VoxelConstants.getPlayer().level().getMinY() - cameraPos.y;

        for (RenderableWaypoint renderable : renderables) {
            Waypoint waypoint = renderable.getWaypoint();
            boolean isEffectivelyActive = waypoint.isActive() || renderable.isHighlighted();

            if (!isEffectivelyActive) continue;

            int x = waypoint.getX();
            int z = waypoint.getZ();
            double distance = Math.sqrt(waypoint.getDistanceSqToCamera(camera));

            renderBeam(poseStack, waypoint, distance, x - cameraPos.x, bottomOfWorld, z - cameraPos.z);
        }
    }

    public void renderWaypointSigns(float partialTick, PoseStack poseStack, Camera camera) {
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

            boolean isOutOfRange = options.maxDistance.get() <= 10000 && distance >= options.maxDistance.get();
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
                renderSign(poseStack, waypoint, textureAtlas, isPointedAt, false, distance, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
            }
            renderSign(poseStack, waypoint, textureAtlas, isPointedAt, isHighlighted, distance, x - cameraPos.x, y - cameraPos.y + 1.12, z - cameraPos.z);
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
        double size = Math.max(Math.sin(angle) * distance, 0.5) * options.signScale.get();

        if (centerOffset <= size * size) {
            return centerOffset;
        }

        return INVALID_OFFSET;
    }

    /**
     * Edited from {@link net.minecraft.client.renderer.blockentity.BeaconRenderer}
     */
    private void renderBeam(PoseStack poseStack, Waypoint waypoint, double distance, double baseX, double baseY, double baseZ) {
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

        RenderUtils.beginBatch(RenderPipelines.BEACON_BEAM_OPAQUE, BeaconRenderer.BEAM_LOCATION);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -beamRadius : beamRadius;
            float z = (face < 2) ? -beamRadius : beamRadius;
            float x2 = (face < 2) ? -beamRadius : beamRadius;
            float z2 = (face == 1 || face == 2) ? -beamRadius : beamRadius;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMaxV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, beamMinV).setColor(beamColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);

        }
        RenderUtils.endBatch();

        poseStack.popPose();

        float glowRadius = BeaconRenderer.BEAM_GLOW_RADIUS;
        float glowMaxV = 1.0F - texturePos;
        float glowMinV = height + beamMaxV;
        int glowColor = waypoint.getUnifiedColor(0.125F);

        RenderUtils.beginBatch(RenderPipelines.BEACON_BEAM_TRANSLUCENT, BeaconRenderer.BEAM_LOCATION);
        for (int face = 0; face < 4; ++face) {
            float x = (face == 0 || face == 3) ? -glowRadius : glowRadius;
            float z = (face < 2) ? -glowRadius : glowRadius;
            float x2 = (face < 2) ? -glowRadius : glowRadius;
            float z2 = (face == 1 || face == 2) ? -glowRadius : glowRadius;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x, height, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x, 0.0F, z).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(1.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x2, 0.0F, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMaxV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x2, height, z2).setNormal(poseStack.last(), 0.0F, 1.0F, 0.0F).setUv(0.0F, glowMinV).setColor(glowColor).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT);

        }
        RenderUtils.endBatch();

        poseStack.popPose();

    }

    private void renderSign(PoseStack poseStack, Waypoint waypoint, TextureAtlas textureAtlas, boolean isPointedAt, boolean isHighlighted, double distance, double baseX, double baseY, double baseZ) {
        String mainLabel = waypoint.name;
        if (isHighlighted) {
            mainLabel = waypointManager.isCoordinateHighlight(waypoint) ? "X:" + waypoint.getX() + ", Y:" + waypoint.getY() + ", Z:" + waypoint.getZ() : "";
        }
        boolean hideLabels = mainLabel.isEmpty();

        double maxDistance = minecraft.gameRenderer.getDepthFar() - 8.0;
        double adjustedDistance = distance;
        if (distance > maxDistance) {
            baseX = baseX / distance * maxDistance;
            baseY = baseY / distance * maxDistance;
            baseZ = baseZ / distance * maxDistance;
            adjustedDistance = maxDistance;
        }

        float scale = ((float) adjustedDistance * 0.1F + 1.0F) * 0.0266F * options.signScale.get();
        poseStack.pushPose();
        poseStack.translate((float) baseX + 0.5F, (float) baseY + 0.5F, (float) baseZ + 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-minecraft.getEntityRenderDispatcher().camera.yRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees(minecraft.getEntityRenderDispatcher().camera.xRot()));
        poseStack.scale(-scale, -scale, -scale);

        float fade = distance > 5.0 ? 1.0F : (float) distance / 5.0F;
        if (!waypoint.enabled && !isHighlighted) {
            fade *= 0.3F;
        }
        boolean focused = options.highlightFocused.get() && isPointedAt;

        float width = 10.0F;
        float r = isHighlighted ? 1.0F : waypoint.red;
        float g = isHighlighted ? 0.0F : waypoint.green;
        float b = isHighlighted ? 0.0F : waypoint.blue;

        Sprite icon = isHighlighted ? textureAtlas.getAtlasSprite("marker/target") : textureAtlas.getAtlasSprite("selectable/" + waypoint.imageSuffix);
        if (icon == textureAtlas.getMissingImage()) {
            icon = textureAtlas.getAtlasSprite(WaypointManager.FALLBACK_ICON_NAME);
        }

        int iconColorDepthTest = ARGB.colorFromFloat(fade, r, g, b);
        RenderUtils.beginBatch(getIconPipeline(focused, true), icon.getIdentifier());
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(iconColorDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(iconColorDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(iconColorDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(iconColorDepthTest);
        RenderUtils.endBatch();

        int iconColorNoDepthTest = ARGB.colorFromFloat(0.3F * fade, r, g, b);
        RenderUtils.beginBatch(getIconPipeline(focused, false), icon.getIdentifier());
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), -width, -width, 0.0F).setUv(icon.getMinU(), icon.getMinV()).setColor(iconColorNoDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), -width, width, 0.0F).setUv(icon.getMinU(), icon.getMaxV()).setColor(iconColorNoDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), width, width, 0.0F).setUv(icon.getMaxU(), icon.getMaxV()).setColor(iconColorNoDepthTest);
        RenderUtils.vertexBuffer().addVertex(poseStack.last(), width, -width, 0.0F).setUv(icon.getMaxU(), icon.getMinV()).setColor(iconColorNoDepthTest);
        RenderUtils.endBatch();

        if (isPointedAt && !hideLabels && options.labelStyle.get() != OptionEnumWaypoint.LabelStyle.OFF) {
            boolean convertFromOneKilometer = options.unitConversion.get() == OptionEnumWaypoint.UnitConversion.FROM_1000M && distance > 1000.0;
            boolean convertFromTenKilometers = options.unitConversion.get() == OptionEnumWaypoint.UnitConversion.FROM_10000M && distance > 10000.0;

            String distanceLabel;
            if (convertFromOneKilometer || convertFromTenKilometers) {
                distanceLabel = I18n.get("minimap.waypoints.distance.kilometers", getDistanceString(distance / 1000.0));
            } else {
                distanceLabel = I18n.get("minimap.waypoints.distance.meters", getDistanceString(distance));
            }

            boolean showSubLabel = options.labelStyle.get() == OptionEnumWaypoint.LabelStyle.DEFAULT;
            boolean moveLabelDown = options.labelStyle.get() != OptionEnumWaypoint.LabelStyle.CLASSIC_TOP;

            String subLabel = showSubLabel ? distanceLabel : "";
            if (!showSubLabel && !distanceLabel.isEmpty()) {
                mainLabel += " (" + distanceLabel + ")";
            }

            int subLabelY = 26;
            int mainLabelY = moveLabelDown ? 10 : -18;

            int backgroundColor = ARGB.colorFromFloat(0.6F * fade, r, g, b);
            int foregroundColor = ARGB.colorFromFloat(0.15F * fade, 0.0F, 0.0F, 0.0F);
            renderLabelBackgrounds(poseStack, getLabelPipeline(focused, true), mainLabel, subLabel, mainLabelY, subLabelY, backgroundColor, foregroundColor);

            backgroundColor = ARGB.colorFromFloat(0.15F * fade, r, g, b);
            foregroundColor = ARGB.colorFromFloat(0.15F * fade, 0.0F, 0.0F, 0.0F);
            renderLabelBackgrounds(poseStack, getLabelPipeline(focused, false), mainLabel, subLabel, mainLabelY, subLabelY, backgroundColor, foregroundColor);

            int textColor = (int) (255.0F * fade) << 24 | 0x00CCCCCC;
            renderLabels(poseStack, Font.DisplayMode.SEE_THROUGH, mainLabel, subLabel, mainLabelY, subLabelY, textColor);
        }
        poseStack.popPose();
    }


    private RenderPipeline getIconPipeline(boolean focused, boolean occluded) {
        return !focused && occluded ? VoxelMapPipelines.WAYPOINT_ICON_DEPTH_TEST : VoxelMapPipelines.WAYPOINT_ICON_NO_DEPTH_TEST;
    }

    private RenderPipeline getLabelPipeline(boolean focused, boolean occluded) {
        return !focused && occluded ? VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND_DEPTH_TEST : VoxelMapPipelines.WAYPOINT_TEXT_BACKGROUND_NO_DEPTH_TEST;
    }

    private String getDistanceString(double distance) {
        long roundDist = Math.round(distance * 10.0);
        return (roundDist / 10) + "." + (roundDist % 10);
    }

    private void renderLabels(PoseStack poseStack, Font.DisplayMode displayMode, String mainLabel, String subLabel, int mainLabelY, int subLabelY, int color) {
        if (!mainLabel.isEmpty()) {
            float halfWidth = minecraft.font.width(mainLabel) / 2.0F;

            minecraft.font.drawInBatch(mainLabel, -halfWidth, mainLabelY, color, false, poseStack.last().pose(), minecraft.renderBuffers().bufferSource(), displayMode, 0x00000000, LightTexture.FULL_BRIGHT);
        }

        if (!subLabel.isEmpty()) {
            float halfWidth = minecraft.font.width(subLabel) / 2.0F;
            float scale = 0.75F;

            poseStack.pushPose();
            poseStack.scale(scale, scale, 1.0F);
            minecraft.font.drawInBatch(subLabel, -halfWidth, subLabelY, color, false, poseStack.last().pose(), minecraft.renderBuffers().bufferSource(), displayMode, 0x00000000, LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        minecraft.renderBuffers().bufferSource().endLastBatch();
    }

    private void renderLabelBackgrounds(PoseStack poseStack, RenderPipeline pipeline, String mainLabel, String subLabel, int mainLabelY, int subLabelY, int color1, int color2) {
        RenderUtils.beginBatch(pipeline, TextureSetup.noTexture());

        if (!mainLabel.isEmpty()) {
            float halfWidth = minecraft.font.width(mainLabel) / 2.0F;

            float x00 = -halfWidth - 2.0F;
            float x01 = halfWidth + 2.0F;
            float y00 = mainLabelY - 2.0F;
            float y01 = mainLabelY + 9.0F;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x00, y00, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x00, y01, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x01, y01, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x01, y00, 0.0F).setColor(color1);

            float x10 = -halfWidth - 1.0F;
            float x11 = halfWidth + 1.0F;
            float y10 = mainLabelY - 1.0F;
            float y11 = mainLabelY + 8.0F;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x10, y10, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x10, y11, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x11, y11, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x11, y10, 0.0F).setColor(color2);
        }

        if (!subLabel.isEmpty()) {
            float halfWidth = minecraft.font.width(subLabel) / 2.0F;
            float scale = 0.75F;

            float x00 = (-halfWidth - 2) * scale;
            float x01 = (halfWidth + 2) * scale;
            float y00 = (subLabelY - 2) * scale;
            float y01 = (subLabelY + 9) * scale;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x00, y00, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x00, y01, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x01, y01, 0.0F).setColor(color1);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x01, y00, 0.0F).setColor(color1);

            float x10 = (-halfWidth - 1) * scale;
            float x11 = (halfWidth + 1) * scale;
            float y10 = (subLabelY - 1) * scale;
            float y11 = (subLabelY + 8) * scale;

            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x10, y10, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x10, y11, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x11, y11, 0.0F).setColor(color2);
            RenderUtils.vertexBuffer().addVertex(poseStack.last(), x11, y10, 0.0F).setColor(color2);
        }

        RenderUtils.endBatch();
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
