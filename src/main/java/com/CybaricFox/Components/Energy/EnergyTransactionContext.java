package com.CybaricFox.Components.Energy;

public class EnergyTransactionContext {
    public EnergyTransaction transaction = EnergyTransaction.NOT_SET;
    public EnergyComponent energyComponent;

    public EnergyTransactionContext(EnergyTransaction transaction, EnergyComponent energyComponent) {
        this.transaction = transaction;
        this.energyComponent = energyComponent;
    }
}
