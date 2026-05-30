package com.mamiyaotaru.voxelmap.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class VoxelmapServerConfigManager {
    private static final Gson CONFIG_GSON = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    private static final Gson PACKET_GSON = new GsonBuilder().serializeNulls().create();

    private final Path configFile;
    private volatile VoxelmapServerConfig activeConfig = VoxelmapServerConfig.defaults();

    public VoxelmapServerConfigManager(Path configFile) {
        this.configFile = configFile;
    }

    public synchronized void loadOrCreate() throws IOException {
        ensureConfigFileExists();
        activeConfig = readConfig();
    }

    public synchronized void reload() throws IOException {
        ensureConfigFileExists();
        VoxelmapServerConfig loadedConfig = readConfig();
        activeConfig = loadedConfig;
    }

    public String createSettingsJson(String worldId) {
        return PACKET_GSON.toJson(activeConfig.createClientSettingsJson(worldId));
    }

    public Path getConfigFile() {
        return configFile;
    }

    private void ensureConfigFileExists() throws IOException {
        if (Files.exists(configFile)) {
            return;
        }

        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        writeDefaultConfig();
    }

    private void writeDefaultConfig() throws IOException {
        try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            CONFIG_GSON.toJson(VoxelmapServerConfig.defaults().toJson(), writer);
            writer.write(System.lineSeparator());
        }
    }

    private VoxelmapServerConfig readConfig() throws IOException {
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonElement rootElement = JsonParser.parseReader(reader);
            if (!rootElement.isJsonObject()) {
                throw new IllegalArgumentException("VoxelMap server config root must be an object");
            }

            JsonObject root = rootElement.getAsJsonObject();
            return VoxelmapServerConfig.fromJson(root);
        }
    }
}
