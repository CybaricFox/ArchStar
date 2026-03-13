package com.CybaricFox;

import com.CybaricFox.ComponentSystems.*;
import com.CybaricFox.Components.Conveyors.ConveyorComponent;
import com.CybaricFox.Components.Energy.EnergyBehaviour.Behaviours.CommonConsumer;
import com.CybaricFox.Components.Energy.EnergyBehaviour.Behaviours.CommonFueledProducer;
import com.CybaricFox.Components.Energy.EnergyBehaviourRegistry;
import com.CybaricFox.Components.Energy.EnergyCableComponent;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.FuelComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.CybaricFox.Interactions.DebuggerInteraction;
import com.CybaricFox.Interactions.OpenGeneratorInteraction;
import com.CybaricFox.Interactions.OpenPoweredProcessorInteraction;
import com.CybaricFox.Systems.CommonUIReader;
import com.CybaricFox.Systems.CommonUIUpdater;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class ArchStar extends JavaPlugin {
    private static ArchStar instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    //Block Components
    private ComponentType<ChunkStore, EnergyComponent> energyComponent;
    private ComponentType<ChunkStore, FuelComponent> fuelComponent;
    private ComponentType<ChunkStore, InputComponent> inputComponent;
    private ComponentType<ChunkStore, OutputComponent> outputComponent;
    private ComponentType<ChunkStore, EnergyCableComponent> energyCableComponent;
    private ComponentType<ChunkStore, ConveyorComponent> conveyorComponent;

    private final EnergyRefSystem energyRefSystem = new EnergyRefSystem();
    private final ConveyorRefSystem conveyorRefSystem = new ConveyorRefSystem();

    public ArchStar(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public EnergyRefSystem getEnergyNetworkSystem() {
        return energyRefSystem;
    }
    public ConveyorRefSystem getConveyorPlaceSystem() {return conveyorRefSystem;}

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Beginning ArchStar setup.");

        //Custom registry setups
        setupEnergyBehaviours();

        energyComponent = getChunkStoreRegistry().registerComponent(EnergyComponent.class, "EnergyBlock", EnergyComponent.CODEC);
        fuelComponent = getChunkStoreRegistry().registerComponent(FuelComponent.class, "FuelBlock", FuelComponent.CODEC);
        inputComponent = getChunkStoreRegistry().registerComponent(InputComponent.class, "InputBlock", InputComponent.CODEC);
        outputComponent = getChunkStoreRegistry().registerComponent(OutputComponent.class, "OutputBlock", OutputComponent.CODEC);
        energyCableComponent = getChunkStoreRegistry().registerComponent(EnergyCableComponent.class, "CableBlock", EnergyCableComponent.CODEC);
        conveyorComponent = getChunkStoreRegistry().registerComponent(ConveyorComponent.class, "Conveyor", ConveyorComponent.CODEC);

        getChunkStoreRegistry().registerSystem(energyRefSystem);
        getChunkStoreRegistry().registerSystem(new EnergySystem());
        getChunkStoreRegistry().registerSystem(new CustomProcessingSystem());
        getChunkStoreRegistry().registerSystem(new CustomProcessRefSystem());
        getChunkStoreRegistry().registerSystem(conveyorRefSystem);
        getChunkStoreRegistry().registerSystem(new ConveyorSystem());

        getEntityStoreRegistry().registerSystem(new CommonUIReader());
        getEntityStoreRegistry().registerSystem(new CommonUIUpdater(1));

        //Debug helper
        //FoxLibrary.registerCustomPagePackets(LOGGER);

        //Commands


        //Interactions
        getCodecRegistry(Interaction.CODEC).register("Open_Power_Generator", OpenGeneratorInteraction.class, OpenGeneratorInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register("Open_Powered_Processor", OpenPoweredProcessorInteraction.class, OpenPoweredProcessorInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC).register("Debug_ArchStar_Block", DebuggerInteraction.class, DebuggerInteraction.CODEC);

        //WorldGen
        LOGGER.at(Level.INFO).log("ArchStar setup finished.");
    }

    private void setupEnergyBehaviours() {
        EnergyBehaviourRegistry.register("Solid_Fuel_Generator", CommonFueledProducer::new);
        EnergyBehaviourRegistry.register("Electric_Grinder", CommonConsumer::new);
        EnergyBehaviourRegistry.register("Electric_Furnace", CommonConsumer::new);
    }

    @Override
    protected void start() {
        super.start();

        chooseOreGen();

        //checkForHytalor();
    }

    public ComponentType<ChunkStore, EnergyComponent> getEnergyComponentType() {return energyComponent;}
    public ComponentType<ChunkStore, FuelComponent> getFuelComponentType() {return fuelComponent;}
    public ComponentType<ChunkStore, InputComponent> getInputComponentType() {return inputComponent;}
    public ComponentType<ChunkStore, OutputComponent> getOutputComponentType() {return outputComponent;}
    public ComponentType<ChunkStore, EnergyCableComponent> getEnergyCableComponentType() {return energyCableComponent;}
    public ComponentType<ChunkStore, ConveyorComponent> getConveyorComponentType() {return conveyorComponent;}

    public static ArchStar get() {return instance;}

    //Checks if a compatible ore gen mod is installed.
    private void chooseOreGen() {
        PluginBase oreGenLibrary = PluginManager.get().getPlugin(PluginIdentifier.fromString("DTAPGAMING:OreGenLibrary"));

        if(oreGenLibrary != null) {
            LOGGER.at(Level.INFO).log("OreGenLibrary Found. Ores will generate!");
            return;
        }

        LOGGER.at(Level.SEVERE).log("Failed to find a compatible ore gen mod! Ores will not generate!");
    }

    //Checks if Hytalor is installed
    private void checkForHytalor() {
        PluginBase hytalor = PluginManager.get().getPlugin(PluginIdentifier.fromString("com.hypersonicsharkz:Hytalor"));

        if(hytalor != null) {
            LOGGER.at(Level.INFO).log("Hytalor Found. World Generation Enabled!");
            return;
        }

        LOGGER.at(Level.SEVERE).log("Failed to find Hytalor! World Generation Disabled!");
    }
}



