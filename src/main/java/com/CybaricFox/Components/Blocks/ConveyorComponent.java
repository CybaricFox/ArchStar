package com.CybaricFox.Components.Blocks;

import com.CybaricFox.Components.Helpers.ConveyorState;
import com.CybaricFox.Components.Helpers.ConveyorType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ConveyorComponent implements Component<ChunkStore> {
    public static final BuilderCodec<ConveyorComponent> CODEC;
    private final int baseTimer = 150;

    private SimpleItemContainer container;
    //How much longer before the item is transferred.
    private int timer = 0;
    private int timerIO = 0;
    private Vector3i targetBlock;
    private Vector3i targetMachine;
    public ConveyorState state = ConveyorState.VOLATILE;

    private ConveyorType type = ConveyorType.NOTSET;
    //A multiplier to the timer
    //Tier I = 1.0 (5s by default)
    //Tier II =
    //Tier III =
    private float speedMultiplier = 1.0f;
    //The value the timer will be reset to. This value is set dynamically.

    private int maxTimer = baseTimer;

    public ConveyorComponent() {
        setMaxTimer();
    }
    public ConveyorComponent(SimpleItemContainer container, ConveyorType type, float speedMultiplier, int timer, int timerIO) {
        this.type = type;
        this.speedMultiplier = speedMultiplier;
        setMaxTimer();
        this.timer = timer;
        this.timerIO = timerIO;

        setContainer(container);
    }

    private void setContainer(SimpleItemContainer container) {
        this.container = Objects.requireNonNullElseGet(container, () -> new SimpleItemContainer((short) 2));
    }

    public ConveyorType getType() {
        return type;
    }

    public boolean hasItem() {
        return !container.isEmpty();
    }

    private void setMaxTimer() {
        maxTimer = Math.round(baseTimer * speedMultiplier);
    }

    public void startTimer() {
        timer = maxTimer;
    }

    public void startIOTimer() {
        timerIO = maxTimer;
    }

    public void decrementTimer() {
        if(timer > 0) {
            timer--;
        }
        if(timerIO > 0) {
            timerIO--;
        }
    }

    public int getTimer() {
        return timer;
    }

    public int getIOTimer() {
        return timerIO;
    }

    public ItemStack getItem() {
        return container.getItemStack((short) 0);
    }

    public ItemStack getIOItem() {
        return container.getItemStack((short) 1);
    }

    public void removeItem() {
        container.removeItemStackFromSlot((short) 0);
    }

    public void removeIOItem() {
        container.removeItemStackFromSlot((short) 1);
    }

    public void setItem(ItemStack item) {
        container.addItemStackToSlot((short) 0, item);
    }

    public void setIOItem(ItemStack item) {
        container.addItemStackToSlot((short) 1, item);
    }

    public void setTargetBlock(Vector3i target) {
        targetBlock = target;
    }

    public Vector3i getTargetBlock() {
        return targetBlock;
    }

    public Vector3i getTargetMachine() {
        return targetMachine;
    }

    public void setTargetMachine(Vector3i target) {
        targetMachine = target;
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new ConveyorComponent(container, type, speedMultiplier, timer, timerIO);
    }

    static {
        CODEC = (BuilderCodec.builder(ConveyorComponent.class, ConveyorComponent::new))
                //Required fields
                .append(new KeyedCodec<>("Type", Codec.STRING), (component, s) -> component.type = ConveyorType.valueOf(s.toUpperCase()), (component) -> component.type.toString()).add()
                .append(new KeyedCodec<>("SpeedMultiplier", Codec.FLOAT), (component, s) -> component.speedMultiplier = s, (component) -> component.speedMultiplier).add()

                //Common fields
                .append(new KeyedCodec<>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()
                .append(new KeyedCodec<>("Timer", Codec.INTEGER), (component, s) -> component.timer = s, (component) -> component.timer).add()
                .append(new KeyedCodec<>("TimerIO", Codec.INTEGER), (component, s) -> component.timerIO = s, (component) -> component.timerIO).add()
                .append(new KeyedCodec<>("TargetBlock", Vector3i.CODEC), (component, s) -> component.targetBlock = s, (component) -> component.targetBlock).add()
                .append(new KeyedCodec<>("TargetMachine", Vector3i.CODEC), (component, s) -> component.targetMachine = s, (component) -> component.targetMachine).add()

                .build();
    }
}
