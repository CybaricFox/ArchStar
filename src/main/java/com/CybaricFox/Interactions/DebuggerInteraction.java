package com.CybaricFox.Interactions;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.ConveyorComponent;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorImporter;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorInstance;
import com.CybaricFox.Components.Helpers.Conveyors.ConveyorType;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

public class DebuggerInteraction  extends SimpleBlockInteraction {
    public static final BuilderCodec<DebuggerInteraction> CODEC;

    public DebuggerInteraction() {
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, pos);
        if(blockRef == null) return;

        ConveyorComponent conveyorComponent = blockRef.getStore().getComponent(blockRef, ArchStar.get().getConveyorComponentType());
        if(conveyorComponent != null) {
            conveyorInteraction(playerComponent, conveyorComponent);
        }

        EnergyComponent energyComponent = blockRef.getStore().getComponent(blockRef, ArchStar.get().getEnergyComponentType());
        if(energyComponent != null) {
            energyInteraction(playerComponent, energyComponent);
        }
    }

    private void energyInteraction(Player playerComponent, EnergyComponent energyComponent) {
        playerComponent.sendMessage(Message.raw("================"));
        playerComponent.sendMessage(Message.raw("Type: " + energyComponent.getType().toString()));
        playerComponent.sendMessage(Message.raw("Network ID: " + energyComponent.getNetworkID()));
    }

    private void conveyorInteraction(Player playerComponent, ConveyorComponent conveyorComponent) {
        ArrayList<ConveyorInstance> instances = conveyorComponent.getAllInstances();
        Vector3i targetBlock = conveyorComponent.getTargetBlock();

        playerComponent.sendMessage(Message.raw("================"));
        playerComponent.sendMessage(Message.raw("Type: " + conveyorComponent.getType().toString()));
        playerComponent.sendMessage(Message.raw("Target Block: " + (targetBlock != null ? targetBlock.toString() : "NULL")));

        if(conveyorComponent.getType() == ConveyorType.ROUTER && conveyorComponent.getRouterData() != null) {
            playerComponent.sendMessage(Message.raw("-----"));
            playerComponent.sendMessage(Message.raw("Router Data:"));
            playerComponent.sendMessage(Message.raw("Last Direction: " + conveyorComponent.getRouterData().getLastDirection()));
            playerComponent.sendMessage(Message.raw("Number of Outs: " + conveyorComponent.getRouterData().getNumberOfOuts()));
        }
        else if (conveyorComponent.getType() == ConveyorType.ROUTER && conveyorComponent.getRouterData() == null) {
            playerComponent.sendMessage(Message.raw("Router Data is not yet validated."));
        }

        playerComponent.sendMessage(Message.raw("-----"));
        playerComponent.sendMessage(Message.raw("Conveyor Instances:"));
        for(ConveyorInstance instance : instances) {
            playerComponent.sendMessage(Message.raw("ItemStack: " + instance.getItem().toString()));
            playerComponent.sendMessage(Message.raw("Transfer Cooldown: " + instance.getCooldown()));
        }
        playerComponent.sendMessage(Message.raw("Total Items: " + conveyorComponent.getAllInstances().size() + "/" + conveyorComponent.getMaxSize()));
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    static {
        CODEC = (BuilderCodec.builder(DebuggerInteraction.class, DebuggerInteraction::new, SimpleBlockInteraction.CODEC)).build();
    }
}

