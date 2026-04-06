package com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades;

import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class BasicModulationUpgrade extends BaseUpgrade {
    public BasicModulationUpgrade(UpgradeType type, String name, String desc, String iconPath) {
        super(type, name, desc, iconPath);
        id = "Basic_Modulation";
    }

    @Override
    public void onPurchase(Ref<ChunkStore> blockRef, ItemStack item) {

    }

    @Override
    public void onUninstall(Ref<ChunkStore> blockRef, ItemStack item) {

    }
}
