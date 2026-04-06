package com.CybaricFox.Modules.ArchTransport;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchTransport.Systems.ConveyorRefSystem;
import com.CybaricFox.Modules.ArchTransport.Systems.ConveyorSystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class ArchTransportModule {
    private static ArchTransportModule instance;

    private ComponentType<ChunkStore, ConveyorComponent> conveyorComponent;

    private final ConveyorRefSystem conveyorRefSystem = new ConveyorRefSystem();

    public ArchTransportModule(JavaPlugin plugin) {
        initialize(plugin);
    }

    public static ArchTransportModule get() {
        if(instance == null) {
            throw new NullPointerException("Called Arch Transport Module before it was instantiated!");
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        ArchLibrary.LOGGER.at(Level.INFO).log("Initializing com.CybaricFox.Modules.ArchTransport");

        instance = this;

        conveyorComponent = plugin.getChunkStoreRegistry().registerComponent(ConveyorComponent.class, "Conveyor", ConveyorComponent.CODEC);

        plugin.getChunkStoreRegistry().registerSystem(conveyorRefSystem);
        plugin.getChunkStoreRegistry().registerSystem(new ConveyorSystem());

        ArchLibrary.LOGGER.at(Level.INFO).log("com.CybaricFox.Modules.ArchTransport Initialized");
    }

    public ComponentType<ChunkStore, ConveyorComponent> getConveyorComponentType() {return conveyorComponent;}

    public ConveyorRefSystem getConveyorRefSystem() {
        return conveyorRefSystem;
    }
}
