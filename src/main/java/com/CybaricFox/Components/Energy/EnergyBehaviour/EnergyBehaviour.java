package com.CybaricFox.Components.Energy.EnergyBehaviour;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.Components.Energy.EnergyComponent;

//ATTENTION
//THIS CLASS WILL BE DEPRECATED IN THE FUTURE! ProcessBehaviour simply makes far more sense than EnergyBehaviour.
//However, there are no energyless machines currently, so I'll ignore this for now.
public abstract class EnergyBehaviour{

    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        return false;
    };
}
