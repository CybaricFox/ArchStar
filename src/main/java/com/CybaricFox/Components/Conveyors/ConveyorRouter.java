package com.CybaricFox.Components.Conveyors;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.Direction;
import com.CybaricFox.ArchStar;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.ArrayList;

public class ConveyorRouter {
    private Direction last = Direction.NOT_SET;
    private ArrayList<Direction> out = new ArrayList<>();

    public boolean noPath = false;

    public ConveyorRouter(Vector3i pos, World world) {
        setOUT(pos, world);
    }

    public Direction getLastDirection() {
        return last;
    }

    public void setOUT(Vector3i pos, World world) {
        out.clear();

        ArrayList<Vector3i> neighbors = ArchLibrary.getNeighborVectors(pos);

        for(int i = 0; i < 6; i++) {
            Ref<ChunkStore> ref = ArchLibrary.getBlockEntity(world, neighbors.get(i));
            if(ref == null) continue;

            ConveyorComponent conveyorComponent = ref.getStore().getComponent(ref, ArchStar.get().getConveyorComponentType());
            if(conveyorComponent == null) continue;

            if(conveyorComponent.getTargetBlock() != null && conveyorComponent.getTargetBlock().equals(pos)) continue;

            switch(i) {
                case 0 -> out.add(Direction.NORTH);
                case 1 -> out.add(Direction.SOUTH);
                case 2 -> out.add(Direction.EAST);
                case 3 -> out.add(Direction.WEST);
                case 4 -> out.add(Direction.UP);
                case 5 -> out.add(Direction.DOWN);
            }
        }
    }

    public boolean noOut() {
        return out.isEmpty();
    }

    public int getNumberOfOuts() {
        return out.size();
    }

    public Direction getNextDirection(Direction from) {
        if(out.isEmpty()) {
            last = Direction.NOT_SET;
            noPath = true;

            return last;
        }

        if(last == Direction.NOT_SET) {
            last = out.getFirst();
            noPath = false;

            if(last == from && out.size() > 1) {
                last = out.get(1);
                noPath = false;
            }

            return last;
        }

        noPath = false;

        boolean foundLast = false;

        for(Direction direction : out) {
            if(foundLast) {
                last = direction;
                if(last == from) {
                    continue;
                }
                return last;
            }
            if(direction == last) {
                foundLast = true;
            }
        }

        last = out.getFirst();
        if(last == from) {
            if(out.size() != 1) {
                last = out.get(1);
            }
        }
        return last;
    }
}
