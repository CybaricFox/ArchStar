package com.CybaricFox.Modules.ArchTransport;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.Direction;
import com.CybaricFox.Modules.ArchLibrary.DirectionLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.FuelComponent;
import com.CybaricFox.Modules.ArchMachines.Components.InputComponent;
import com.CybaricFox.Modules.ArchMachines.Components.OutputComponent;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
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
        return buffer.getComponent(ref, OutputComponent.getComponentType());
    }

    public InputComponent getInputComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, InputComponent.getComponentType());
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

    public FuelComponent getFuelComponent(CommandBuffer<ChunkStore> buffer, World world) {
        Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, machinePos);
        if(ref == null) return null;
        return buffer.getComponent(ref, FuelComponent.getComponentType());
    }
}
