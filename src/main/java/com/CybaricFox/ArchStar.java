package com.CybaricFox;

import com.CybaricFox.ComponentSystems.*;
import com.CybaricFox.Components.Blocks.*;
import com.CybaricFox.Interactions.OpenGeneratorInteraction;
import com.CybaricFox.Interactions.OpenPoweredProcessorInteraction;
import com.CybaricFox.Systems.CommonUIReader;
import com.CybaricFox.Systems.CommonUIUpdater;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
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
        LOGGER.at(Level.INFO).log("Beginning ArchStar setup.");

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

        //WorldGen
        LOGGER.at(Level.INFO).log("ArchStar setup finished.");
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



