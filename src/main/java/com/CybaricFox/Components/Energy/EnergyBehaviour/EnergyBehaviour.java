package com.CybaricFox.Components.Energy.EnergyBehaviour;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Energy.IEnergyRunnable;

public abstract class EnergyBehaviour implements IEnergyRunnable {

    @Override
    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        return false;
    };
}
