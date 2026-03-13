package com.CybaricFox.Components.Processing;

import com.CybaricFox.Components.CommonContainerComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class OutputComponent extends CommonContainerComponent {
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

    static {
        CODEC = (BuilderCodec.builder(OutputComponent.class, OutputComponent::new))
                //Common fields
                .append(new KeyedCodec<Integer>("Size", Codec.INTEGER), (component, s) -> component.actualSize = s, (component) -> component.actualSize).add()
                .append(new KeyedCodec<Integer>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()
                .append(new KeyedCodec<SimpleItemContainer>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()

                .build();
    }
}
