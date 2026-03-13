package com.CybaricFox.Components.Conveyors;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.Direction;
import com.CybaricFox.API.DirectionLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
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

    @SuppressWarnings("deprecation")
    public ItemContainerState getItemContainer(World world) {
        if (world.getState(machinePos.x, machinePos.y, machinePos.z, true) instanceof ItemContainerState state) {
            return state;
        }

        return null;
    }

    @SuppressWarnings("deprecation")
    public ProcessingBenchState getBenchState(World world) {
        if (world.getState(machinePos.x, machinePos.y, machinePos.z, true) instanceof ProcessingBenchState state) {
            return state;
        }

        return null;
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
}
