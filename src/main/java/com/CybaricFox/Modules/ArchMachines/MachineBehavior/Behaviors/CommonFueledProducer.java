package com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors;

import com.CybaricFox.Modules.ArchEnergy.ArchEnergyModule;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchEnergy.EnergyNetwork;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchEnergy.EnergyTransaction;
import com.CybaricFox.Modules.ArchMachines.Components.FuelComponent;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.GeneratorPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class CommonFueledProducer extends MachineBehavior {

    public CommonFueledProducer(String id) {
        super(id);
        setPageRef(GeneratorPage.class);
    }

    @Override
    public boolean run(EssentialsContext context) {
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(context.world, context.pos);
        if(blockRef == null) return false;

        EnergyComponent energyComponent = blockRef.getStore().getComponent(blockRef, EnergyComponent.getComponentType());
        if(energyComponent == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("Attempted to run CommonFuelProducer but EnergyComponent is null! Location: " + context.pos);
            return false;
        }

        FuelComponent fuelComponent = blockRef.getStore().getComponent(blockRef, FuelComponent.getComponentType());
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
            EnergyNetwork targetNetwork = ArchEnergyModule.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            targetNetwork.requestTransaction(EnergyTransaction.SEND, energyComponent);
        }

        return result;
    }

    @Override
    public MachineBehavior createInstance() {
        return new CommonFueledProducer(getId());
    }
}
