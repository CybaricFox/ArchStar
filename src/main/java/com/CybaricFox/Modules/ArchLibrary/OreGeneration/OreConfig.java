package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

public class OreConfig {
    private final String ore;
    private final int minPockets;
    private final int maxPockets;
    private final int minVeinSize;
    private final int maxVeinSize;
    private final int minY;
    private final int maxY;
    private final String[] hostBlocks;

    public OreConfig(String itemId, int minPockets, int maxPockets, int minVeinSize, int maxVeinSize, int minY, int maxY, String[] hostBlocks) {
        ore = itemId;
        this.minPockets = minPockets;
        this.maxPockets = maxPockets;
        this.maxVeinSize = maxVeinSize;
        this.minY = minY;
        this.maxY = maxY;

        this.hostBlocks = new String[hostBlocks.length + 1];
        for(int i = 0; i < this.hostBlocks.length; i++) {
            if(i == this.hostBlocks.length - 1) {
                this.hostBlocks[i] = ore;
            } else {
                this.hostBlocks[i] = hostBlocks[i];
            }
        }

        if(minVeinSize == 0) {
            this.minVeinSize = 1;
        } else {
            this.minVeinSize = minVeinSize;
        }
    }

    public String getOre() {
        return ore;
    }

    public int getMinPockets() {
        return minPockets;
    }

    public int getMaxPockets() {
        return maxPockets;
    }

    public int getMinVeinSize() {
        return minVeinSize;
    }

    public int getMaxVeinSize() {
        return maxVeinSize;
    }

    public int getMinY() {
        return minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public String[] getHostBlocks() {
        return hostBlocks;
    }
}
