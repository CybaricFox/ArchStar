package com.CybaricFox.Systems;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.UI.Pages.CommonPage;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/*
    System for refreshing the Ui on player interaction
 */
public class CommonUIReader extends EntityTickingSystem<EntityStore> {
    @Override
    public void tick(float v, int i, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
        Player player = commandBuffer.getComponent(ref, Player.getComponentType());
        CustomUIPage customPage = player.getPageManager().getCustomPage();

        //Since the handle function doesn't like to work, lets force it to!


        if(customPage instanceof CommonPage commonPage) {
            if(!commonPage.isValid) return;
            commonPage.beginBuildingCycle();

            Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), commonPage.getPos());
            EnergyComponent energyComponent = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());
            if(energyComponent != null) {
                commonPage.refreshEnergy(energyComponent, false);
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
