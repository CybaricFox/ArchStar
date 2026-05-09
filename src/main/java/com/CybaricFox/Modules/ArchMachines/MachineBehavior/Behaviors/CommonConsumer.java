package com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors;

import com.CybaricFox.Modules.ArchEnergy.ArchEnergyModule;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchEnergy.EnergyNetwork;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchEnergy.EnergyTransaction;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.PoweredProcessingPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class CommonConsumer extends MachineBehavior {

    public CommonConsumer(String id) {
        super(id);
        setPageRef(PoweredProcessingPage.class);
    }

    @Override
    public boolean run(EssentialsContext context) {
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(context.world, context.pos);

        EnergyComponent energyComponent = blockRef.getStore().getComponent(blockRef, EnergyComponent.getComponentType());
        if(energyComponent == null) {
            return false;
        }

        if(!energyComponent.isMaxed()) {
            EnergyNetwork targetNetwork = ArchEnergyModule.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            targetNetwork.requestTransaction(EnergyTransaction.WITHDRAW, energyComponent);
        }

        return true;
    }

    @Override
    public MachineBehavior createInstance() {
        return new CommonConsumer(getId());
    }
}
