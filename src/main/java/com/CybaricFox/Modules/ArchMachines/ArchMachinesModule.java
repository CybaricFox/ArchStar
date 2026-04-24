package com.CybaricFox.Modules.ArchMachines;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.*;
import com.CybaricFox.Modules.ArchMachines.Interactions.OpenMachineInteraction;
import com.CybaricFox.Modules.ArchMachines.Interactions.RechargeHeldItemInteraction;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors.CommonCapacitor;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors.CommonConsumer;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors.CommonFueledProducer;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors.UpgradeStation;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehaviorRegistry;
import com.CybaricFox.Modules.ArchMachines.Systems.CommonUIReader;
import com.CybaricFox.Modules.ArchMachines.Systems.CustomProcessRefSystem;
import com.CybaricFox.Modules.ArchMachines.Systems.CustomProcessingSystem;
import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.Upgrade.UpgradeRegistry;
import com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades.BasicModulationUpgrade;
import com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades.ConversionUpgrade;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ArchMachinesModule {
    private static ArchMachinesModule instance;

    private ComponentType<ChunkStore, FuelComponent> fuelComponent;
    private ComponentType<ChunkStore, InputComponent> inputComponent;
    private ComponentType<ChunkStore, OutputComponent> outputComponent;
    private ComponentType<ChunkStore, ChargerComponent> chargerComponent;
    private ComponentType<ChunkStore, MachineBehaviorComponent> machineComponent;

    public ArchMachinesModule(JavaPlugin plugin) {
        initialize(plugin);
    }

    public static ArchMachinesModule get() {
        if(instance == null) {
            throw new NullPointerException("Called Arch Machines Module before it was instantiated!");
        }
        return instance;
    }

    public void initialize(JavaPlugin plugin) {
        ArchLibrary.LOGGER.at(Level.INFO).log("Initializing com.CybaricFox.Modules.ArchMachines");

        instance = this;

        fuelComponent = plugin.getChunkStoreRegistry().registerComponent(FuelComponent.class, "FuelBlock", FuelComponent.CODEC);
        inputComponent = plugin.getChunkStoreRegistry().registerComponent(InputComponent.class, "InputBlock", InputComponent.CODEC);
        outputComponent = plugin.getChunkStoreRegistry().registerComponent(OutputComponent.class, "OutputBlock", OutputComponent.CODEC);
        chargerComponent = plugin.getChunkStoreRegistry().registerComponent(ChargerComponent.class, "ChargerBlock", ChargerComponent.CODEC);
        machineComponent = plugin.getChunkStoreRegistry().registerComponent(MachineBehaviorComponent.class, "MachineBehavior", MachineBehaviorComponent.CODEC);

        plugin.getChunkStoreRegistry().registerSystem(new CustomProcessingSystem());
        plugin.getChunkStoreRegistry().registerSystem(new CustomProcessRefSystem());

        plugin.getEntityStoreRegistry().registerSystem(new CommonUIReader());

        plugin.getCodecRegistry(Interaction.CODEC).register("Open_Machine", OpenMachineInteraction.class, OpenMachineInteraction.CODEC);
        plugin.getCodecRegistry(Interaction.CODEC).register("Recharge_Held_Item", RechargeHeldItemInteraction.class, RechargeHeldItemInteraction.CODEC);

        setupMachineBehaviors();
        setupUpgrades();
        fixCustomUI();

        ArchLibrary.LOGGER.at(Level.INFO).log("com.CybaricFox.Modules.ArchMachines Initialized");
    }

    public ComponentType<ChunkStore, FuelComponent> getFuelComponentType() {return fuelComponent;}
    public ComponentType<ChunkStore, InputComponent> getInputComponentType() {return inputComponent;}
    public ComponentType<ChunkStore, OutputComponent> getOutputComponentType() {return outputComponent;}
    public ComponentType<ChunkStore, ChargerComponent> getChargerComponentType() {return chargerComponent;}
    public ComponentType<ChunkStore, MachineBehaviorComponent> getMachineComponentType() {return machineComponent;}

    private void setupMachineBehaviors() {
        MachineBehaviorRegistry.register("Solid_Fuel_Generator", CommonFueledProducer::new);
        MachineBehaviorRegistry.register("Electric_Grinder", CommonConsumer::new);
        MachineBehaviorRegistry.register("Electric_Furnace", CommonConsumer::new);
        MachineBehaviorRegistry.register("Capacitor", CommonCapacitor::new);
        MachineBehaviorRegistry.register("Upgrade_Station", UpgradeStation::new);
    }

    private void setupUpgrades() {
        BaseUpgrade upgrade;
        //Register the upgrades
        upgrade = UpgradeRegistry.registerUpgrade(new BasicModulationUpgrade(UpgradeType.BLOCK, "Basic Modulation", "Unlocks basic upgrades and allows machine information to be displayed in the information panel.", "Icons/ItemCategories/Circuit_Icon.png"));
        upgrade.addItem("Basic_Circuit", 1);

        //Item Upgrades
        upgrade = UpgradeRegistry.registerUpgrade(new ConversionUpgrade(
                UpgradeType.ITEM,
                "Cobalt Drill",
                "Converts the steel drill into a cobalt drill. Increases its base stats to that of a cobalt pickaxe." +
                        "\nDue to the affects of magic on power systems, more energy is consumed per operation to counteract the close presence of cobalt." +
                        "\nPower Drain 25v/op -> 40v/op",
                "ArchStarResources/UpgradeIcons/Cobalt_Drill_Conversion_Icon.png",
                "Cobalt_Drill"));
        upgrade.addItem("Ingredient_Bar_Cobalt", 6);
        upgrade = UpgradeRegistry.registerUpgrade(new ConversionUpgrade(
                UpgradeType.ITEM,
                "Adamantite Drill",
                "Converts the cobalt drill into an adamantite drill. Increases its base stats to that of an adamantite pickaxe." +
                        "\nWhile the weight of adamantite may make this drill powerful, the energy needed to rev up the engine is far more expensive compared to its cobalt counterpart." +
                        "\n Power Drain 40v/op -> 75v/op",
                "ArchStarResources/UpgradeIcons/Adamantite_Drill_Conversion_Icon.png",
                "Adamantite_Drill"));
        upgrade.addItem("Ingredient_Bar_Adamantite", 6);

        //Register what items have what upgrades
        //Items can have both block and item upgrades, but each will only appear under certain conditions
        //Blocks are upgraded in the machine ui
        //Items are upgraded in the upgrade station ui
        UpgradeRegistry.registerItem("Solid_Fuel_Generator", new ArrayList<>(List.of("Basic_Modulation")));
        UpgradeRegistry.registerItem("Electric_Furnace", new ArrayList<>(List.of("Basic_Modulation")));
        UpgradeRegistry.registerItem("Electric_Grinder", new ArrayList<>(List.of("Basic_Modulation")));
        UpgradeRegistry.registerItem("Capacitor", new ArrayList<>(List.of("Basic_Modulation")));
        UpgradeRegistry.registerItem("Steel_Drill", new ArrayList<>(List.of("Cobalt_Drill")));
        UpgradeRegistry.registerItem("Cobalt_Drill", new ArrayList<>(List.of("Adamantite_Drill")));
    }

    //FIXES THE GOD DAMN ITEM GRID NOT PLAYING WELL WITH CUSTOM UIS!!!
    private void fixCustomUI() {
        PacketAdapters.registerInbound((PlayerPacketFilter) (player, packet) -> {
            if(player.getReference() == null) return false;
            player.getReference().getStore().getExternalData().getWorld().execute(() -> {
                if(packet instanceof CustomPageEvent customPageEvent && customPageEvent.data != null) {
                    //Ensure this only affects ArchStar events!
                    if(customPageEvent.data.contains("ARCH-SIG")) {
                        String alteredData = customPageEvent.data.replace("ARCH-SIG", "ARCH-PRO");
                        Ref<EntityStore> playerRef = player.getReference();
                        Player playerComponent = playerRef.getStore().getComponent(playerRef, Player.getComponentType());

                        playerComponent.getPageManager().getCustomPage().handleDataEvent(playerRef, playerRef.getStore(), alteredData);
                    }
                }
            });
            return false;
        });
    }
}
