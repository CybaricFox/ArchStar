package com.CybaricFox.Components.Energy;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Processing.MachineBehavior.MachineBehavior;
import com.CybaricFox.Components.Processing.MachineBehavior.MachineBehaviorRegistry;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.logging.Level;

//Component that contains data on energy
public class EnergyComponent implements Component<ChunkStore> {
    public static final BuilderCodec<EnergyComponent> CODEC;

    public int getMaxEnergy() {
        return maxEnergy;
    }

    public int getCurrentEnergy() {
        return currentEnergy;
    }

    public int getInputRate() {
        return inputRate;
    }

    public int getOutputRate() {
        return outputRate;
    }

    public int getNetworkID() {
        return networkID;
    }

    public boolean isMaxed() {
        return currentEnergy >= maxEnergy;
    }

    public int getRemaining() {
        return maxEnergy - currentEnergy;
    }

    //Producers use this to determine the amount of energy to produce per tick
    //Consumers use this to determine the amount of energy to consume per tick
    public int getProductionRate() {return productionRate;}

    public EnergyBlockType getType(){return type;}

    private int maxEnergy = 0;
    private int currentEnergy = 0;
    private int inputRate = 0;
    private int outputRate = 0;
    private int productionRate = 0;
    private int outputThisTick = 0;
    private int inputThisTick = 0;
    private int networkID = -1;
    private EnergyBlockType type = EnergyBlockType.NOT_SET;

    //Create component with default values
    public EnergyComponent(){

    }
    //Used for cloning
    public EnergyComponent(int maxEnergy, int currentEnergy, int inputRate, int outputRate, int networkID, EnergyBlockType type, int productionRate) {
        this.maxEnergy = maxEnergy;
        this.currentEnergy = currentEnergy;
        this.inputRate = inputRate;
        this.outputRate = outputRate;
        this.productionRate = productionRate;
        this.networkID = networkID;
        this.type = type;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new EnergyComponent(this.maxEnergy, this.currentEnergy, this.inputRate, this.outputRate, this.networkID, this.type, this.productionRate);
    }

    public void setNetworkID(int id) {
        networkID = id;
    }

    //Used by producers
    public void produceEnergy() {
        if(currentEnergy + productionRate <= maxEnergy) {
            currentEnergy += productionRate;
        } else {
            currentEnergy = maxEnergy;
        }
    }

    public boolean consumeEnergy() {
        if(currentEnergy - productionRate >= 0) {
            currentEnergy -= productionRate;
            return true;
        } else {
            return false;
        }
    }

    //Used by not producers
    public void addEnergy(int amount) {
        if(currentEnergy + amount <= maxEnergy) {
            currentEnergy += amount;
        } else {
            currentEnergy = maxEnergy;
        }
    }

    public void addInputThisTick(int amount) {
        inputThisTick += amount;

        if(inputThisTick > inputRate) {
            ArchStar.LOGGER.at(Level.WARNING).log("A machine accepted more energy than it can in one 1 tick! Max input per tick: " + inputRate + ". Energy this tick: " + inputThisTick);
        }
    }

    //Remove energy as a result of transfer
    public int transferEnergy(int inputOfTarget, int remaining) {
        //Cannot output more than rate in 1 tick
        //Cannot output if there is no energy
        if(outputThisTick >= outputRate || currentEnergy == 0) {
            return 0;
        }

        int targetAmount = outputRate - outputThisTick;

        if(targetAmount > remaining) targetAmount = remaining;
        if(targetAmount > inputOfTarget) targetAmount = inputOfTarget;
        if(targetAmount > currentEnergy) targetAmount = currentEnergy;

        currentEnergy -= targetAmount;
        outputThisTick += targetAmount;
        return targetAmount;
    }

    public void resetIOThisTick() {
        outputThisTick = 0;
        inputThisTick = 0;
    }

    public int getOutputThisTick() {
        return outputThisTick;
    }

    public int getInputThisTick() {
        return inputThisTick;
    }

    static {
        CODEC = (BuilderCodec.builder(EnergyComponent.class, EnergyComponent::new))
                .append(new KeyedCodec<>("MaxEnergy", Codec.INTEGER), (component, s) -> component.maxEnergy = s, (component) -> component.maxEnergy).add()
                .append(new KeyedCodec<>("InputRate", Codec.INTEGER), (component, s) -> component.inputRate = s, (component) -> component.inputRate).add()
                .append(new KeyedCodec<>("OutputRate", Codec.INTEGER), (component, s) -> component.outputRate = s, (component) -> component.outputRate).add()
                .append(new KeyedCodec<>("ProductionRate", Codec.INTEGER), (component, s) -> component.productionRate = s, (component) -> component.productionRate).add()
                .append(new KeyedCodec<>("Type", Codec.STRING), (component, s) -> component.type = EnergyBlockType.valueOf(s.toUpperCase()), (component) -> component.type.toString()).add()
                //These should not be touched in JSON. These are save only.
                .append(new KeyedCodec<>("CurrentEnergy", Codec.INTEGER), (component, s) -> component.currentEnergy = s, (component) -> component.currentEnergy).add()
                .build();
    }
}
