package com.CybaricFox;

import com.CybaricFox.Modules.ArchEnergy.ArchEnergyModule;
import com.CybaricFox.Modules.ArchLibrary.ArchCoreModule;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.ArchMachinesModule;
import com.CybaricFox.Modules.ArchTransport.ArchTransportModule;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class ArchStar extends JavaPlugin {
    private static ArchStar instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public ArchStar(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Beginning ArchStar setup.");

        //Load com.CybaricFox.Modules.ArchLibrary
        ArchLibrary.setLogger(LOGGER);
        new ArchCoreModule(this);
        ArchLibrary.activateDebug = false;

        //Load Modules in order
        new ArchEnergyModule(this);
        new ArchMachinesModule(this);
        new ArchTransportModule(this);

        //Debug helper
        //com.CybaricFox.Modules.ArchLibrary.registerCustomPagePackets(LOGGER);

        LOGGER.at(Level.INFO).log("ArchStar setup finished.");
    }

    @Override
    protected void start() {
        super.start();

        ArchCoreModule.get().start();

        //checkForHytalor();
    }

    public static ArchStar get() {return instance;}

    //Checks if Hytalor is installed
    //Not currently required
    private void checkForHytalor() {
        PluginBase hytalor = PluginManager.get().getPlugin(PluginIdentifier.fromString("com.hypersonicsharkz:Hytalor"));

        if(hytalor != null) {
            LOGGER.at(Level.INFO).log("Hytalor Found. World Generation Enabled!");
            return;
        }

        LOGGER.at(Level.SEVERE).log("Failed to find Hytalor! World Generation Disabled!");
    }
}



