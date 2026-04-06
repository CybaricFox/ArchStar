package com.CybaricFox.Modules.ArchMachines.MachineBehavior.Behaviors;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchMachines.Components.ChargerComponent;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.Components.OutputComponent;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.ChargerPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class CommonCapacitor extends MachineBehavior {

    public CommonCapacitor() {
        setPageRef(ChargerPage.class);
    }

    @Override
    public boolean run(EssentialsContext context) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(context.world, context.pos);

        EnergyComponent energyComponent = ref.getStore().getComponent(ref, EnergyComponent.getComponentType());
        if(energyComponent == null) return false;
        //Charge items in input
        ChargerComponent chargerComponent = ref.getStore().getComponent(ref, ChargerComponent.getComponentType());
        if(chargerComponent == null) return false;

        int capacitorPercent = Math.round((float) energyComponent.getMaxEnergy() / 100);

        for(short i = 0; i < chargerComponent.getCapacity(); i++) {
            ItemStack item = chargerComponent.getItemStack(i);

            if(item != null) {
                //Charge the item based on the lower 1% between the capacitor and the item
                int itemPercent = (int) Math.min(capacitorPercent, Math.round(item.getMaxDurability() / 100));

                if(!item.getItem().getData().getRawTags().isEmpty()) {
                    for(String tag : item.getItem().getData().getRawTags().get("Type")) {
                        if(tag.equals("Powered_Item") || tag.equals("Battery")) {
                            int amount = energyComponent.transferEnergy(itemPercent, (int) (item.getMaxDurability() - item.getDurability()));
                            item = item.withIncreasedDurability(amount);

                            if(item.getDurability() == item.getMaxDurability()) {
                                //There may not be an output component
                                OutputComponent outputComponent = ref.getStore().getComponent(ref, OutputComponent.getComponentType());
                                if(outputComponent == null) {
                                    chargerComponent.getContainer().setItemStackForSlot(i, item);
                                    break;
                                }

                                if(outputComponent.getContainer().canAddItemStack(item)) {
                                    chargerComponent.getContainer().removeItemStackFromSlot(i);
                                    outputComponent.getContainer().addItemStack(item);
                                    break;
                                }
                            }

                            chargerComponent.getContainer().setItemStackForSlot(i, item);
                            break;
                        }
                    }
                }
            }
        }

        return true;
    }
}
