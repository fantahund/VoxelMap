package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;
import java.util.ArrayList;
import java.util.List;

public class WorldUpdateListener {
    private final List<IChangeObserver> chunkProcessors = new ArrayList<>();

    public void addListener(IChangeObserver chunkProcessor) {
        chunkProcessors.add(chunkProcessor);
    }

    public void notifyObservers(int chunkX, int chunkZ) {
        try {
            for (IChangeObserver chunkProcessor : this.chunkProcessors)
                chunkProcessor.handleChangeInWorld(chunkX, chunkZ);
        } catch (RuntimeException exception) {
            VoxelConstants.getLogger().error("Exception", exception);
        }
    }
}