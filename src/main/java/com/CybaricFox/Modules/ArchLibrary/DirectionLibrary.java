package com.CybaricFox.Modules.ArchLibrary;

import com.CybaricFox.ArchStar;
import org.joml.Vector3i;

import java.util.logging.Level;

public class DirectionLibrary {
    //Returns the forward direction of a block from its rotation index
    public static Direction getForwardDirection(int rotationIndex) {

        Direction direction = Direction.NOT_SET;
        switch(rotationIndex) {
            case 0 -> direction = Direction.NORTH;
            case 1 -> direction = Direction.WEST;
            case 2 -> direction = Direction.SOUTH;
            case 3 -> direction = Direction.EAST;
        }

        if(direction == Direction.NOT_SET) {
            ArchStar.LOGGER.at(Level.WARNING).log("INVALID FORWARD DIRECTION! Expected 0-3, got " + rotationIndex + " instead.");
        }

        return direction;
    }

    public static Direction getBackwardDirection(Direction direction) {
        switch(direction) {
            case NORTH -> direction = Direction.SOUTH;
            case EAST -> direction = Direction.WEST;
            case SOUTH -> direction = Direction.NORTH;
            case WEST -> direction = Direction.EAST;
            case UP -> direction = Direction.DOWN;
            case DOWN -> direction = Direction.UP;
            case NOT_SET -> direction = Direction.NOT_SET;
        }

        if(direction == Direction.NOT_SET) {
            ArchStar.LOGGER.at(Level.WARNING).log("ERROR: Cannot obtain backward direction! INVALID FORWARD DIRECTION: " + direction);
        }

        return direction;
    }

    public static Vector3i getCoordsFromDirection(Direction direction, Vector3i pos) {
        Vector3i targetVector = null;

        switch(direction) {
            case NORTH -> targetVector = new Vector3i(pos.x, pos.y, pos.z - 1);
            case SOUTH -> targetVector = new Vector3i(pos.x, pos.y, pos.z + 1);
            case EAST -> targetVector = new Vector3i(pos.x + 1, pos.y, pos.z);
            case WEST -> targetVector = new Vector3i(pos.x - 1, pos.y, pos.z);
            case UP -> targetVector = new Vector3i(pos.x, pos.y + 1, pos.z);
            case DOWN -> targetVector = new Vector3i(pos.x, pos.y - 1, pos.z);
        }

        return targetVector;
    }
}
