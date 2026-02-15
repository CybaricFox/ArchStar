package com.CybaricFox.Components.Blocks;

import com.CybaricFox.Components.Helpers.CommonContainerComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class FuelComponent extends CommonContainerComponent {
    public static final BuilderCodec<FuelComponent> CODEC;

    //Is fuel currently processing?
    public boolean isCooking = false;
    //Should this block be allowed to process fuel?
    public boolean isActive = true;
    //The time left in ticks before another fuel is consumed.
    private int cookTimeLeft = 0;
    //The total cook time of the current fuel. Used to calculate the progress bar.
    private int itemCookTime = 0;

    public FuelComponent() {

    }
    public FuelComponent(int actualSize, int maxSize, SimpleItemContainer container, int cookTimeLeft) {
        super(actualSize, maxSize, container);

        this.cookTimeLeft = cookTimeLeft;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new FuelComponent(actualSize, maxSize, container, cookTimeLeft);
    }

    public boolean consumeFuel() {
        short targetSlot = -1;

        //Get the first container that contains an item
        for(short i = 0; i < actualSize; i++) {
            if(getContainer().getItemStack(i) != null) {
                targetSlot = i;
                break;
            }
        }

        //no fuel in machine. Do not process.
        if(targetSlot == -1) return false;

        ItemStack item = container.getItemStack(targetSlot);

        //Check if the item is a type of fuel
        if(item.getItem().getResourceTypes() == null) return false;
        for(ItemResourceType type : item.getItem().getResourceTypes()) {
            if(type.id.equals("Fuel")) {
                double quality = item.getItem().getFuelQuality();
                itemCookTime = (int) quality * 66;
                cookTimeLeft = itemCookTime;
                isCooking = true;

                container.removeItemStackFromSlot(targetSlot, 1);
                isUIUpdated = false;
                return true;
            }
        }

        return false;
    }

    public void decrementCookTime() {
        if(cookTimeLeft > 0) {
            cookTimeLeft--;
        } else {
            isCooking = false;
        }
    }

    private void loadCookTime(int time) {
        cookTimeLeft = time;
        if(time != 0) {
            isCooking = true;
        }
    }

    public float getProgressAsPercentage() {
        if(!isCooking || cookTimeLeft == 0) return 0;

        return 1 / ((float) itemCookTime / cookTimeLeft);
    }

    static {
        CODEC = (BuilderCodec.builder(FuelComponent.class, FuelComponent::new))
                //Common fields
                .append(new KeyedCodec<Integer>("Size", Codec.INTEGER), (component, s) -> component.actualSize = s, (component) -> component.actualSize).add()
                .append(new KeyedCodec<Integer>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()
                .append(new KeyedCodec<SimpleItemContainer>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()

                //Save fields
                .append(new KeyedCodec<Integer>("CookTimeLeft", Codec.INTEGER), FuelComponent::loadCookTime, (component) -> component.cookTimeLeft).add()

                .build();
    }
}
