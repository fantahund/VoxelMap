package com.mamiyaotaru.voxelmap.options;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.options.containers.AbstractOptionsContainer;
import com.mamiyaotaru.voxelmap.util.FileUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class MapOptionsManager {
    private final ArrayList<AbstractOptionsContainer> containers = new ArrayList<>();
    private File saveFile;

    public void addOptionsContainer(AbstractOptionsContainer container) {
        containers.add(container);
    }

    public void updateOptionsActive() {
        if (!VoxelConstants.getVoxelMapInstance().isRunning()) {
            return;
        }

        for (AbstractOptionsContainer settings : containers) {
            settings.updateOptionsActive();
        }
    }

    public void updateOptionsAllowed(ServerSettingsManager serverSettings, String source) {
        if (!VoxelConstants.getVoxelMapInstance().isRunning()) {
            return;
        }

        for (AbstractOptionsContainer settings : containers) {
            settings.updateOptionsAllowed(serverSettings);
        }
        VoxelConstants.getLogger().info("Options updated to match server settings. (Source: {})", source);
    }

    public void loadAll() {
        saveFile = FileUtils.join(FileUtils.minecraftPath(), "config", FileUtils.withExtension("voxelmap", "properties"));
        try {
            if (saveFile.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(saveFile), StandardCharsets.UTF_8.newDecoder()));
                String curLine;
                while ((curLine = in.readLine()) != null) {
                    String[] keyValue = curLine.split(":", 2);

                    for (AbstractOptionsContainer settings : containers) {
                        settings.loadLine(keyValue);
                    }
                }
                in.close();
            }

            saveAll();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Error Loading Settings ", e);
        }
    }

    public void saveAll() {
        saveFile = FileUtils.join(FileUtils.minecraftPath(), "config", FileUtils.withExtension("voxelmap", "properties"));
        saveFile.getParentFile().mkdirs();
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(saveFile), StandardCharsets.UTF_8.newEncoder())));
            for (AbstractOptionsContainer settings : containers) {
                settings.saveAll(out);
            }
            out.close();
        } catch (Exception e) {
            VoxelConstants.getLogger().error("Error Saving Settings ", e);
        }
    }
}
