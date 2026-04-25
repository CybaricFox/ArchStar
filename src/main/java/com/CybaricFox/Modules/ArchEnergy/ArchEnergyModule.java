package com.CybaricFox.Modules.ArchEnergy;

import com.CybaricFox.Modules.ArchEnergy.Components.EnergyCableComponent;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchEnergy.Systems.EnergyRefSystem;
import com.CybaricFox.Modules.ArchEnergy.Systems.EnergySystem;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class ArchEnergyModule {
    private static ArchEnergyModule instance;

    private ComponentType<ChunkStore, EnergyComponent> energyComponent;
    private ComponentType<ChunkStore, EnergyCableComponent> energyCableComponent;

    private final EnergyRefSystem energyRefSystem = new EnergyRefSystem();

    public ArchEnergyModule(JavaPlugin plugin) {
        initialize(plugin);
    }

    public static ArchEnergyModule get() {
        if(instance == null) {
            throw new NullPointerException("Called Arch Energy Module before it was instantiated!");
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        ArchLibrary.LOGGER.at(Level.INFO).log("Initializing ArchEnergy");

        instance = this;

        energyComponent = plugin.getChunkStoreRegistry().registerComponent(EnergyComponent.class, "EnergyBlock", EnergyComponent.CODEC);
        energyCableComponent = plugin.getChunkStoreRegistry().registerComponent(EnergyCableComponent.class, "CableBlock", EnergyCableComponent.CODEC);

        plugin.getChunkStoreRegistry().registerSystem(energyRefSystem);
        plugin.getChunkStoreRegistry().registerSystem(new EnergySystem());

        ArchLibrary.LOGGER.at(Level.INFO).log("ArchEnergy Initialized");
    }

    public ComponentType<ChunkStore, EnergyComponent> getEnergyComponentType() {return energyComponent;}
    public ComponentType<ChunkStore, EnergyCableComponent> getEnergyCableComponentType() {return energyCableComponent;}

    public EnergyRefSystem getEnergyNetworkSystem() {
        return energyRefSystem;
    }
}
