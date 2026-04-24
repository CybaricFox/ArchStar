package com.CybaricFox.Modules.ArchMachines.Components;

import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchMachines.ArchMachinesModule;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehaviorRegistry;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.CommonPage;
import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.Upgrade.UpgradeRegistry;
import com.CybaricFox.Modules.ArchTransport.ConveyorInstance;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MachineBehaviorComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MachineBehaviorComponent> CODEC;

    private MachineBehavior machineBehavior;
    private ArrayList<String> boughtUpgrades = new ArrayList<>();
    private String openSound;

    public MachineBehaviorComponent() {

    }
    public MachineBehaviorComponent(ArrayList<String> boughtUpgrades, String openSound) {
        this.boughtUpgrades = new ArrayList<>(boughtUpgrades);
        this.openSound = openSound;
    }

    public boolean displayData() {
        return boughtUpgrades.contains("Basic_Modulation");
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new MachineBehaviorComponent(boughtUpgrades, openSound);
    }

    public void setMachineBehavior(String blockId) {
        machineBehavior = MachineBehaviorRegistry.create(blockId);
    }

    public CommonPage getPage(PlayerRef playerRef, BuilderCodec<CommonPage.CommonData> data, Vector3i pos) {
        return machineBehavior.getPage(playerRef, data, pos);
    }

    public boolean run(EssentialsContext context) {
        if(machineBehavior == null) {
            throw new NullPointerException("Machine Block behaviour not set! Attempted to run the behaviour before setting it!");
        }

        return machineBehavior.run(context);
    }

    public String getOpenSound(){return openSound;}

    public boolean containsUpgrade(String id) {
        return boughtUpgrades.contains(id);
    }

    public void purchaseUpgrade(String upgradeId, Ref<ChunkStore> blockRef) {
        if(boughtUpgrades.contains(upgradeId)) {
            boughtUpgrades.remove(upgradeId);
            UpgradeRegistry.getUpgrade(upgradeId).onUninstall(blockRef, null);
        } else {
            boughtUpgrades.add(upgradeId);
            UpgradeRegistry.getUpgrade(upgradeId).onPurchase(blockRef, null);
        }
    }
    public ItemStack purchaseUpgrade(String upgradeId, ItemStack item) {
        return UpgradeRegistry.getUpgrade(upgradeId).onPurchase(null, item);
    }

    public static ComponentType<ChunkStore, MachineBehaviorComponent> getComponentType() {
        return ArchMachinesModule.get().getMachineComponentType();
    }

    public ArrayList<ItemStack> onRemoval() {
        ArrayList<ItemStack> itemStacks = new ArrayList<>();

        for(String upgrade : boughtUpgrades) {
            BaseUpgrade baseUpgrade = UpgradeRegistry.getUpgrade(upgrade);

            for(int i = 0; i < baseUpgrade.getItems().size(); i++) {
                ItemStack itemStack = new ItemStack(baseUpgrade.getItems().get(i), baseUpgrade.getItemQuantity(baseUpgrade.getItems().get(i)));
                itemStacks.add(itemStack);
            }
        }

        return itemStacks;
    }

    static {
        CODEC = (BuilderCodec.builder(MachineBehaviorComponent.class, MachineBehaviorComponent::new))
                .append(new KeyedCodec<>("OpenSound", Codec.STRING), (component, s) -> component.openSound = s, (component) -> component.openSound).add()

                .append(new KeyedCodec<>("BoughtUpgrades", new ArrayCodec<>(Codec.STRING, String[]::new)), (component, s) -> component.boughtUpgrades = new ArrayList<>(Arrays.asList(s)), (component) -> component.boughtUpgrades.toArray(String[]::new)).add()
                .build();
    }
}
