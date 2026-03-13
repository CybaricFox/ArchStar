package com.CybaricFox.Components.Energy;

import com.CybaricFox.API.EssentialsContext;

public interface IEnergyRunnable {
    boolean run(EssentialsContext context, EnergyComponent energyComponent);
}
