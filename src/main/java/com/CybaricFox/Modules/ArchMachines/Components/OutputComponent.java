package com.CybaricFox.Modules.ArchMachines.Components;

import com.CybaricFox.Modules.ArchMachines.ArchMachinesModule;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.Common.IMachineUIComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class OutputComponent extends CommonContainerComponent implements IMachineUIComponent {
    public static final BuilderCodec<OutputComponent> CODEC;

    public OutputComponent() {

    }
    public OutputComponent(int actualSize, int maxSize, SimpleItemContainer container) {
        super(actualSize, maxSize, container);
    }

    public short getSlotWithFirstItem() {
        for(short i = 0; i < getCapacity(); i++) {
            if(container.getItemStack(i) != null) {
                return i;
            }
        }

        return -1;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new OutputComponent(actualSize, maxSize, container);
    }

    public static ComponentType<ChunkStore, OutputComponent> getComponentType() {
        return ArchMachinesModule.get().getOutputComponentType();
    }

    static {
        CODEC = (BuilderCodec.builder(OutputComponent.class, OutputComponent::new))
                //Common fields
                .append(new KeyedCodec<Integer>("Size", Codec.INTEGER), (component, s) -> component.actualSize = s, (component) -> component.actualSize).add()
                .append(new KeyedCodec<Integer>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()
                .append(new KeyedCodec<SimpleItemContainer>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()

                .build();
    }

    @Override
    public float getProgress() {
        return -1.0f;
    }

    private static int sectionID = 0;
    @Override
    public int getSectionID() {
        if(sectionID == 0) sectionID = setSectionID();
        return sectionID;
    }

    @Override
    public boolean canInsert() {
        return false;
    }

    @Override
    public void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity) {

    }
}
