package com.CybaricFox.Modules.ArchLibrary;

import com.CybaricFox.Modules.ArchLibrary.Interactions.AdvancedDamageInteraction;
import com.CybaricFox.Modules.ArchLibrary.Interactions.SignatureInteraction;
import com.CybaricFox.Modules.ArchLibrary.OreGeneration.OreGenSystem;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;

import java.util.logging.Level;

public class ArchCoreModule {
    private static ArchCoreModule instance;
    private final OreGenSystem oreGenSystem = new OreGenSystem();

    public ArchCoreModule(JavaPlugin plugin) {
        initialize(plugin);
    }

    public static ArchCoreModule get() {
        if(instance == null) {
            throw new NullPointerException("Called Arch Core Module before it was instantiated!");
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        ArchLibrary.LOGGER.at(Level.INFO).log("Initializing ArchCore");

        instance = this;

        plugin.getCodecRegistry(Interaction.CODEC).register("Debug_ArchStar_Block", DebuggerInteraction.class, DebuggerInteraction.CODEC);
        plugin.getCodecRegistry(Interaction.CODEC).register("Signature", SignatureInteraction.class, SignatureInteraction.CODEC);
        plugin.getCodecRegistry(Interaction.CODEC).register("ArchStar_Advanced_Damage", AdvancedDamageInteraction.class, AdvancedDamageInteraction.CODEC);

        plugin.getChunkStoreRegistry().registerSystem(oreGenSystem);
        plugin.getEventRegistry().registerGlobal(ChunkPreLoadProcessEvent.class, oreGenSystem.getOreGenerator()::processChunk);

        ArchLibrary.LOGGER.at(Level.INFO).log("ArchCore Initialized");
    }
}
