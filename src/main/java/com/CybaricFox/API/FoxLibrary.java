package com.CybaricFox.API;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

//General Library class for common functions
public class FoxLibrary {
    //Converts global coordinates to local chunk coordinates
    public static Vector3i convertToLocalCoords(Vector3i globalCoords) {
        return new Vector3i(
                Math.floorMod(globalCoords.x, 32),
                globalCoords.y,
                Math.floorMod(globalCoords.z, 32)
        );
    }

    //Returns a block entity at the world location if one exists
    public static Ref<ChunkStore> getBlockEntity(World world, Vector3i globalPos) {
        //Convert global coords to local coords
        Vector3i localCoords = FoxLibrary.convertToLocalCoords(globalPos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(globalPos.x, globalPos.z));

        Ref<ChunkStore> chunkRef = chunk.getReference();

        BlockComponentChunk blockComponentChunk = chunkRef.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());

        chunk.getBlockType(localCoords);

        //Get the index of the block in the chunk
        int index = ChunkUtil.indexBlockInColumn(localCoords.x, localCoords.y, localCoords.z);

        return blockComponentChunk.getEntityReference(index);
    }
}
