package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class OreGenSystem extends TickingSystem<ChunkStore> {

    private final OreGenerator oreGenerator = new OreGenerator();

    public OreGenerator getOreGenerator() {
        return oreGenerator;
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<ChunkStore> store) {
        if(!oreGenerator.oreGenQueue.isEmpty()) {

            OreHolder holder = oreGenerator.oreGenQueue.getFirst();

            Ref<ChunkStore> ref = holder.world.getChunkStore().getChunkReference(ChunkUtil.indexChunk(holder.x, holder.z));
            if(ref == null) return;

            WorldChunk chunk = store.getComponent(ref, WorldChunk.getComponentType());

            if(chunk == null) return;

            oreGenerator.generateOres(chunk);
            oreGenerator.oreGenQueue.removeFirst();
        }
    }
}
