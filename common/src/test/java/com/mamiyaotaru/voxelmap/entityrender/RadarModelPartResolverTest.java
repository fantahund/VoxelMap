package com.mamiyaotaru.voxelmap.entityrender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class RadarModelPartResolverTest {
    @Test
    void prefersSolidHead2OverDegenerateEyeGeometry() {
        ModelPart eyes = partWithCube(4.0F, 2.0F, 0.0F);
        ModelPart head = part(Map.of("eyes", eyes));
        ModelPart head2 = partWithCube(8.0F, 8.0F, 8.0F);
        ModelPart root = part(linkedChildren("head", head, "head2", head2));

        List<RadarModelPartResolver.Selection> results = RadarModelPartResolver.resolveHeadCandidates(root);

        assertEquals(1, results.size());
        assertSame(head2, results.getFirst().part());
    }

    @Test
    void acceptsRecursiveVillagerNoseGeometry() {
        ModelPart nose = partWithCube(2.0F, 4.0F, 2.0F);
        ModelPart head = part(Map.of("nose", nose));
        ModelPart root = part(Map.of("head", head));

        RadarModelPartResolver.Selection result = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();

        assertSame(head, result.part());
        assertEquals("root/head", result.path());
    }

    @Test
    void geometrySizeWinsBeforeHeadNamePriority() {
        ModelPart head = partWithCube(2.0F, 2.0F, 2.0F);
        ModelPart head2 = partWithCube(4.0F, 4.0F, 4.0F);
        ModelPart root = part(linkedChildren("head", head, "head2", head2));

        List<RadarModelPartResolver.Selection> results = RadarModelPartResolver.resolveHeadCandidates(root);

        assertSame(head2, results.getFirst().part());
        assertSame(head, results.get(1).part());
    }

    @Test
    void usesNameOrderAsTieBreaker() {
        ModelPart forehead = partWithCube(2.0F, 2.0F, 2.0F);
        ModelPart headCustom = partWithCube(2.0F, 2.0F, 2.0F);
        ModelPart head2 = partWithCube(2.0F, 2.0F, 2.0F);
        LinkedHashMap<String, ModelPart> children = new LinkedHashMap<>();
        children.put("forehead", forehead);
        children.put("head_custom", headCustom);
        children.put("head2", head2);

        List<RadarModelPartResolver.Selection> results = RadarModelPartResolver.resolveHeadCandidates(part(children));

        assertSame(head2, results.get(0).part());
        assertSame(headCustom, results.get(1).part());
        assertSame(forehead, results.get(2).part());
    }

    @Test
    void prefersPrincipalHeadOverContainerWithFullSizeAnimatedSibling() {
        ModelPart head2 = partWithCube(8.0F, 8.0F, 8.0F);
        ModelPart jaw = partWithCube(8.0F, 8.0F, 8.0F);
        ModelPart headwear = part(linkedChildren("head2", head2, "jaw", jaw));
        ModelPart root = part(Map.of("headwear", headwear));

        List<RadarModelPartResolver.Selection> results = RadarModelPartResolver.resolveHeadCandidates(root);

        assertSame(head2, results.get(0).part());
        assertSame(headwear, results.get(1).part());
        assertTrue(results.get(0).includeChildren());
    }

    @Test
    void keepsHeadContainerFirstWhenParallelGeometryIsOnlyADetail() {
        ModelPart mainHead = partWithCube(8.0F, 8.0F, 8.0F);
        ModelPart mouth = partWithCube(2.0F, 2.0F, 2.0F);
        ModelPart head = part(linkedChildren("head_main", mainHead, "mouth", mouth));
        ModelPart root = part(Map.of("head", head));

        List<RadarModelPartResolver.Selection> results = RadarModelPartResolver.resolveHeadCandidates(root);

        assertSame(head, results.get(0).part());
        assertSame(mainHead, results.get(1).part());
    }

    @Test
    void returnsNoHeadCandidateSoCallerCanTryRootLast() {
        ModelPart root = part(linkedChildren("head", emptyPart(), "body", partWithCube(2.0F, 2.0F, 2.0F)));

        assertTrue(RadarModelPartResolver.resolveHeadCandidates(root).isEmpty());
        assertSame(root, RadarModelPartResolver.root(root).part());
    }

    @Test
    void orientationPathEndsAtLargestSolidCube() {
        ModelPart ear = partWithCube(1.0F, 1.0F, 1.0F);
        ModelPart face = partWithCube(8.0F, 8.0F, 8.0F);
        ModelPart head = part(linkedChildren("ear", ear, "face", face));
        ModelPart body = part(Map.of("head2", head));
        ModelPart root = part(Map.of("body", body));
        RadarModelPartResolver.Selection selection = RadarModelPartResolver.resolveHeadCandidates(root).getFirst();

        assertEquals(List.of(root, body, head, face), RadarModelPartResolver.orientationPath(selection));
    }

    @Test
    void findsDirectGhastBodyInsideAnEmfWrapperWithoutTentacleChildren() {
        ModelPart tentacle = partWithCube(2.0F, 10.0F, 2.0F);
        ModelPart customBody = new ModelPart(partWithCube(16.0F, 16.0F, 16.0F).cubes, Map.of("tentacle_1", tentacle));
        ModelPart bodyWrapper = part(Map.of("body", customBody));
        ModelPart root = part(Map.of("body", bodyWrapper));

        RadarModelPartResolver.Selection directBody = RadarModelPartResolver.largestDirectGeometry(
                RadarModelPartResolver.find(root, "body").orElseThrow()).orElseThrow();

        assertSame(customBody, directBody.part());
        assertFalse(directBody.includeChildren());
        assertEquals(4096.0D, directBody.geometryScore());
        assertEquals("root/body/body", directBody.path());
        assertEquals(List.of(root, bodyWrapper, customBody), RadarModelPartResolver.orientationPath(directBody));
    }

    private static ModelPart emptyPart() {
        return new ModelPart(List.of(), Map.of());
    }

    private static ModelPart part(Map<String, ModelPart> children) {
        return new ModelPart(List.of(), children);
    }

    private static ModelPart partWithCube(float width, float height, float depth) {
        ModelPart.Cube cube = new ModelPart.Cube(0, 0, 0.0F, 0.0F, 0.0F, width, height, depth,
                0.0F, 0.0F, 0.0F, false, 64.0F, 64.0F, Set.of(Direction.NORTH));
        return new ModelPart(List.of(cube), Map.of());
    }

    private static Map<String, ModelPart> linkedChildren(String firstName, ModelPart firstPart, String secondName, ModelPart secondPart) {
        LinkedHashMap<String, ModelPart> children = new LinkedHashMap<>();
        children.put(firstName, firstPart);
        children.put(secondName, secondPart);
        return children;
    }
}
