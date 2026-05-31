package com.CybaricFox.Modules.ArchLibrary;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

public class EssentialsContext {
    public World world;
    public WorldChunk chunk;
    public BlockModule.BlockStateInfo info;
    public Vector3i pos;
    public boolean isValid = false;

    public EssentialsContext(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> commandBuffer) {
        world = commandBuffer.getExternalData().getWorld();

        info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if(info == null) return;

        chunk = commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
        if(chunk == null) return;

        pos = ArchLibrary.getGlobalCoordsFromChunk(info, chunk);

        isValid = true;
    }

    public EssentialsContext(Ref<ChunkStore> ref, Store<ChunkStore> store) {
        world = store.getExternalData().getWorld();

        info = store.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if(info == null) return;

        chunk = store.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
        if(chunk == null) return;

        pos = ArchLibrary.getGlobalCoordsFromChunk(info, chunk);

        isValid = true;
    }
}
