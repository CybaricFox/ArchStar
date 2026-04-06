package com.CybaricFox.Modules.ArchLibrary;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class TickContext {
    public BlockSection blockSection;
    public ChunkSection chunkSection;
    public BlockComponentChunk blockComponentChunk;
    public boolean isValid = false;

    public TickContext(int index, ArchetypeChunk<ChunkStore> archetypeChunk, CommandBuffer<ChunkStore> commandBuffer) {
        blockSection = archetypeChunk.getComponent(index, BlockSection.getComponentType());
        if(blockSection == null) return;

        if (blockSection.getTickingBlocksCountCopy() == 0) {
            return;
        }

        chunkSection = archetypeChunk.getComponent(index, ChunkSection.getComponentType());
        if(chunkSection == null) return;

        blockComponentChunk = commandBuffer.getComponent(chunkSection.getChunkColumnReference(), BlockComponentChunk.getComponentType());
        if(blockComponentChunk == null) return;

        isValid = true;
    }
}
