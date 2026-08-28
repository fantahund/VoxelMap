package com.mamiyaotaru.voxelmap.entityrender;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Matrix3f;
import org.joml.Vector3f;

final class RadarModelPose {
    private static final float VERTICAL_FORWARD_THRESHOLD = 0.9F;
    private final List<PartState> states;

    private RadarModelPose(List<PartState> states) {
        this.states = List.copyOf(states);
    }

    static RadarModelPose capture(ModelPart root) {
        ArrayList<PartState> states = new ArrayList<>();
        capture(root, new IdentityHashMap<>(), states);
        return new RadarModelPose(states);
    }

    void apply() {
        states.forEach(PartState::apply);
    }

    static void resetToInitial(ModelPart root) {
        resetToInitial(root, new IdentityHashMap<>());
    }

    static void normalizeOrientations(List<RadarModelPartResolver.Selection> selections) {
        LinkedHashSet<ModelPart> anchors = new LinkedHashSet<>();
        selections.forEach(selection -> anchors.addAll(RadarModelPartResolver.orientationPath(selection)));

        for (ModelPart part : anchors) {
            PartPose initialPose = part.getInitialPose();
            part.xRot = initialPose.xRot();
            part.yRot = initialPose.yRot();
            part.zRot = initialPose.zRot();
        }

        selections.forEach(RadarModelPose::compensateVerticalHeadOrientation);
    }

    private static void compensateVerticalHeadOrientation(RadarModelPartResolver.Selection selection) {
        if (!selection.name().toLowerCase(Locale.ROOT).contains("head")) {
            return;
        }

        List<ModelPart> path = RadarModelPartResolver.orientationPath(selection);
        int headIndex = indexOfIdentity(path, selection.part());
        if (headIndex < 0) {
            return;
        }

        Matrix3f worldRotation = composeRotations(path, 0, path.size());
        Vector3f forward = worldRotation.transform(new Vector3f(0.0F, 0.0F, -1.0F));
        if (Math.abs(forward.y) < VERTICAL_FORWARD_THRESHOLD) {
            return;
        }

        // Fresh Animations can place a custom head below a vanilla quadruped body whose initial pitch is 90 degrees.
        // Compensate at the head so detail rotations below it remain relative to the complete, front-facing head.
        Matrix3f parentRotation = composeRotations(path, 0, headIndex);
        Matrix3f geometryRotation = composeRotations(path, headIndex + 1, path.size());
        Matrix3f correctedHeadRotation = parentRotation.invert().mul(geometryRotation.invert());
        Vector3f correctedAngles = correctedHeadRotation.getEulerAnglesZYX(new Vector3f());
        selection.part().setRotation(correctedAngles.x, correctedAngles.y, correctedAngles.z);
    }

    private static Matrix3f composeRotations(List<ModelPart> path, int fromIndex, int toIndex) {
        Matrix3f rotation = new Matrix3f();
        for (int index = fromIndex; index < toIndex; index++) {
            ModelPart part = path.get(index);
            rotation.rotateZYX(part.zRot, part.yRot, part.xRot);
        }
        return rotation;
    }

    private static int indexOfIdentity(List<ModelPart> parts, ModelPart searchedPart) {
        for (int index = 0; index < parts.size(); index++) {
            if (parts.get(index) == searchedPart) {
                return index;
            }
        }
        return -1;
    }

    private static void capture(ModelPart part, IdentityHashMap<ModelPart, Boolean> seen, List<PartState> states) {
        if (seen.put(part, Boolean.TRUE) != null) {
            return;
        }

        states.add(new PartState(part, part.x, part.y, part.z, part.xRot, part.yRot, part.zRot,
                part.xScale, part.yScale, part.zScale, part.visible, part.skipDraw));
        part.children.values().forEach(child -> capture(child, seen, states));
    }

    private static void resetToInitial(ModelPart part, IdentityHashMap<ModelPart, Boolean> seen) {
        if (seen.put(part, Boolean.TRUE) != null) {
            return;
        }

        part.resetPose();
        part.children.values().forEach(child -> resetToInitial(child, seen));
    }

    private record PartState(ModelPart part, float x, float y, float z, float xRot, float yRot, float zRot,
                             float xScale, float yScale, float zScale, boolean visible, boolean skipDraw) {
        void apply() {
            part.x = x;
            part.y = y;
            part.z = z;
            part.xRot = xRot;
            part.yRot = yRot;
            part.zRot = zRot;
            part.xScale = xScale;
            part.yScale = yScale;
            part.zScale = zScale;
            part.visible = visible;
            part.skipDraw = skipDraw;
        }
    }
}
