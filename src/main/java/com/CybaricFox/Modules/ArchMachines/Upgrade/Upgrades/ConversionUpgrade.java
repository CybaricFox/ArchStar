package com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.logging.Level;

public class ConversionUpgrade extends BaseUpgrade {
    public final String conversionId;

    public ConversionUpgrade(UpgradeType type, String name, String desc, String iconPath, String conversionId) {
        super(type, name, desc, iconPath);

        this.id = conversionId;
        this.conversionId = conversionId;
    }

    @Override
    public ItemStack onPurchase(Ref<ChunkStore> blockRef, ItemStack item) {
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
}
