package com.CybaricFox.Components.Processing.MachineBehavior.Behaviors;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Energy.EnergyTransaction;
import com.CybaricFox.Components.Processing.MachineBehavior.MachineBehavior;
import com.CybaricFox.UI.Pages.PoweredProcessingPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.HashMap;

public class CommonConsumer extends MachineBehavior {

    public CommonConsumer() {
        setPageRef(PoweredProcessingPage.class);
    }

    @Override
    public boolean run(EssentialsContext context) {
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(context.world, context.pos);

        EnergyComponent energyComponent = blockRef.getStore().getComponent(blockRef, ArchStar.get().getEnergyComponentType());
        if(energyComponent == null) {
            return false;
        }

        if(!energyComponent.isMaxed()) {
            EnergyNetwork targetNetwork = ArchStar.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            targetNetwork.requestTransaction(EnergyTransaction.WITHDRAW, energyComponent);
        }

        return true;
    }
}
