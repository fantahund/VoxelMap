/*
 * MIT License
 *
 * Copyright (c) 2025 Clickism
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mamiyaotaru.voxelmap.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Utility class to check for newer versions of a project hosted on Modrinth.
 * <p>
 * Compared to the old implementation, this one also aggregates changelogs for all
 * versions newer than the currently installed version, because the Modrinth version
 * list response includes a "changelog" field per version object. [page:2]
 */
public class ModrinthUpdateChecker {

    /**
     * Modrinth endpoint to list versions of a project. [page:2]
     */
    private static final String API_URL = "https://api.modrinth.com/v2/project/{id}/version";

    private final String projectId;
    private final String loader;

    @Nullable
    private final String minecraftVersion;

    private static final int green = 0xA8E6CF;
    private static final int red = 0xFF9AA2;

    /**
     * Data we need for update UI:
     * - version: normalized version string used for comparisons and URL building
     * - changelog: raw changelog text from Modrinth (may contain multiple lines) [page:2]
     */
    public record VersionInfo(String version, @Nullable String changelog) {
    }

    /**
     * Result of the update check:
     * - latestVersion: latest compatible version (normalized)
     * - updates: all versions that are newer than the installed version
     */
    public record UpdateResult(String latestVersion, List<VersionInfo> updates) {}

    public ModrinthUpdateChecker(String projectId, String loader) {
        this(projectId, loader, null);
    }

    /**
     * Create a new update checker for the given project.
     * Checks the latest compatible version for the given loader and a specific Minecraft version.
     *
     * @param projectId        the Modrinth project ID or slug
     * @param loader           the loader name used by Modrinth
     * @param minecraftVersion the current Minecraft version (or null for any)
     */
    public ModrinthUpdateChecker(String projectId, String loader, @Nullable String minecraftVersion) {
        this.projectId = projectId;
        this.loader = loader;
        this.minecraftVersion = minecraftVersion;
    }

    /**
     * Fetches versions from Modrinth and returns an aggregated update result.
     * The Modrinth response is an array of version objects containing fields like
     * version_number, loaders, game_versions, and changelog. [page:2]
     */
    public void checkUpdates(String installedModVersion, Consumer<UpdateResult> consumer) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL.replace("{id}", projectId))).GET().build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> {
                if (response.statusCode() != 200) return;

                JsonArray versionsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                UpdateResult result = buildUpdateResult(installedModVersion, versionsArray);
                if (result == null) return;

                consumer.accept(result);
            });
        } catch (Exception exception) {
            VoxelConstants.getLogger().log(Level.ERROR, exception);
        }
    }

    /**
     * Builds a list of all compatible versions from Modrinth, then:
     * - finds the latest version
     * - filters the versions that are newer than the installed version
     * <p>
     * Uses version_number and changelog from the Modrinth version objects. [page:2]
     */
    @Nullable
    protected UpdateResult buildUpdateResult(String installedModVersion, JsonArray versions) {
        String installedRaw = getRawVersion(installedModVersion);

        List<VersionInfo> compatible = versions.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(this::isVersionCompatible)
                .map(obj -> new VersionInfo(
                        getRawVersion(obj.get("version_number").getAsString()),
                        (obj.has("changelog") && !obj.get("changelog").isJsonNull()) ? obj.get("changelog").getAsString() : null
                )).collect(Collectors.toList());

        if (compatible.isEmpty()) return null;

        String latest = compatible.stream().map(VersionInfo::version).max(String::compareTo).orElse(null);

        if (latest == null) return null;

        List<VersionInfo> updates = compatible.stream()
                .filter(v -> v.version().compareTo(installedRaw) > 0)
                .sorted(Comparator.comparing(VersionInfo::version))
                .collect(Collectors.toList());

        return new UpdateResult(latest, updates);
    }

    /**
     * Modrinth version objects include "game_versions" and "loaders". [page:2]
     */
    protected boolean isVersionCompatible(JsonObject version) {
        JsonArray gameVersions = version.get("game_versions").getAsJsonArray();
        JsonArray loaders = version.get("loaders").getAsJsonArray();

        boolean mcOk = (minecraftVersion == null) || gameVersions.contains(new JsonPrimitive(minecraftVersion));
        boolean loaderOk = loaders.contains(new JsonPrimitive(loader));
        return mcOk && loaderOk;
    }

    /**
     * Normalizes a version string to a comparable value.
     */
    public static String getRawVersion(String version) {
        if (version == null || version.isEmpty()) return "";
        version = version.replaceAll("^\\D+", "");
        String[] split = version.split("\\+");
        return split[0];
    }

    /**
     * Builds a multiline hover component like:
     * <p>
     * 1.1:
     * Change1
     * Change2
     * 1.2:
     * Change1
     * <p>
     * It respects multi-line changelog strings by splitting on line separators.
     */
    public static Component buildAggregatedChangelogHover(List<VersionInfo> updates) {
        if (updates == null || updates.isEmpty()) {
            return Component.literal("No changelog available.");
        }

        Component out = Component.translatable("voxelmap.update.changes").setStyle(Style.EMPTY.withColor(red)).append("\n");

        for (int i = 0; i < updates.size(); i++) {
            VersionInfo v = updates.get(i);

            if (i > 0) out = out.copy().append(Component.literal("\n"));

            out = out.copy().append(Component.literal(v.version() + ":").setStyle(Style.EMPTY.withColor(red)));

            String changelog = (v.changelog() == null) ? "" : v.changelog();
            String[] lines = changelog.split("\\R", -1);

            if (lines.length == 0 || (lines.length == 1 && lines[0].isBlank())) {
                out = out.copy().append(Component.literal("\n  (No changelog provided.)").setStyle(Style.EMPTY.withColor(green)));
                continue;
            }

            for (String line : lines) {
                out = out.copy().append(Component.literal("\n " + line).setStyle(Style.EMPTY.withColor(green)));
            }
        }

        return out;
    }

    public static void checkUpdates() {
        if (!VoxelConstants.getVoxelMapInstance().getMapOptions().updateNotifier) {
            return;
        }
        String modVersion = FabricLoader.getInstance().getModContainer("voxelmap").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(null);

        if (modVersion == null) return;

        String mcVersion = SharedConstants.getCurrentVersion().name();

        new ModrinthUpdateChecker("voxelmap-updated", VoxelConstants.getModApiBridge().getModLoader(), mcVersion).checkUpdates(modVersion, result -> {
            String installedRaw = getRawVersion(modVersion);
            if (installedRaw.equals(result.latestVersion())) {
                VoxelConstants.getLogger().info("Voxelmap is up to date.");
                return;
            }

            String url = "https://modrinth.com/mod/voxelmap-updated/version/" + result.latestVersion();
            Component prefix = Component.translatable("voxelmap.update.prefix", result.latestVersion()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(green)));
            Component suffix = Component.translatable("voxelmap.update.suffix").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(green)));
            Component hover = buildAggregatedChangelogHover(result.updates());
            Style linkStyle = Style.EMPTY.withColor(TextColor.fromRgb(red)).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(url))).withHoverEvent(new HoverEvent.ShowText(hover));

            Component link = Component.translatable("voxelmap.update.link").setStyle(linkStyle);
            Component msg = prefix.copy().append(link).append(suffix);
            VoxelConstants.getMinecraft().execute(() -> VoxelConstants.getPlayer().displayClientMessage(msg, false));
        });
    }
}
