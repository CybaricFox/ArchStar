package com.CybaricFox.Components.Energy.EnergyBehaviour.Behaviours;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.ChargerComponent;
import com.CybaricFox.Components.Energy.EnergyBehaviour.EnergyBehaviour;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;


public class CommonCapacitor extends EnergyBehaviour {
    @Override
    public boolean run(EssentialsContext context, EnergyComponent energyComponent) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(context.world, context.pos);

        //Charge items in input
        ChargerComponent chargerComponent = ref.getStore().getComponent(ref, ArchStar.get().getChargerComponentType());
        if(chargerComponent == null) return false;

        int capacitorPercent = Math.round((float) energyComponent.getMaxEnergy() / 100);

        for(short i = 0; i < chargerComponent.getCapacity(); i++) {
            ItemStack item = chargerComponent.getItemStack(i);

            if(item != null) {
                //Charge the item based on the lower 1% between the capacitor and the item
                int itemPercent = (int) Math.min(capacitorPercent, Math.round(item.getMaxDurability() / 100));

                for(String tag : item.getItem().getData().getRawTags().get("Type")) {
                    if(tag.equals("Powered_Item") || tag.equals("Battery")) {
                        int amount = energyComponent.transferEnergy(itemPercent, (int) (item.getMaxDurability() - item.getDurability()));
                        item = item.withIncreasedDurability(amount);

                        if(item.getDurability() == item.getMaxDurability()) {
                            //There may not be an output component
                            OutputComponent outputComponent = ref.getStore().getComponent(ref, ArchStar.get().getOutputComponentType());
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

        return true;
    }
}
