package com.CybaricFox.Components.Conveyors;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.Direction;
import com.CybaricFox.API.DirectionLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class ConveyorImporter {
    private Vector3i machinePos;

    public ConveyorImporter(Direction forwardDirection, Vector3i pos) {
        if(forwardDirection == Direction.NOT_SET || pos == null) {
            return;
        }

        setMachinePos(forwardDirection, pos);
    }

    public void setMachinePos(Direction forwardDirection, Vector3i pos) {
        machinePos = DirectionLibrary.getCoordsFromDirection(DirectionLibrary.getBackwardDirection(forwardDirection), pos);
    }

    public Vector3i getMachinesPos() {
        return machinePos;
    }

    public OutputComponent getOutputComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, ArchStar.get().getOutputComponentType());
    }

    public InputComponent getInputComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, ArchStar.get().getInputComponentType());
    }

    public ProcessingBenchBlock getProcessingComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, ProcessingBenchBlock.getComponentType());
    }

    public ItemContainerBlock getContainerComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, ItemContainerBlock.getComponentType());
    }
}
