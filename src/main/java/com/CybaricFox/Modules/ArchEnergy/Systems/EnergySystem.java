package com.CybaricFox.Modules.ArchEnergy.Systems;

import com.CybaricFox.Modules.ArchEnergy.ArchEnergyModule;
import com.CybaricFox.Modules.ArchEnergy.EnergyBlockType;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchEnergy.EnergyNetwork;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
//import org.joml.Vector3i;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;

/*
    Tick system for energy blocks.
 */
public class EnergySystem extends TickingSystem<ChunkStore>{
    @Override
    public void tick(float v, int index, @Nonnull Store<ChunkStore> store) {
        //Tick energy networks
        ArrayList<EnergyNetwork> networks = ArchEnergyModule.get().getEnergyNetworkSystem().getAllNetworks();

        for(EnergyNetwork network : networks) {
            //Run consumers first
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.CONSUMER).entrySet()) {
                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(network.getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, store);
                if(!essentialsContext.isValid) continue;

                MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
                if(machineBehaviorComponent == null) continue;
                machineBehaviorComponent.run(essentialsContext);
            }
            //Run producers next
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.PRODUCER).entrySet()) {
                blockSet.getValue().resetIOThisTick();

                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(network.getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, store);
                if(!essentialsContext.isValid) continue;

                MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
                if(machineBehaviorComponent == null) continue;
                boolean result = machineBehaviorComponent.run(essentialsContext);

                //Only producers want to use this
                if(result) {
                    ArchLibrary.changeBlockState(essentialsContext, "Processing");
                } else {
                    ArchLibrary.changeBlockState(essentialsContext, "default");
                }
            }
            //Storages usually don't need to have a run function, but run them anyway in case of unique behaviours
            for(HashMap.Entry<Vector3i, EnergyComponent> blockSet : network.getAllOfType(EnergyBlockType.STORAGE).entrySet()) {
                blockSet.getValue().resetIOThisTick();

                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(network.getWorld(), blockSet.getKey());

                EssentialsContext essentialsContext = new EssentialsContext(blockRef, store);
                if(!essentialsContext.isValid) continue;

                MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
                if(machineBehaviorComponent == null) continue;
                machineBehaviorComponent.run(essentialsContext);
            }

            //Once all blocks in the network have been checked, run any transaction requests made during this time.
            network.runTransactions();
        }
    }
}


