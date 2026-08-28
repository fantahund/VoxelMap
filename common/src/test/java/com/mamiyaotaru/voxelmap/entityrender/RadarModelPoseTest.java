package com.mamiyaotaru.voxelmap.entityrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class RadarModelPoseTest {
    @Test
    void keepsAnimatedAssemblyButNeutralizesOnlyOrientationAnchors() {
        ModelPart ear = partWithCube(1.0F);
        ModelPart head = new ModelPart(partWithCube(8.0F).cubes, Map.of("ear", ear));
        ModelPart body = new ModelPart(List.of(), Map.of("head2", head));
        ModelPart root = new ModelPart(List.of(), Map.of("body", body));

        root.setInitialPose(PartPose.rotation(0.1F, 0.2F, 0.3F));
        body.setInitialPose(PartPose.rotation(0.4F, 0.5F, 0.6F));
        head.setInitialPose(PartPose.rotation(0.7F, 0.8F, 0.9F));
        ear.setInitialPose(PartPose.rotation(1.0F, 1.1F, 1.2F));

        root.setRotation(2.1F, 2.2F, 2.3F);
        body.setRotation(2.4F, 2.5F, 2.6F);
        head.setRotation(2.7F, 2.8F, 2.9F);
        ear.setRotation(3.0F, 3.1F, 3.2F);
        head.setPos(11.0F, 12.0F, 13.0F);
        head.xScale = 1.4F;
        head.yScale = 1.5F;
        head.zScale = 1.6F;

        RadarModelPartResolver.Selection selection = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();
        RadarModelPose.normalizeOrientations(List.of(selection));
        RadarModelPose prepared = RadarModelPose.capture(root);

        head.setPos(0.0F, 0.0F, 0.0F);
        head.xScale = head.yScale = head.zScale = 1.0F;
        ear.setRotation(0.0F, 0.0F, 0.0F);
        prepared.apply();

        assertEquals(11.0F, head.x);
        assertEquals(12.0F, head.y);
        assertEquals(13.0F, head.z);
        assertEquals(1.4F, head.xScale);
        assertEquals(1.5F, head.yScale);
        assertEquals(1.6F, head.zScale);
        assertEquals(0.1F, root.xRot);
        assertEquals(0.4F, body.xRot);
        assertEquals(0.7F, head.xRot);
        assertEquals(3.0F, ear.xRot);
    }

    @Test
    void restoresVisibilityAndSkipDraw() {
        ModelPart root = partWithCube(2.0F);
        root.visible = false;
        root.skipDraw = true;
        RadarModelPose pose = RadarModelPose.capture(root);

        root.visible = true;
        root.skipDraw = false;
        pose.apply();

        assertFalse(root.visible);
        assertTrue(root.skipDraw);
    }

    @Test
    void resetsPartsAddedAfterModelConstructionToTheirInitialPose() {
        HashMap<String, ModelPart> children = new HashMap<>();
        ModelPart root = new ModelPart(List.of(), children);
        ModelPart lazyCustomPart = partWithCube(4.0F);
        lazyCustomPart.setInitialPose(PartPose.offsetAndRotation(1.0F, 2.0F, 3.0F, 0.1F, 0.2F, 0.3F));
        children.put("fresh_body", lazyCustomPart);
        lazyCustomPart.setPos(10.0F, 20.0F, 30.0F);
        lazyCustomPart.setRotation(1.1F, 1.2F, 1.3F);

        RadarModelPose.resetToInitial(root);

        assertEquals(1.0F, lazyCustomPart.x);
        assertEquals(2.0F, lazyCustomPart.y);
        assertEquals(3.0F, lazyCustomPart.z);
        assertEquals(0.1F, lazyCustomPart.xRot);
        assertEquals(0.2F, lazyCustomPart.yRot);
        assertEquals(0.3F, lazyCustomPart.zRot);
    }

    @Test
    void compensatesVanillaQuadrupedBodyRotationForNestedFreshHead() {
        ModelPart ear = partWithCube(1.0F);
        ModelPart head = new ModelPart(partWithCube(8.0F).cubes, Map.of("ear", ear));
        ModelPart customBody = new ModelPart(List.of(), Map.of("head2", head));
        ModelPart vanillaBody = new ModelPart(List.of(), Map.of("body", customBody));
        ModelPart root = new ModelPart(List.of(), Map.of("body", vanillaBody));

        vanillaBody.setInitialPose(PartPose.rotation((float) (Math.PI / 2.0), 0.0F, 0.0F));
        customBody.setInitialPose(PartPose.ZERO);
        head.setInitialPose(PartPose.ZERO);
        ear.setInitialPose(PartPose.ZERO);

        vanillaBody.xRot = 0.2F;
        customBody.xRot = 0.3F;
        head.xRot = -0.5F;
        ear.xRot = 0.4F;

        RadarModelPartResolver.Selection selection = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();
        RadarModelPose.normalizeOrientations(List.of(selection));

        assertEquals((float) (Math.PI / 2.0), vanillaBody.xRot);
        assertEquals((float) (-Math.PI / 2.0), head.xRot, 1.0E-5F);
        assertEquals(0.4F, ear.xRot);
    }

    @Test
    void leavesAlreadyFrontFacingFreshHeadUnchanged() {
        ModelPart head = partWithCube(8.0F);
        ModelPart body = new ModelPart(List.of(), Map.of("head2", head));
        ModelPart root = new ModelPart(List.of(), Map.of("body", body));
        body.setInitialPose(PartPose.rotation(0.2F, 0.0F, 0.0F));
        head.setInitialPose(PartPose.rotation(-0.2F, 0.0F, 0.0F));

        RadarModelPartResolver.Selection selection = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();
        RadarModelPose.normalizeOrientations(List.of(selection));

        assertEquals(0.2F, body.xRot);
        assertEquals(-0.2F, head.xRot);
    }

    @Test
    void includesNestedMainGeometryRotationInHeadCompensation() {
        ModelPart face = partWithCube(8.0F);
        ModelPart ear = partWithCube(1.0F);
        ModelPart head = new ModelPart(List.of(), Map.of("face", face, "ear", ear));
        ModelPart body = new ModelPart(List.of(), Map.of("head2", head));
        ModelPart root = new ModelPart(List.of(), Map.of("body", body));
        body.setInitialPose(PartPose.rotation((float) (Math.PI / 2.0), 0.0F, 0.0F));
        face.setInitialPose(PartPose.rotation(0.25F, 0.0F, 0.0F));
        ear.xRot = 0.4F;

        RadarModelPartResolver.Selection selection = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();
        RadarModelPose.normalizeOrientations(List.of(selection));

        assertEquals((float) (-Math.PI / 2.0) - 0.25F, head.xRot, 1.0E-5F);
        assertEquals(0.25F, face.xRot);
        assertEquals(0.4F, ear.xRot);
    }

    @Test
    void doesNotApplyHeadCompensationToVerticalFullModelSelection() {
        ModelPart body = partWithCube(8.0F);
        body.setInitialPose(PartPose.rotation((float) (Math.PI / 2.0), 0.0F, 0.0F));
        RadarModelPartResolver.Selection selection = RadarModelPartResolver.root(body);

        RadarModelPose.normalizeOrientations(List.of(selection));

        assertEquals((float) (Math.PI / 2.0), body.xRot);
    }

    private static ModelPart partWithCube(float size) {
        ModelPart.Cube cube = new ModelPart.Cube(0, 0, 0.0F, 0.0F, 0.0F, size, size, size,
                0.0F, 0.0F, 0.0F, false, 64.0F, 64.0F, Set.of(Direction.NORTH));
        return new ModelPart(List.of(cube), Map.of());
    }
}
