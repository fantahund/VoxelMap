package com.mamiyaotaru.voxelmap.entityrender;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.model.geom.ModelPart;

final class RadarModelPartResolver {
    private static final float MIN_CUBE_SIZE = 1.0E-4F;
    private static final double LARGE_PARALLEL_GEOMETRY_RATIO = 0.5D;

    private RadarModelPartResolver() {
    }

    static List<Selection> resolveHeadCandidates(ModelPart root) {
        IdentityHashMap<ModelPart, Boolean> seen = new IdentityHashMap<>();

        return collectParts(root).stream()
                .filter(selection -> isHeadName(selection.name()))
                .filter(selection -> selection.geometryScore() > 0.0D)
                .filter(selection -> seen.put(selection.part(), Boolean.TRUE) == null)
                .sorted(headCandidateComparator())
                .toList();
    }

    private static Comparator<Selection> headCandidateComparator() {
        return Comparator.<Selection>comparingDouble(selection -> largestCubeVolumeRecursive(selection.part())).reversed()
                .thenComparing(RadarModelPartResolver::hasLargeParallelGeometry)
                .thenComparing(Comparator.comparingDouble(Selection::geometryScore).reversed())
                .thenComparingInt(selection -> namePriority(selection.name()));
    }

    private static boolean hasLargeParallelGeometry(Selection selection) {
        NamedPart principalHead = largestNestedHead(selection.part());
        if (principalHead == null) {
            return false;
        }

        double containerLargestCube = largestCubeVolumeRecursive(selection.part());
        if (principalHead.largestCubeVolume() < containerLargestCube) {
            return false;
        }

        double parallelLargestCube = largestCubeVolumeExcluding(selection.part(), principalHead.part());

        /*
         * CEM packs sometimes put a solid main head and an independently animated, equally large overlay in a
         * geometry-less head container. Fresh Animations' Enderman is one example: headwear contains head2 and a
         * second full-size jaw cube. Ranking by summed subtree volume selected the container and rendered the jaw as
         * an apparent second head. Small parallel branches are ordinary details and stay included; only a branch with
         * at least half the main cube's volume demotes the container behind its principal head. The container remains
         * in the candidate list as the next transparent-image fallback.
         */
        return parallelLargestCube >= principalHead.largestCubeVolume() * LARGE_PARALLEL_GEOMETRY_RATIO;
    }

    private static NamedPart largestNestedHead(ModelPart part) {
        NamedPart largest = null;
        for (var entry : part.children.entrySet()) {
            ModelPart child = entry.getValue();
            if (isHeadName(entry.getKey()) && geometryScore(child) > 0.0D) {
                NamedPart candidate = new NamedPart(entry.getKey(), child,
                        largestCubeVolumeRecursive(child), geometryScore(child));
                if (isBetterPrincipalHead(candidate, largest)) {
                    largest = candidate;
                }
            }

            NamedPart descendant = largestNestedHead(child);
            if (isBetterPrincipalHead(descendant, largest)) {
                largest = descendant;
            }
        }
        return largest;
    }

    private static boolean isBetterPrincipalHead(NamedPart candidate, NamedPart current) {
        if (candidate == null) {
            return false;
        }
        if (current == null || candidate.largestCubeVolume() != current.largestCubeVolume()) {
            return current == null || candidate.largestCubeVolume() > current.largestCubeVolume();
        }
        if (candidate.geometryScore() != current.geometryScore()) {
            return candidate.geometryScore() > current.geometryScore();
        }
        return namePriority(candidate.name()) < namePriority(current.name());
    }

    private static double largestCubeVolumeExcluding(ModelPart part, ModelPart excludedSubtree) {
        if (part == excludedSubtree) {
            return 0.0D;
        }

        double largest = largestCubeVolume(part);
        for (ModelPart child : part.children.values()) {
            largest = Math.max(largest, largestCubeVolumeExcluding(child, excludedSubtree));
        }
        return largest;
    }

    static Optional<Selection> find(ModelPart root, String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return collectParts(root).stream()
                .filter(selection -> selection.name().toLowerCase(Locale.ROOT).equals(normalizedName))
                .filter(selection -> selection.geometryScore() > 0.0D)
                .findFirst();
    }

    static Optional<Selection> findSibling(Selection selection, String name) {
        if (selection.ancestors().isEmpty()) {
            return Optional.empty();
        }

        ModelPart parent = selection.ancestors().getLast();
        ModelPart sibling = parent.children.get(name);
        double score = geometryScore(sibling);
        if (sibling == null || score <= 0.0D) {
            return Optional.empty();
        }

        int separator = selection.path().lastIndexOf('/');
        String parentPath = separator < 0 ? "root" : selection.path().substring(0, separator);
        return Optional.of(new Selection(sibling, selection.ancestors(), parentPath + "/" + name, name, true, score));
    }

    static Selection root(ModelPart root) {
        return new Selection(root, List.of(), "root", "root", true, geometryScore(root));
    }

