package com.CybaricFox.Components.Energy.EnergyBehaviour;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.Components.Energy.EnergyComponent;

public abstract class EnergyBehaviour{

    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        return false;
    };
}
