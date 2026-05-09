package com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class ConversionUpgrade extends BaseUpgrade {
    //Id of the item that the item will convert into
    public final String conversionId;

    public ConversionUpgrade(UpgradeType type, String name, String desc, String iconPath, String conversionId, String id) {
        super(type, name, desc, iconPath, id);

        this.conversionId = conversionId;
    }
    public ConversionUpgrade(String id, UpgradeType type) {
        super(id, type);
        conversionId = null;
    }

    @Override
    public ItemStack onPurchase(Ref<ChunkStore> blockRef, ItemStack item) {
        if(conversionId == null) {
            ArchLibrary.LOGGER.at(Level.SEVERE).log("onPurchase was called for a conversion upgrade with no conversion ID set! Upgrade Id: " + id);
            return null;
        }
        ItemStack tempItem = new ItemStack(conversionId, 1);
        if(!tempItem.isValid()) {
            ArchLibrary.LOGGER.at(Level.SEVERE).log(item.getItemId() + " cannot be converted! " + conversionId + " is not a valid item!");
            return null;
        }
        return new ItemStack(conversionId, item.getQuantity(), item.getDurability(), tempItem.getMaxDurability(), null);
    }

    @Override
    public ItemStack onUninstall(Ref<ChunkStore> blockRef, ItemStack item) {
        return null;
    }

    @Override
    public BaseUpgrade parse(JsonObject data){
        //Get data
        String name = data.get("name").getAsString();
        String desc = data.get("desc").getAsString();
        String iconPath = data.get("iconPath").getAsString();
        String id = data.get("id").getAsString();
        String conversionId = data.get("conversionId").getAsString();

        //Load instance
        ConversionUpgrade instance = new ConversionUpgrade(type, name, desc, iconPath, conversionId, id);

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
