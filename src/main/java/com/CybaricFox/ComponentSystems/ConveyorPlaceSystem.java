package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.Direction;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.ConveyorComponent;
import com.CybaricFox.Components.Helpers.ConveyorState;
import com.CybaricFox.Components.Helpers.ConveyorType;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

public class ConveyorPlaceSystem extends RefSystem<ChunkStore> {
    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason addReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        ConveyorComponent conveyorComponent = commandBuffer.getComponent(ref, ArchStar.get().getConveyorComponentType());
        if(conveyorComponent == null) return;

        EssentialsContext context = new EssentialsContext(ref, commandBuffer);
        if(!context.isValid) return;

        //Import conveyors must always tick
        if(conveyorComponent.getType() == ConveyorType.IMPORT) {
            changeTickState(context.pos, commandBuffer, true);
        }

        //Finished setting up component. Change state.
        if(conveyorComponent.hasItem()) {
            changeTickState(context.pos, commandBuffer, true);
            if(conveyorComponent.getTimer() == 0) {
                conveyorComponent.state = ConveyorState.WAIT;
            } else {
                conveyorComponent.state = ConveyorState.TRANSFER;
            }
        } else {
            changeTickState(context.pos, commandBuffer, false);
            conveyorComponent.state = ConveyorState.OPEN;
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.or(ArchStar.get().getConveyorComponentType());
    }

    public void changeTickState(Vector3i pos, CommandBuffer<ChunkStore> commandBuffer, boolean ticking) {
        Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(commandBuffer.getExternalData().getWorld(), pos);
        if(blockRef == null) return;

        EssentialsContext context = new EssentialsContext(blockRef, commandBuffer);

        //Confirm that the given pos has a conveyor component. Do not change tick state unless it does.
        ConveyorComponent conveyorComponent = commandBuffer.getComponent(blockRef, ArchStar.get().getConveyorComponentType());
        if(conveyorComponent == null) return;

        //Importer must always tick
        if(!ticking && conveyorComponent.getType() == ConveyorType.IMPORT) return;

        Vector3i localCoords = FoxLibrary.convertToLocalCoords(context.pos);

        context.chunk.setTicking(pos.x, pos.y, pos.z, ticking);
    }
}
