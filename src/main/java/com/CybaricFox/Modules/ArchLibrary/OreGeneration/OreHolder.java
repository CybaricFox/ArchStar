package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

public class OreHolder {
    public final int x;
    public final int z;
    public final World world;

    public OreHolder(WorldChunk chunk) {
        x = chunk.getX();
        z = chunk.getZ();
        world = chunk.getWorld();
    }
}
