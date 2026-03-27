package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Processing.FuelComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.MachineBehaviorComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.CybaricFox.Components.CommonContainerComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/*
    This system handles powered processing blocks being destroyed.
 */
public class CustomProcessRefSystem extends RefSystem<ChunkStore> {
    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        MachineBehaviorComponent machineBehaviorComponent = commandBuffer.getComponent(ref, ArchStar.get().getMachineComponent());
        if(machineBehaviorComponent == null) return;

        EssentialsContext context = new EssentialsContext(ref, commandBuffer);

        machineBehaviorComponent.setMachineBehavior(context.world.getBlockType(context.pos).getItem().getId());
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        //Only call when the block is destroyed, not unloaded
        if(removeReason == RemoveReason.REMOVE) {
            EssentialsContext context = new EssentialsContext(ref, commandBuffer);

            //Batch items so only one call to the world thread is needed
            ArrayList<ItemStack> items = new ArrayList<>();

            FuelComponent fuel = commandBuffer.getComponent(ref, ArchStar.get().getFuelComponentType());
            InputComponent input = commandBuffer.getComponent(ref, ArchStar.get().getInputComponentType());
            OutputComponent output = commandBuffer.getComponent(ref, ArchStar.get().getOutputComponentType());

            items = dropItems(fuel, items);
            items = dropItems(input, items);
            items = dropItems(output, items);

            ArchLibrary.spawnItems(context.world, context.pos, items);
        }
    }

    //Adds the items from the components container to the array of items that will be dropped.
    private ArrayList<ItemStack> dropItems(CommonContainerComponent component, ArrayList<ItemStack> items) {
        if(component == null) return items;

        for(short i = 0; i < component.getCapacity(); i++) {
            ItemStack item = component.getItemStack(i);

            if(item == null) continue;

            items.add(item);
        }

        return items;
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.or(
                        ArchStar.get().getInputComponentType(),
                        ArchStar.get().getOutputComponentType(),
                        ArchStar.get().getFuelComponentType(),
                        ArchStar.get().getMachineComponent());
    }
}
