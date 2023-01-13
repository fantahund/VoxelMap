package com.mamiyaotaru.voxelmap.persistent;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IPersistentMap;
import com.mamiyaotaru.voxelmap.util.I18nUtils;
import com.mamiyaotaru.voxelmap.util.MessageUtils;
import com.mamiyaotaru.voxelmap.util.TextUtils;
import net.minecraft.client.world.ClientWorld;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class WorldMatcher {
    private final IPersistentMap map;
    private final ClientWorld world;
    private boolean cancelled;

    public WorldMatcher(IPersistentMap map, ClientWorld world) {
        this.map = map;
        this.world = world;
    }

    public void findMatch() {
        Runnable runnable = new Runnable() {
            int x;
            int z;
            final ArrayList<ComparisonCachedRegion> candidateRegions = new ArrayList<>();
            ComparisonCachedRegion region;
            final String worldName = VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName();
            final String worldNamePathPart = TextUtils.scrubNameFile(this.worldName);
            final String dimensionName = VoxelConstants.getVoxelMapInstance().getDimensionManager().getDimensionContainerByWorld(WorldMatcher.this.world).getStorageName();
            final String dimensionNamePathPart = TextUtils.scrubNameFile(this.dimensionName);
            final File cachedRegionFileDir = new File(VoxelConstants.getMinecraft().runDirectory, "/voxelmap/cache/" + this.worldNamePathPart + "/");

            public void run() {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException var8) {
                    VoxelConstants.getLogger().error(var8);
                }

                this.cachedRegionFileDir.mkdirs();
                ArrayList<String> knownSubworldNames = new ArrayList<>(VoxelConstants.getVoxelMapInstance().getWaypointManager().getKnownSubworldNames());
                String[] subworldNamesArray = new String[knownSubworldNames.size()];
                knownSubworldNames.toArray(subworldNamesArray);
                MessageUtils.printDebug("player coords " + VoxelConstants.getPlayer().getX() + " " + VoxelConstants.getPlayer().getZ() + " in world " + VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName());
                this.x = (int) Math.floor(VoxelConstants.getPlayer().getX() / 256.0);
                this.z = (int) Math.floor(VoxelConstants.getPlayer().getZ() / 256.0);
                this.loadRegions(subworldNamesArray);
                int attempts = 0;

                while (!WorldMatcher.this.cancelled && (this.candidateRegions.size() == 0 || this.region.getLoadedChunks() < 5) && attempts < 5) {
                    ++attempts;

                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException var7) {
                        VoxelConstants.getLogger().error(var7);
                    }

                    if (this.x == (int) Math.floor(VoxelConstants.getPlayer().getX() / 256.0) && this.z == (int) Math.floor(VoxelConstants.getPlayer().getZ() / 256.0)) {
                        if (this.candidateRegions.size() > 0) {
                            MessageUtils.printDebug("going to load current region");
                            this.region.loadCurrent();
                            MessageUtils.printDebug("loaded chunks in local region: " + this.region.getLoadedChunks());
                        }
                    } else {
                        this.x = (int) Math.floor(VoxelConstants.getPlayer().getX() / 256.0);
                        this.z = (int) Math.floor(VoxelConstants.getPlayer().getZ() / 256.0);
                        MessageUtils.printDebug("player coords changed to " + VoxelConstants.getPlayer().getX() + " " + VoxelConstants.getPlayer().getZ() + " in world " + VoxelConstants.getVoxelMapInstance().getWaypointManager().getCurrentWorldName());
                        this.loadRegions(subworldNamesArray);
                    }

                    if (attempts >= 5) {
                        if (this.candidateRegions.size() == 0) {
                            MessageUtils.printDebug("no candidate regions at current coordinates, bailing");
                        } else {
                            MessageUtils.printDebug("took too long to load local region, bailing");
                        }
                    }
                }

                Iterator<ComparisonCachedRegion> iterator = this.candidateRegions.iterator();

                while (!WorldMatcher.this.cancelled && iterator.hasNext()) {
                    ComparisonCachedRegion candidateRegion = iterator.next();
                    MessageUtils.printDebug("testing region " + candidateRegion.getSubworldName() + ": " + candidateRegion.getKey());
                    if (this.region.getSimilarityTo(candidateRegion) < 95) {
                        MessageUtils.printDebug("region failed");
                        iterator.remove();
                    } else {
                        MessageUtils.printDebug("region succeeded");
                    }
                }

                MessageUtils.printDebug("remaining regions: " + this.candidateRegions.size());
                if (!WorldMatcher.this.cancelled && this.candidateRegions.size() == 1 && !VoxelConstants.getVoxelMapInstance().getWaypointManager().receivedAutoSubworldName()) {
                    VoxelConstants.getVoxelMapInstance().newSubWorldName(this.candidateRegions.get(0).getSubworldName(), false);
                    MessageUtils.chatInfo(I18nUtils.getString("worldmap.multiworld.foundworld1") + ":" + " §a" + this.candidateRegions.get(0).getSubworldName() + ".§r" + " " + I18nUtils.getString("worldmap.multiworld.foundworld2"));
                } else if (!WorldMatcher.this.cancelled && !VoxelConstants.getVoxelMapInstance().getWaypointManager().receivedAutoSubworldName()) {
                    MessageUtils.printDebug("remaining regions: " + this.candidateRegions.size());
                    MessageUtils.chatInfo("§4VoxelMap§r" + ":" + " " + I18nUtils.getString("worldmap.multiworld.unknownsubworld"));
                }

            }

            private void loadRegions(String[] subworldNamesArray) {
                for (String subworldName : subworldNamesArray) {
                    if (!WorldMatcher.this.cancelled) {
                        File subworldDir = new File(this.cachedRegionFileDir, subworldName + "/" + this.dimensionNamePathPart);
                        if (subworldDir.isDirectory()) {
                            ComparisonCachedRegion candidateRegion = new ComparisonCachedRegion(WorldMatcher.this.map, this.x + "," + this.z, WorldMatcher.this.world, this.worldName, subworldName, this.x, this.z);
                            candidateRegion.loadStored();
                            this.candidateRegions.add(candidateRegion);
                            MessageUtils.printDebug("added candidate region " + candidateRegion.getSubworldName() + ": " + candidateRegion.getKey());
                        } else {
                            MessageUtils.printDebug(subworldName + " not found as a candidate region");
                        }
                    }
                }

                this.region = new ComparisonCachedRegion(WorldMatcher.this.map, this.x + "," + this.z, VoxelConstants.getMinecraft().world, this.worldName, "", this.x, this.z);
                MessageUtils.printDebug("going to load current region");
                this.region.loadCurrent();
                MessageUtils.printDebug("loaded chunks in local region: " + this.region.getLoadedChunks());
            }
        };
        ThreadManager.executorService.execute(runnable);
    }

    public void cancel() {
        this.cancelled = true;
    }
}
