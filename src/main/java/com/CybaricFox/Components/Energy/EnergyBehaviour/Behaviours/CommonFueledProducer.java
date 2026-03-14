package com.CybaricFox.Components.Energy.EnergyBehaviour.Behaviours;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.Components.Energy.EnergyBehaviour.EnergyBehaviour;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Energy.EnergyTransaction;
import com.CybaricFox.Components.Processing.FuelComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class CommonFueledProducer extends EnergyBehaviour {
    @Override
    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(context.world, context.pos);
        if(blockRef == null) return false;

        FuelComponent fuelComponent = blockRef.getStore().getComponent(blockRef, ArchStar.get().getFuelComponentType());
        if(fuelComponent == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("Attempted to run CommonFuelProducer but FuelComponent is null! Location: " + context.pos);
            return false;
        }

        //If the block is not maxed on energy
        if(!energyComponent.isMaxed()) {

            //Check if the block is cooking
            if(fuelComponent.isCooking()) {
                //Produce energy
                energyComponent.produceEnergy();
            } else {
                //Consume fuel if available
                fuelComponent.consumeFuel();
            }
        }

        //We have to get this first because decrement will make it false prematurely.
        boolean result = fuelComponent.isCooking();

        //Always decrement cook time
        fuelComponent.decrementCookTime();

        if(energyComponent.getCurrentEnergy() != 0) {
            EnergyNetwork targetNetwork = ArchStar.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            targetNetwork.requestTransaction(EnergyTransaction.SEND, energyComponent);
        }

        return result;
    }
}
