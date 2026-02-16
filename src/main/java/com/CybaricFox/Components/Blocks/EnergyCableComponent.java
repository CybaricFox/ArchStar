package com.CybaricFox.Components.Blocks;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class EnergyCableComponent implements Component<ChunkStore> {
    public static final BuilderCodec<EnergyCableComponent> CODEC;

    //Networks may only contain one family of cables.
    private String family = "None";
    //The amount of voltage thats allowed to flow through the network per tick.
    private int maxVoltage = 0;
    //if true, this cable does not care about its family and can be connected anywhere.
    public boolean isUniversal = false;
    //The network this cable is connected to
    private int networkID = -1;

    EnergyCableComponent(){}
    EnergyCableComponent(String family, int maxVoltage, boolean isUniversal, int networkID) {
        this.family = family;
        this.maxVoltage = maxVoltage;
        this.isUniversal = isUniversal;
        this.networkID = networkID;
    }

    public int getNetworkID() {
        return networkID;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new EnergyCableComponent(family, maxVoltage, isUniversal, networkID);
    }

    static {
        CODEC = (BuilderCodec.builder(EnergyCableComponent.class, EnergyCableComponent::new))
                .append(new KeyedCodec<>("Family", Codec.STRING), (component, s) -> component.family = s, (component) -> component.family).add()
                .append(new KeyedCodec<>("MaxVoltage", Codec.INTEGER), (component, s) -> component.maxVoltage = s, (component) -> component.maxVoltage).add()
                .append(new KeyedCodec<>("Universal", Codec.BOOLEAN), (component, s) -> component.isUniversal = s, (component) -> component.isUniversal).add()
                //These should not be touched in JSON. These are save only.
                .build();
    }
}
