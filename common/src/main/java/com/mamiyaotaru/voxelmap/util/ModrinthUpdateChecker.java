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
import java.util.function.Consumer;

/**
 * Checks Modrinth for the latest compatible version and shows an in-game chat message if an update exists.
 * This implementation returns both the version number and the changelog, because the Modrinth versions API
 * provides a "changelog" field in each version object. [page:1]
 */
public class ModrinthUpdateChecker {

    /**
     * Modrinth API endpoint to list a project's versions.
     * Each entry can include "version_number", "game_versions", "loaders" and "changelog". [page:1]
     */
    private static final String API_URL = "https://api.modrinth.com/v2/project/{id}/version";

    private final String projectId;
    private final String loader;

    @Nullable
    private final String minecraftVersion;

    /**
     * Container for data returned from Modrinth that we need in-game.
     * "version" is the normalized version string used for comparison and the download page URL.
     * "changelog" is shown as a hover tooltip on the download button text. [page:1]
     */
    public record UpdateInfo(String version, @Nullable String changelog) {
    }

    /**
     * Create a new update checker for the given project.
     * Checks the latest compatible version for the given loader and any Minecraft version.
     *
     * @param projectId the Modrinth project ID or slug
     * @param loader    the loader name used by Modrinth (example: "fabric")
     */
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
     * Performs an async request to Modrinth and passes the latest compatible version + changelog
     * to the provided consumer. [page:1]
     *
     * @param consumer callback receiving UpdateInfo (version + changelog)
     */
    public void checkVersion(Consumer<UpdateInfo> consumer) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL.replace("{id}", projectId))).GET().build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> {
                if (response.statusCode() != 200) return;

                JsonArray versionsArray = JsonParser.parseString(response.body()).getAsJsonArray();
                UpdateInfo latest = getLatestVersionInfo(versionsArray);
                if (latest == null) return;

                consumer.accept(latest);
            });
        } catch (Exception exception) {
            VoxelConstants.getLogger().log(Level.ERROR, exception);
        }
    }

    /**
     * From the Modrinth versions list, finds the newest entry that matches:
     * - requested loader
     * - requested Minecraft version (if not null) [page:1]
     * <p>
     * Returns version_number (normalized) plus the "changelog" text. [page:1]
     * <p>
     * Note: This keeps your existing comparison approach (string compare on a normalized version).
     * If you later want "true newest" ordering, consider sorting by Modrinth's date_published. [page:1]
     *
     * @param versions response array from GET /project/{id|slug}/version [page:1]
     * @return UpdateInfo or null if none match
     */
    @Nullable
    protected UpdateInfo getLatestVersionInfo(JsonArray versions) {
        JsonObject newest = versions.asList().stream()
                .map(JsonElement::getAsJsonObject)
                .filter(this::isVersionCompatible)
                .max(Comparator.comparing(o -> getRawVersion(o.get("version_number").getAsString())))
                .orElse(null);

        if (newest == null) return null;

        String rawVersion = getRawVersion(newest.get("version_number").getAsString());

        String changelog = null;
        if (newest.has("changelog") && !newest.get("changelog").isJsonNull()) {
            changelog = newest.get("changelog").getAsString();
        }

        return new UpdateInfo(rawVersion, changelog);
    }

    /**
     * Checks if a version object is compatible with the given loader and (optional) Minecraft version.
     * The Modrinth versions API contains "game_versions" and "loaders" arrays for each version. [page:1]
     *
     * @param version Modrinth version object
     * @return true if compatible
     */
    protected boolean isVersionCompatible(JsonObject version) {
        JsonArray gameVersions = version.get("game_versions").getAsJsonArray();
        JsonArray loaders = version.get("loaders").getAsJsonArray();

        boolean mcOk = (minecraftVersion == null) || gameVersions.contains(new JsonPrimitive(minecraftVersion));
        boolean loaderOk = loaders.contains(new JsonPrimitive(loader));
        return mcOk && loaderOk;
    }

    /**
     * Normalizes a version string to the part you compare against.
     * Example: "fabric-1.2+1.17.1" -> "1.2"
     *
     * @param version the version string
     * @return normalized version
     */
    public static String getRawVersion(String version) {
        if (version.isEmpty()) return version;
        version = version.replaceAll("^\\D+", "");
        String[] split = version.split("\\+");
        return split[0];
    }

    /**
     * Checks for updates and shows a clickable message in chat:
     * - The download part is clickable (OpenUrl).
     * - The download part shows the changelog as hover text (SHOW_TEXT). [page:0][page:1]
     * <p>
     * This assumes you already have these translations:
     * voxelmap.update.prefix, voxelmap.update.link, voxelmap.update.suffix
     */
    public static void checkUpdates() {
        String modVersion = FabricLoader.getInstance().getModContainer("voxelmap").map(container -> container.getMetadata().getVersion().getFriendlyString()).orElse(null);
        String minecraftVersion = SharedConstants.getCurrentVersion().name();

        new ModrinthUpdateChecker("voxelmap-updated", VoxelConstants.getModApiBridge().getModLoader(), minecraftVersion).checkVersion(info -> {
            if (modVersion == null || ModrinthUpdateChecker.getRawVersion(modVersion).equals(info.version())) {
                VoxelConstants.getLogger().info("Voxelmap is up to date.");
                return;
            }

            VoxelConstants.getLogger().info("Newer version of Voxelmap available: {}", info.version());

            String url = "https://modrinth.com/mod/voxelmap-updated/version/" + info.version();
            int green = 0xA8E6CF;
            int red = 0xFF9AA2;

            Component prefix = Component.translatable("voxelmap.update.prefix", info.version()).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(green)));
            Component suffix = Component.translatable("voxelmap.update.suffix").setStyle(Style.EMPTY.withColor(TextColor.fromRgb(green)));
            Component hover;
            if (info.changelog() == null || info.changelog().isBlank()) {
                hover = Component.literal("No changelog provided.");
            } else {
                String[] lines = info.changelog().split("\\R", -1);
                Component built = Component.translatable("voxelmap.update.changes").setStyle(Style.EMPTY.withColor(red)).append(Component.literal("\n"));

                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) built = built.copy().append(Component.literal("\n"));
                    built = built.copy().append(Component.literal(lines[i]).setStyle(Style.EMPTY.withColor(green)));
                }
                hover = built;
            }

            Style linkStyle = Style.EMPTY.withColor(TextColor.fromRgb(red)).withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create(url))).withHoverEvent(new HoverEvent.ShowText(hover));
            Component link = Component.translatable("voxelmap.update.link").setStyle(linkStyle);
            Component msg = prefix.copy().append(link).append(suffix);
            VoxelConstants.getMinecraft().execute(() -> VoxelConstants.getPlayer().displayClientMessage(msg, false));
        });
    }
}
