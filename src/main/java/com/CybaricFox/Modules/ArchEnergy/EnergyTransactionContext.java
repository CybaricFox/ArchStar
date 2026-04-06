package com.CybaricFox.Modules.ArchEnergy;

import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;

public class EnergyTransactionContext {
    public EnergyTransaction transaction = EnergyTransaction.NOT_SET;
    public EnergyComponent energyComponent;

    public EnergyTransactionContext(EnergyTransaction transaction, EnergyComponent energyComponent) {
        this.transaction = transaction;
        this.energyComponent = energyComponent;
    }
}
