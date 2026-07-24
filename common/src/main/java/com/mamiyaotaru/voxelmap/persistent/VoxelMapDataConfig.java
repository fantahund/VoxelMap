package com.mamiyaotaru.voxelmap.persistent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.mamiyaotaru.voxelmap.VoxelConstants;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VoxelMapDataConfig {
    public enum Decision {
        MIGRATED
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static VoxelMapDataConfig instance;

    private Map<String, String> worldDecisions = new LinkedHashMap<>();
    private Map<String, List<String>> serverAliases = new LinkedHashMap<>();

    private VoxelMapDataConfig() {}

    public static synchronized VoxelMapDataConfig getInstance() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    private static File getConfigFile() {
        File dir = new File(VoxelConstants.getMinecraft().gameDirectory, "config/voxelmap");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new File(dir, "data.json");
    }

    private static VoxelMapDataConfig load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (Reader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                VoxelMapDataConfig loaded = GSON.fromJson(reader, VoxelMapDataConfig.class);
                if (loaded != null) {
                    if (loaded.worldDecisions == null) {
                        loaded.worldDecisions = new LinkedHashMap<>();
                    }

                    if (loaded.serverAliases == null) {
                        loaded.serverAliases = new LinkedHashMap<>();
                    }

                    return loaded;
                }
            } catch (IOException | JsonSyntaxException e) {
                VoxelConstants.getLogger().error("Failed to load VoxelMap data config, starting fresh", e);
            }
        }

        return new VoxelMapDataConfig();
    }

    public synchronized void save() {
        try (Writer writer = new FileWriter(getConfigFile(), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            VoxelConstants.getLogger().error("Failed to save VoxelMap data config", e);
        }
    }

    public synchronized Decision getWorldDecision(String saveId) {
        String value = this.worldDecisions.get(saveId);
        if (value == null) {
            return null;
        }

        try {
            return Decision.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public synchronized void setWorldDecision(String saveId, Decision decision) {
        this.worldDecisions.put(saveId, decision.name());
        save();
    }

    public synchronized String resolveCanonical(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return serverName;
        }

        String needle = serverName.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : this.serverAliases.entrySet()) {
            if (entry.getKey().toLowerCase(Locale.ROOT).equals(needle)) {
                return entry.getKey();
            }

            if (entry.getValue() != null) {
                for (String alias : entry.getValue()) {
                    if (alias != null && alias.toLowerCase(Locale.ROOT).equals(needle)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return serverName;
    }

    public synchronized List<String> getAliasesFor(String canonical) {
        return this.serverAliases.getOrDefault(canonical, List.of());
    }

    public synchronized void setAliasesFor(String canonical, List<String> aliases) {
        if (aliases == null || aliases.isEmpty()) {
            this.serverAliases.remove(canonical);
        } else {
            this.serverAliases.put(canonical, List.copyOf(aliases));
        }

        save();
    }
}
