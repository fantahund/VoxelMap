package com.mamiyaotaru.voxelmap.util;

import com.mamiyaotaru.voxelmap.interfaces.IChangeObserver;

import java.util.ArrayList;
import java.util.List;

public class WorldUpdateListener {
    private List<IChangeObserver> chunkProcessors = new ArrayList();

    public void addListener(IChangeObserver chunkProcessor) {
        this.chunkProcessors.add(chunkProcessor);
    }

    public void notifyObservers(int chunkX, int chunkZ) {
        for (IChangeObserver chunkProcessor : this.chunkProcessors) {
            chunkProcessor.handleChangeInWorld(chunkX, chunkZ);
        }

    }
}
