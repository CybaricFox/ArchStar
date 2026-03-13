package com.CybaricFox.Systems;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.FuelComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.CybaricFox.UI.Pages.CommonPage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
    System for refreshing the Ui on player interaction
 */
public class CommonUIReader extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        CustomUIPage customPage = player.getPageManager().getCustomPage();

        if(customPage instanceof CommonPage commonPage) {
            Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), commonPage.getPos());

            EnergyComponent energyComponent = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());
            FuelComponent fuelComponent = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());
            InputComponent inputComponent = block.getStore().getComponent(block, ArchStar.get().getInputComponentType());
            OutputComponent outputComponent = block.getStore().getComponent(block, ArchStar.get().getOutputComponentType());

            UICommandBuilder builder = new UICommandBuilder();
            boolean sendUpdate = false;

            if(energyComponent != null) {

            }
            if(fuelComponent != null) {
                if(!fuelComponent.isUIUpdated) {
                    commonPage.refreshFuelUI(fuelComponent, builder);
                    commonPage.refreshProgressBar(fuelComponent, builder);
                    fuelComponent.isUIUpdated = true;
                    sendUpdate = true;
                }
            }
            if(inputComponent != null) {
                if(!inputComponent.isUIUpdated) {
                    commonPage.refreshInputUI(inputComponent, builder);
                    commonPage.refreshProgressBar(inputComponent, builder);
                    inputComponent.isUIUpdated = true;
                    sendUpdate = true;
                }
            }
            if(outputComponent != null) {
                if(!outputComponent.isUIUpdated) {
                    commonPage.refreshOutputUI(outputComponent, builder);
                    outputComponent.isUIUpdated = true;
                    sendUpdate = true;
                }
            }

            if(sendUpdate) commonPage.sendBuilder(builder);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(Player.getComponentType());
    }
}
