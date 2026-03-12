package com.CybaricFox.Components.Helpers.Conveyors;

import com.CybaricFox.API.Direction;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.server.core.inventory.ItemStack;

public class ConveyorInstance {
    public static final BuilderCodec<ConveyorInstance> CODEC;

    public ItemStack item;
    public Direction from = Direction.NOT_SET;
    public Direction to = Direction.NOT_SET;
    private int transferCooldown = 0;

    public ConveyorInstance(){

    }
    public ConveyorInstance(ItemStack item, Direction to) {
        this.item = item;
        this.to = to;
    }
    public ConveyorInstance(ItemStack item, Direction from, Direction to, int transferCooldown) {
        this.item = item;
        this.from = from;
        this.to = to;
        this.transferCooldown = transferCooldown;
    }


    public int getCooldown() {
        return transferCooldown;
    }

    public void setCooldown(int timer) {
        transferCooldown = timer;
    }

    public ItemStack getItem() {
        return item;
    }

    public void decrementTimer() {
        if(transferCooldown > 0) {
            transferCooldown--;
        }
    }

    public ConveyorInstance clone() {
        return new ConveyorInstance(item, from, to, transferCooldown);
    }

    static {
        CODEC = (BuilderCodec.builder(ConveyorInstance.class, ConveyorInstance::new))
                //Common fields
                .append(new KeyedCodec<>("Item", ItemStack.CODEC), (component, s) -> component.item = s, (component) -> component.item).add()
                .append(new KeyedCodec<>("From", Codec.STRING), (component, s) -> component.from = Direction.valueOf(s.toUpperCase()), (component) -> component.from.toString()).add()
                .append(new KeyedCodec<>("To", Codec.STRING), (component, s) -> component.to = Direction.valueOf(s.toUpperCase()), (component) -> component.to.toString()).add()
                .append(new KeyedCodec<>("TransferCooldown", Codec.INTEGER), (component, s) -> component.transferCooldown = s, (component) -> component.transferCooldown).add()

                .build();
    }
}