    static Optional<Selection> largestDirectGeometry(Selection selection) {
        GeometryOwner owner = largestDirectGeometry(selection.part(), selection.ancestors(), selection.path(), selection.name());
        return owner.largestCubeVolume() > 0.0D ? Optional.of(owner.selection()) : Optional.empty();
    }

    static List<ModelPart> orientationPath(Selection selection) {
        LinkedHashSet<ModelPart> path = new LinkedHashSet<>(selection.ancestors());
        path.add(selection.part());
        if (selection.includeChildren()) {
            addPathToLargestCube(selection.part(), path);
        }
        return List.copyOf(path);
    }

    static boolean hasGeometry(ModelPart part) {
        return geometryScore(part) > 0.0D;
    }

    static double geometryScore(ModelPart part) {
        if (part == null) {
            return 0.0D;
        }

        double score = directGeometryScore(part);
        for (ModelPart child : part.children.values()) {
            score += geometryScore(child);
        }
        return score;
    }

    private static double directGeometryScore(ModelPart part) {
        double score = 0.0D;
        for (ModelPart.Cube cube : part.cubes) {
            score += cubeVolume(cube);
        }
        return score;
    }

    private static double cubeVolume(ModelPart.Cube cube) {
        float width = Math.abs(cube.maxX - cube.minX);
        float height = Math.abs(cube.maxY - cube.minY);
        float depth = Math.abs(cube.maxZ - cube.minZ);
        if (width <= MIN_CUBE_SIZE || height <= MIN_CUBE_SIZE || depth <= MIN_CUBE_SIZE) {
            return 0.0D;
        }
        return (double) width * height * depth;
    }

    private static boolean addPathToLargestCube(ModelPart part, LinkedHashSet<ModelPart> path) {
        ModelPart bestChild = null;
        double bestScore = largestCubeVolume(part);

        for (ModelPart child : part.children.values()) {
            double childScore = largestCubeVolumeRecursive(child);
            if (childScore > bestScore) {
                bestChild = child;
                bestScore = childScore;
            }
        }

        if (bestChild == null) {
            return bestScore > 0.0D;
        }

        path.add(bestChild);
        addPathToLargestCube(bestChild, path);
        return true;
    }

    private static double largestCubeVolumeRecursive(ModelPart part) {
        double largest = largestCubeVolume(part);
        for (ModelPart child : part.children.values()) {
            largest = Math.max(largest, largestCubeVolumeRecursive(child));
        }
        return largest;
    }

    private static double largestCubeVolume(ModelPart part) {
        double largest = 0.0D;
        for (ModelPart.Cube cube : part.cubes) {
            largest = Math.max(largest, cubeVolume(cube));
        }
        return largest;
    }

    private static GeometryOwner largestDirectGeometry(ModelPart part, List<ModelPart> ancestors, String path, String name) {
        GeometryOwner best = new GeometryOwner(
                new Selection(part, ancestors, path, name, false, directGeometryScore(part)),
                largestCubeVolume(part));

        for (var entry : part.children.entrySet()) {
            ArrayList<ModelPart> childAncestors = new ArrayList<>(ancestors);
            childAncestors.add(part);
            GeometryOwner childOwner = largestDirectGeometry(entry.getValue(), List.copyOf(childAncestors),
                    path + "/" + entry.getKey(), entry.getKey());
            if (childOwner.largestCubeVolume() > best.largestCubeVolume()) {
                best = childOwner;
            }
        }
        return best;
    }

    private static boolean isHeadName(String name) {
        return name.toLowerCase(Locale.ROOT).contains("head");
    }

    private static int namePriority(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        if (normalized.equals("head")) {
            return 0;
        }
        if (normalized.equals("head2")) {
            return 1;
        }
        if (normalized.startsWith("head_")) {
            return 2;
        }
        return 3;
    }

    private static List<Selection> collectParts(ModelPart root) {
        ArrayList<Selection> parts = new ArrayList<>();
        collectChildren(root, List.of(root), "root", parts);
        return parts;
    }

    private static void collectChildren(ModelPart parent, List<ModelPart> ancestors, String parentPath, List<Selection> parts) {
        for (var entry : parent.children.entrySet()) {
            String name = entry.getKey();
            ModelPart child = entry.getValue();
            String path = parentPath + "/" + name;
            parts.add(new Selection(child, ancestors, path, name, true, geometryScore(child)));

            ArrayList<ModelPart> childAncestors = new ArrayList<>(ancestors);
            childAncestors.add(child);
            collectChildren(child, List.copyOf(childAncestors), path, parts);
        }
    }

    record Selection(ModelPart part, List<ModelPart> ancestors, String path, String name, boolean includeChildren, double geometryScore) {
        Selection {
            ancestors = List.copyOf(ancestors);
        }
    }

    private record NamedPart(String name, ModelPart part, double largestCubeVolume, double geometryScore) {
    }

    private record GeometryOwner(Selection selection, double largestCubeVolume) {
    }
}
