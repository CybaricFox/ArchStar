package com.CybaricFox.Components.Energy.EnergyBehaviour.Behaviours;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.Components.Energy.EnergyBehaviour.EnergyBehaviour;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Energy.EnergyTransaction;

public class CommonConsumer extends EnergyBehaviour {
    @Override
    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        if(!energyComponent.isMaxed()) {
            EnergyNetwork targetNetwork = ArchStar.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            targetNetwork.requestTransaction(EnergyTransaction.WITHDRAW, energyComponent);
        }

        return true;
    }
}
