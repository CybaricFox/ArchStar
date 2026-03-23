package com.CybaricFox.Components.Energy;

import com.CybaricFox.Components.CommonContainerComponent;
import com.CybaricFox.UI.Pages.Common.IMachineUIComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class ChargerComponent extends CommonContainerComponent implements IMachineUIComponent {
    public static final BuilderCodec<ChargerComponent> CODEC;

    public ChargerComponent() {

    }
    public ChargerComponent(int actualSize, int maxSize, SimpleItemContainer container) {
        super(actualSize, maxSize, container);

    }

    public float getProgressAsPercentage() {
        ItemStack item = container.getItemStack((short) 0);
        if(item == null) return 0;
        if(item.getMaxDurability() == 0) return 0;

        double progress = item.getDurability();
        double threshold = item.getMaxDurability();

        return (float) ((float) progress / threshold);
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ChargerComponent(actualSize, maxSize, container);
    }

    static {
        CODEC = (BuilderCodec.builder(ChargerComponent.class, ChargerComponent::new))
                //Common fields
                .append(new KeyedCodec<>("Size", Codec.INTEGER), (component, s) -> component.actualSize = s, (component) -> component.actualSize).add()
                .append(new KeyedCodec<>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()
                .append(new KeyedCodec<>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()

                //Required fields

                //Save fields
                .build();
    }

    @Override
    public float getProgress() {
        return getProgressAsPercentage();
    }

    private static int sectionID = 0;
    @Override
    public int getSectionID() {
        if(sectionID == 0) sectionID = setSectionID();
        return sectionID;
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    @Override
    public void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity) {

    }
}
