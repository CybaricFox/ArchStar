package com.CybaricFox.Modules.ArchMachines.Upgrade;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public abstract class BaseUpgrade {
    private String name;
    private String desc;
    private final String iconPath;
    protected String id;

    //All upgrades that must be bought before this one becomes accessible.
    private final ArrayList<String> prerequisites = new ArrayList<>();

    //List of items needed to purchase this upgrade
    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<Integer> itemQuantities = new ArrayList<>();

    public UpgradeType type = UpgradeType.NOT_SET;

    public BaseUpgrade(UpgradeType type, String name, String desc, String iconPath) {
        this.type = type;
        this.iconPath = iconPath;
        translate(name, desc);
    }

    private void translate(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public void addPrereq(String upgrade) {
        prerequisites.add(upgrade);
    }
    public void addItem(String itemId, int quantity) {
        items.add(itemId);
        itemQuantities.add(quantity);
    }

    //Can this upgrade be purchased?
    public boolean isAvailable(ArrayList<String> purchasedUpgrades, ArrayList<String> allUpgrades) {
        ArrayList<String> temp = new ArrayList<>();

        //Check that the block/item contains the prereq upgrades in its list
        for(String upgrade : prerequisites) {
            if(allUpgrades.contains(upgrade)) {
                temp.add(upgrade);
            }
        }
        //If temp is empty, then the block does not contain any prequisit upgrades, thus ignore the prereqs.
        if(temp.isEmpty()) return true;

        //If temp is not empty, then check that all prereq upgrades the item DOES contain are purchased.
        //Check that the prereqs exist in the items bought upgrades
        for(String upgrade : purchasedUpgrades) {
            temp.remove(upgrade);
        }

        return temp.isEmpty();
    }

    public boolean consumeItems(ItemContainer container) {
        ArrayList<String> tempItems = new ArrayList<>(items);
        ArrayList<Short> slots = new ArrayList<>();

        for(short i = 0; i < container.getCapacity(); i++) {
            ItemStack item = container.getItemStack(i);
            if(item != null) {
                if(tempItems.contains(item.getItem().getId())) {
                    int index = items.indexOf(item.getItem().getId());
                    if(item.getQuantity() >= itemQuantities.get(index)) {
                        tempItems.remove(item.getItem().getId());
                        slots.add(i);
                        if(tempItems.isEmpty()) break;
                    }
                }
            }
        }

        if(tempItems.isEmpty()) {
            for(int i = 0; i < items.size(); i++) {
                for(short s = 0; s < slots.size(); s++) {
                    if(container.getItemStack(slots.get(s)).getItem().getId().equals(items.get(i))) {
                        container.removeItemStackFromSlot(slots.get(s), itemQuantities.get(i));
                        slots.remove(s);
                        break;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void refundItems(ItemContainer container, World world, Vector3i pos) {
        for(int i = 0; i < items.size(); i++) {
            ItemStack itemStack = new ItemStack(items.get(i), itemQuantities.get(i));

            if(container.canAddItemStack(itemStack)) {
                container.addItemStack(itemStack);
            } else {
                ArchLibrary.spawnItems(world, pos, new ArrayList<>(Collections.singleton(itemStack)));
            }
        }
    }

    public String getId() {
        return id;
    }
    public String getName() {return name;}
    public String getDesc() {return desc;}
    public ArrayList<String> getItems() {return items;}
    public String getIconPath() {return iconPath;}

    public int getItemQuantity(String itemId) {
        int index = items.indexOf(itemId);
        if(index == -1) throw new ArrayStoreException(name + " does not contain " + itemId + " in its list of required items!");
        return itemQuantities.get(index);
    }

    //Called when this upgrade is purchased. Use the type to identify whats being upgraded.
    //In most cases, one argument should be null depending on the upgrade type.
    public abstract ItemStack onPurchase(Ref<ChunkStore> blockRef, ItemStack item);

    public abstract ItemStack onUninstall(Ref<ChunkStore> blockRef, ItemStack item);
}
