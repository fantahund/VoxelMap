package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WaypointFileMerger {
    private static final String SUFFIX = "(merge)";

    private WaypointFileMerger() {}

    public static void mergeInto(File source, File dest) {
        try {
            List<String> destLines = Files.readAllLines(dest.toPath(), StandardCharsets.UTF_8);
            List<String> sourceLines = Files.readAllLines(source.toPath(), StandardCharsets.UTF_8);

            Set<String> subworlds = readCsvSet(destLines, "subworlds:");
            Set<String> oldNorthWorlds = readCsvSet(destLines, "oldNorthWorlds:");
            Map<String, String> seeds = readSeeds(destLines);

            List<String> waypointLines = new ArrayList<>();
            Set<String> identities = new LinkedHashSet<>();
            Set<String> names = new LinkedHashSet<>();
            for (String line : destLines) {
                if (line.startsWith("name:")) {
                    waypointLines.add(line);
                    identities.add(identityOf(line));
                    names.add(nameOf(line));
                }
            }

            subworlds.addAll(readCsvSet(sourceLines, "subworlds:"));
            oldNorthWorlds.addAll(readCsvSet(sourceLines, "oldNorthWorlds:"));
            for (Map.Entry<String, String> entry : readSeeds(sourceLines).entrySet()) {
                seeds.putIfAbsent(entry.getKey(), entry.getValue());
            }

            for (String line : sourceLines) {
                if (!line.startsWith("name:")) {
                    continue;
                }

                if (identities.contains(identityOf(line))) {
                    continue;
                }

                String descrubbedName = nameOf(line);
                String finalLine = line;
                if (names.contains(descrubbedName)) {
                    String candidate = descrubbedName + SUFFIX;
                    while (names.contains(candidate)) {
                        candidate = candidate + SUFFIX;
                    }

                    finalLine = withName(line, candidate);
                    descrubbedName = candidate;
                }

                waypointLines.add(finalLine);
                identities.add(identityOf(finalLine));
                names.add(descrubbedName);
            }

            writeMerged(dest.toPath(), subworlds, oldNorthWorlds, seeds, waypointLines);
        } catch (IOException e) {
            VoxelConstants.getLogger().error("Failed to merge waypoint file " + source.getPath() + " into " + dest.getPath(), e);
        }
    }

    private static Map<String, String> tokens(String line) {
        Map<String, String> map = new LinkedHashMap<>();
        for (String token : line.split(",")) {
            int colon = token.indexOf(':');
            if (colon >= 0) {
                map.put(token.substring(0, colon), token.substring(colon + 1));
            }
        }

        return map;
    }

    private static String nameOf(String line) {
        return TextUtils.descrubName(tokens(line).getOrDefault("name", ""));
    }

    private static String identityOf(String line) {
        Map<String, String> t = tokens(line);
        return t.getOrDefault("name", "") + ' ' + t.getOrDefault("x", "") + ' '
                + t.getOrDefault("z", "") + ' ' + t.getOrDefault("world", "") + ' '
                + t.getOrDefault("dimensions", "");
    }

    private static String withName(String line, String newDescrubbedName) {
        String scrubbed = TextUtils.scrubName(newDescrubbedName);
        StringBuilder sb = new StringBuilder();
        for (String token : line.split(",")) {
            if (sb.length() > 0) {
                sb.append(',');
            }

            if (token.startsWith("name:")) {
                sb.append("name:").append(scrubbed);
            } else {
                sb.append(token);
            }
        }

        return sb.toString();
    }

    private static Set<String> readCsvSet(List<String> lines, String prefix) {
        Set<String> set = new LinkedHashSet<>();
        for (String line : lines) {
            if (line.startsWith(prefix)) {
                for (String entry : line.substring(prefix.length()).split(",")) {
                    if (!entry.isEmpty()) {
                        set.add(entry);
                    }
                }

                break;
            }
        }

        return set;
    }

    private static Map<String, String> readSeeds(List<String> lines) {
        Map<String, String> seeds = new LinkedHashMap<>();
        for (String line : lines) {
            if (line.startsWith("seeds:")) {
                for (String pair : line.substring("seeds:".length()).split(",")) {
                    String[] kv = pair.split("#");
                    if (kv.length == 2) {
                        seeds.put(kv[0], kv[1]);
                    }
                }

                break;
            }
        }

        return seeds;
    }

    private static void writeMerged(Path dest, Set<String> subworlds, Set<String> oldNorthWorlds, Map<String, String> seeds, List<String> waypointLines) throws IOException {
        try (Writer writer = Files.newBufferedWriter(dest, StandardCharsets.UTF_8); PrintWriter out = new PrintWriter(writer)) {
            out.println("subworlds:" + String.join(",", subworlds) + (subworlds.isEmpty() ? "" : ","));
            out.println("oldNorthWorlds:" + String.join(",", oldNorthWorlds) + (oldNorthWorlds.isEmpty() ? "" : ","));

            StringBuilder seedsString = new StringBuilder();
            for (Map.Entry<String, String> entry : seeds.entrySet()) {
                seedsString.append(entry.getKey()).append('#').append(entry.getValue()).append(',');
            }

            out.println("seeds:" + seedsString);

            for (String line : waypointLines) {
                out.println(line);
            }
        }
    }
}
