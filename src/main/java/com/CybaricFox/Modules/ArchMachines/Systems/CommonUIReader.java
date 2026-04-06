package com.CybaricFox.Modules.ArchMachines.Systems;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.CommonPage;
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
    System for refreshing the Ui on player interaction
 */
public class CommonUIReader extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        CustomUIPage customPage = player.getPageManager().getCustomPage();

        if(customPage instanceof CommonPage commonPage) {
            if(!commonPage.isValid) return;
            commonPage.beginBuildingCycle();

            Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), commonPage.getPos());
            EnergyComponent energyComponent = block.getStore().getComponent(block, EnergyComponent.getComponentType());
            if(energyComponent != null) {
                commonPage.refreshEnergy(energyComponent, false);
            }

            MachineBehaviorComponent behaviorComponent = block.getStore().getComponent(block, MachineBehaviorComponent.getComponentType());
            if(behaviorComponent != null && behaviorComponent.displayData()) {
                commonPage.refreshData(behaviorComponent);
            }

            commonPage.refreshAllUI();
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Query.or(Player.getComponentType());
    }
}
