package com.CybaricFox.ComponentSystems.Helpers;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;


public class EnergyBlockContext {
    public Ref<ChunkStore> ref;
    public CommandBuffer<ChunkStore> buffer;

    public EnergyBlockContext(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> buffer) {
        this.ref = ref;
        this.buffer = buffer;
    }
}
