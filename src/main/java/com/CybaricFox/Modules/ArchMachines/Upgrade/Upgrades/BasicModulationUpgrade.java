package com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades;

import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.google.gson.*;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class BasicModulationUpgrade extends BaseUpgrade {
    public BasicModulationUpgrade(UpgradeType type, String name, String desc, String iconPath, String id) {
        super(type, name, desc, iconPath, id);
    }
    public BasicModulationUpgrade(String id, UpgradeType type) {
        super(id, type);
    }

    @Override
    public ItemStack onPurchase(Ref<ChunkStore> blockRef, ItemStack item) {
        return null;
    }

    @Override
    public ItemStack onUninstall(Ref<ChunkStore> blockRef, ItemStack item) {
        return null;
    }

    @Override
    public BaseUpgrade parse(JsonObject data) {
        //Get data
        String name = data.get("name").getAsString();
        String desc = data.get("desc").getAsString();
        String iconPath = data.get("iconPath").getAsString();
        String id = data.get("id").getAsString();

        //Load instance
        BasicModulationUpgrade instance = new BasicModulationUpgrade(type, name, desc, iconPath, id);

        //Load item requirements
        JsonArray array = data.getAsJsonArray("items");

        for(JsonElement element : array) {
            JsonObject item = element.getAsJsonObject();

            String itemId = item.get("id").getAsString();
            int quantity = item.get("quantity").getAsInt();

            instance.addItem(itemId, quantity);
        }

        return instance;
    }
}
