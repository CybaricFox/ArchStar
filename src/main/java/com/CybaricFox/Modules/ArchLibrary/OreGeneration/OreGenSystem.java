package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;

public class OreGenSystem extends TickingSystem<ChunkStore> {

    private final OreGenerator oreGenerator = new OreGenerator();

    public OreGenerator getOreGenerator() {
        return oreGenerator;
    }

    @Override
    public void tick(float v, int i, @Nonnull Store<ChunkStore> store) {
        if(!oreGenerator.oreGenQueue.isEmpty()) {
            for(int j = 0; j < oreGenerator.oreGenQueue.size(); j++) {
                OreHolder holder = oreGenerator.oreGenQueue.get(j);

                //Ore Gen will attempt to generate ores in a chunk 30 times before giving up.
                if(holder.passes > 30) {
                    holder.markedForRemoval = true;
                    continue;
                }

                Ref<ChunkStore> ref = holder.world.getChunkStore().getChunkReference(ChunkUtil.indexChunk(holder.x, holder.z));
                if(ref == null) {
                    holder.passes++;
                    continue;
                }

                WorldChunk chunk = store.getComponent(ref, WorldChunk.getComponentType());
                if(chunk == null) {
                    holder.passes++;
                    continue;
                }

                oreGenerator.generateOres(chunk);
                holder.markedForRemoval = true;
            }

            for(int j = oreGenerator.oreGenQueue.size() - 1; j >= 0; j--) {
                if(oreGenerator.oreGenQueue.get(j).markedForRemoval) {
                    oreGenerator.oreGenQueue.remove(j);
                }
            }
        }
    }
}
