package com.CybaricFox.Modules.ArchLibrary;

import com.CybaricFox.Modules.ArchLibrary.Interactions.AdvancedDamageInteraction;
import com.CybaricFox.Modules.ArchLibrary.Interactions.SignatureInteraction;
import com.CybaricFox.Modules.ArchMachines.Interactions.RechargeHeldItemInteraction;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import java.util.logging.Level;

public class ArchCoreModule {
    private static ArchCoreModule instance;

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

        ArchLibrary.LOGGER.at(Level.INFO).log("ArchCore Initialized");
    }

    public void start() {
        chooseOreGen();
    }

    //Checks if a compatible ore gen mod is installed.
    private void chooseOreGen() {
        PluginBase oreGenLibrary = PluginManager.get().getPlugin(PluginIdentifier.fromString("DTAPGAMING:OreGenLibrary"));

        if(oreGenLibrary != null) {
            ArchLibrary.LOGGER.at(Level.INFO).log("OreGenLibrary Found. Ores will generate!");
            return;
        }

        ArchLibrary.LOGGER.at(Level.SEVERE).log("Failed to find a compatible ore gen mod! Ores will not generate!");
    }
}
