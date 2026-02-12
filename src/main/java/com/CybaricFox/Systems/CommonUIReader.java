package com.CybaricFox.Systems;

import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.CybaricFox.Components.Blocks.FuelComponent;
import com.CybaricFox.Components.Blocks.InputComponent;
import com.CybaricFox.Components.Blocks.OutputComponent;
import com.CybaricFox.UI.Pages.CommonPage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
    System for refreshing UI information
 */
public class CommonUIReader extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        CustomUIPage customPage = player.getPageManager().getCustomPage();

        if(customPage instanceof CommonPage) {
            Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), ((CommonPage) customPage).getPos());

            EnergyComponent energyComponent = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());
            FuelComponent fuelComponent = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());
            InputComponent inputComponent = block.getStore().getComponent(block, ArchStar.get().getInputComponentType());
            OutputComponent outputComponent = block.getStore().getComponent(block, ArchStar.get().getOutputComponentType());

            if(energyComponent != null) {
                ((CommonPage) customPage).refreshEnergy(energyComponent.getCurrentEnergy(), energyComponent.getMaxEnergy());
            }
            if(fuelComponent != null) {
                if(!fuelComponent.isUIUpdated) {
                    ((CommonPage) customPage).refreshFuelUI(player.getReference(), null);
                    fuelComponent.isUIUpdated = true;
                }
            }
            if(inputComponent != null) {
                if(!inputComponent.isUIUpdated) {
                    ((CommonPage) customPage).refreshInputUI(player.getReference(), null);
                    inputComponent.isUIUpdated = true;
                }
            }
            if(outputComponent != null) {
                if(!outputComponent.isUIUpdated) {
                    ((CommonPage) customPage).refreshOutputUI(player.getReference(), null);
                    outputComponent.isUIUpdated = true;
                }
            }
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(Player.getComponentType());
    }
}
