package com.CybaricFox.Components.Helpers;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class CommonContainerComponent implements Component<ChunkStore> {
    protected SimpleItemContainer container;

    protected int actualSize = 0;
    protected int maxSize = 0;

    public boolean isUIUpdated = false;

    public CommonContainerComponent() {

    }
    public CommonContainerComponent(int actualSize, int maxSize, SimpleItemContainer container) {
        this.actualSize = actualSize;
        this.maxSize = maxSize;

        if(actualSize > maxSize) {
            this.actualSize = this.maxSize;
        }

        setContainer(container);
    }

    private void setContainer(SimpleItemContainer container) {
        if(container == null) {
            this.container = new SimpleItemContainer((short) actualSize);
        } else {
            this.container = container;
        }
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new CommonContainerComponent(actualSize, maxSize, container);
    }

    public SimpleItemContainer getContainer() {
        if(container == null) {
            setContainer(null);
        }

        return container;
    }

    public int getCapacity() {
        return getContainer().getCapacity();
    }

    public ItemStack getItemStack(short slot) {
        return getContainer().getItemStack(slot);
    }

    //Returns the first slot that has the item
    public short getFirstSlotWithItem(String itemId) {
        for(short i = 0; i < getCapacity(); i++) {
            if(container.getItemStack(i) == null) continue;

            if(container.getItemStack(i).getItemId().equals(itemId)) {
                return i;
            }
        }

        return -1;
    }
}
