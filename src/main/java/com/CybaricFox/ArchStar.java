package com.CybaricFox;

import com.CybaricFox.Modules.ArchEnergy.ArchEnergyModule;
import com.CybaricFox.Modules.ArchLibrary.ArchCoreModule;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.ArchMachinesModule;
import com.CybaricFox.Modules.ArchTransport.ArchTransportModule;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

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
    }

    public static ArchStar get() {return instance;}
}



