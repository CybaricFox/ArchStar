package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.API.TickContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.Components.Energy.EnergyBlockType;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;

/*
    Tick system for energy blocks.
 */
public class EnergySystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float v, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        TickContext context = new TickContext(index, archetypeChunk, commandBuffer);
        if(!context.isValid) return;

        //Tick energy networks
        ArrayList<EnergyNetwork> networks = ArchStar.get().getEnergyNetworkSystem().getAllNetworks();

        for(EnergyNetwork network : networks) {
            //Run consumers first
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.CONSUMER).entrySet()) {
                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(commandBuffer.getExternalData().getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, commandBuffer);

                blockSet.getValue().run(essentialsContext);
            }
            //Run producers next
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.PRODUCER).entrySet()) {
                blockSet.getValue().resetOutputThisTick();

                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(commandBuffer.getExternalData().getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, commandBuffer);

                boolean result = blockSet.getValue().run(essentialsContext);

                //Only producers want to use this
                if(result) {
                    ArchLibrary.changeBlockState(blockRef, commandBuffer, "Processing");
                } else {
                    ArchLibrary.changeBlockState(blockRef, commandBuffer, "default");
                }
            }
            //Storages usually don't need to have a run function, but run them anyway in case of unique behaviours
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.STORAGE).entrySet()) {
                blockSet.getValue().resetOutputThisTick();

                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(commandBuffer.getExternalData().getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, commandBuffer);

                blockSet.getValue().run(essentialsContext);
            }

            //Once all blocks in the network have been checked, run any transaction requests made during this time.
            network.runTransactions();
        }
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }
}


