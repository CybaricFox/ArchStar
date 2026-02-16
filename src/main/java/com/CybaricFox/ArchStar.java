package com.CybaricFox;

import com.CybaricFox.ComponentSystems.*;
import com.CybaricFox.Components.Blocks.*;
import com.CybaricFox.Interactions.OpenGeneratorInteraction;
import com.CybaricFox.Interactions.OpenPoweredProcessorInteraction;
import com.CybaricFox.Systems.CommonUIReader;
import com.CybaricFox.Systems.CommonUIUpdater;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;


public class ArchStar extends JavaPlugin {
    private static ArchStar instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    //Block Components
    private ComponentType<ChunkStore, EnergyComponent> energyComponent;
    private ComponentType<ChunkStore, FuelComponent> fuelComponent;
    private ComponentType<ChunkStore, InputComponent> inputComponent;
    private ComponentType<ChunkStore, OutputComponent> outputComponent;
    private ComponentType<ChunkStore, EnergyCableComponent> energyCableComponent;

    private final EnergyNetworkSystem energyNetworkSystem = new EnergyNetworkSystem();

    public ArchStar(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public EnergyNetworkSystem getEnergyNetworkSystem() {
        return energyNetworkSystem;
    }

    @Override
    protected void setup() {
        energyComponent = getChunkStoreRegistry().registerComponent(EnergyComponent.class, "EnergyBlock", EnergyComponent.CODEC);
        fuelComponent = getChunkStoreRegistry().registerComponent(FuelComponent.class, "FuelBlock", FuelComponent.CODEC);
        inputComponent = getChunkStoreRegistry().registerComponent(InputComponent.class, "InputBlock", InputComponent.CODEC);
        outputComponent = getChunkStoreRegistry().registerComponent(OutputComponent.class, "OutputBlock", OutputComponent.CODEC);
        energyCableComponent = getChunkStoreRegistry().registerComponent(EnergyCableComponent.class, "CableBlock", EnergyCableComponent.CODEC);

        getChunkStoreRegistry().registerSystem(energyNetworkSystem);
        getChunkStoreRegistry().registerSystem(new EnergySystem());
        getChunkStoreRegistry().registerSystem(new CustomProcessingSystem());
        getChunkStoreRegistry().registerSystem(new CustomProcessSystem());

        getEntityStoreRegistry().registerSystem(new CommonUIReader());
        getEntityStoreRegistry().registerSystem(new CommonUIUpdater(1));

        //Debug helper
        //FoxLibrary.registerCustomPagePackets(LOGGER);

        //Commands


        //Interactions
        getCodecRegistry(Interaction.CODEC).register("Open_Power_Generator", OpenGeneratorInteraction.class, OpenGeneratorInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register("Open_Powered_Processor", OpenPoweredProcessorInteraction.class, OpenPoweredProcessorInteraction.CODEC);

        //getBlockStateRegistry().registerBlockState(GeneratorState.class, "generatorBench", GeneratorState.CODEC);\
    }



    public ComponentType<ChunkStore, EnergyComponent> getEnergyComponentType() {return energyComponent;}
    public ComponentType<ChunkStore, FuelComponent> getFuelComponentType() {return fuelComponent;}
    public ComponentType<ChunkStore, InputComponent> getInputComponentType() {return inputComponent;}
    public ComponentType<ChunkStore, OutputComponent> getOutputComponentType() {return outputComponent;}
    public ComponentType<ChunkStore, EnergyCableComponent> getEnergyCableComponentType() {return energyCableComponent;}

    public static ArchStar get() {return instance;}
}



