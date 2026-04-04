package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.WaypointManager;
import com.mamiyaotaru.voxelmap.util.DimensionManager;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.MutableBlockPos;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class WorldMatcher {
    private final PersistentMap map;
    private final ClientLevel world;
    private boolean cancelled;

    public WorldMatcher(PersistentMap map, ClientLevel world) {
        this.map = map;
        this.world = world;
    }

    public void findMatch() {
        Runnable runnable = new Runnable() {
            final Minecraft minecraft = Minecraft.getInstance();
            final Player player = VoxelConstants.getPlayer();
            final WaypointManager waypointManager = VoxelConstants.getVoxelMapInstance().getWaypointManager();
            final DimensionManager dimensionManager = VoxelConstants.getVoxelMapInstance().getDimensionManager();

            final String worldName = waypointManager.getCurrentWorldName();
            final String worldNamePathPart = TextUtils.scrubNameFile(worldName);
            final String dimensionName = dimensionManager.getDimensionContainerByWorld(world).getStorageName();
            final String dimensionNamePathPart = TextUtils.scrubNameFile(dimensionName);
            final File baseDirectory = new File(minecraft.gameDirectory,  CachedRegion.getWorldDirectory(worldNamePathPart) + "/");

            final MutableBlockPos blockPos = new MutableBlockPos(0, 0, 0);
            int x;
            int z;
            final ArrayList<ComparisonCachedRegion> candidateRegions = new ArrayList<>();
            ComparisonCachedRegion currentRegion;

            @Override
            public void run() {
                try {
                    Thread.sleep(500L);

                    if (cancelled) return;
                    if (!baseDirectory.exists()) baseDirectory.mkdirs();

                    updateTargetCoords();
                    loadCandidateRegions();

                    int attempts = 0;
                    while (!cancelled && (candidateRegions.isEmpty() || currentRegion.getLoadedChunks() < 5) && attempts < 5) {
                        Thread.sleep(1000L);
                        attempts++;

                        if (!isCoordsChanged()) {
                            currentRegion.loadCurrent();
                        } else {
                            updateTargetCoords();
                            loadCandidateRegions();
                        }
                    }

                    if (cancelled || waypointManager.receivedAutoSubworldName()) return;

                    Iterator<ComparisonCachedRegion> candidates = candidateRegions.iterator();
                    int minSimilarity = 94;
                    while (!cancelled && candidates.hasNext()) {
                        ComparisonCachedRegion candidate = candidates.next();
                        int similarity = currentRegion.getSimilarityTo(candidate);
                        if (similarity <= minSimilarity) {
                            candidates.remove();
                        } else {
                            minSimilarity = similarity;
                        }
                    }

                    if (!cancelled) handleMatchingResult();

                } catch (InterruptedException e) {
                    VoxelConstants.getLogger().error(e);
                }
            }

            private boolean isCoordsChanged() {
                return x != (int) Math.floor(VoxelConstants.getPlayer().getX() / 256.0) || z != (int) Math.floor(VoxelConstants.getPlayer().getZ() / 256.0);
            }

            private void updateTargetCoords() {
                x = (int) Math.floor(VoxelConstants.getPlayer().getX() / 256.0);
                z = (int) Math.floor(VoxelConstants.getPlayer().getZ() / 256.0);
            }

            private void loadCandidateRegions() {
                candidateRegions.clear();

                boolean underground = world.getBrightness(LightLayer.SKY, blockPos.withXYZ(player.getBlockX(), player.getBlockY(), player.getBlockZ())) <= 0;
                int yLayer = Math.floorDiv(player.getBlockY(), PersistentMap.CAVE_LAYER_HEIGHT);

                for (String subWorldName : waypointManager.getKnownSubworldNames()) {
                    if (cancelled) break;

                    File subWorldDirectory = new File(baseDirectory, CachedRegion.getSubWorldDirectory(subWorldName, dimensionNamePathPart, underground, yLayer));
                    if (subWorldDirectory.isDirectory()) {
                        ComparisonCachedRegion candidate = new ComparisonCachedRegion(map, x + "," + z, world, worldName, subWorldName, x, z, underground, yLayer);
                        candidate.loadStored();
                        candidateRegions.add(candidate);
                    }
                }

                currentRegion = new ComparisonCachedRegion(map, x + "," + z, world, worldName, "", x, z, underground, yLayer);
                currentRegion.loadCurrent();
            }

            private void handleMatchingResult() {
                if (waypointManager.receivedAutoSubworldName()) return;

                if (candidateRegions.size() == 1) {
                    String foundName = candidateRegions.getFirst().getSubworldName();
                    VoxelConstants.getVoxelMapInstance().newSubWorldName(foundName, false);

                    MessageUtils.chatInfo(I18n.get("worldmap.multiworld.foundWorld1") + ": §a" + foundName + ".§r " + I18n.get("worldmap.multiworld.foundWorld2"));
                } else {
                    MessageUtils.chatInfo("§4VoxelMap§r: " + I18n.get("worldmap.multiworld.unknownSubworld"));
                }
            }
        };

        ThreadManager.executorService.execute(runnable);
    }

    public void cancel() {
        cancelled = true;
    }
}
