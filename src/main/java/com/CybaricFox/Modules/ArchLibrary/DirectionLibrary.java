package com.CybaricFox.Modules.ArchLibrary;

import com.CybaricFox.ArchStar;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.math.vector.Vector3dUtil;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3f;
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

    public static Vector3i raycastBlockFromPlayer(Ref<EntityStore> entity, World world) {
        PlayerRef player = entity.getStore().getComponent(entity, PlayerRef.getComponentType());
        if(player == null) return null;

        Vector3d direction = convertRotationToVector3D(player.getHeadRotation());
        Vector3d pos = new Vector3d(player.getTransform().getPosition().x, player.getTransform().getPosition().y + 1.6, player.getTransform().getPosition().z);

        int stepX = direction.x() > 0 ? 1 : -1;
        int stepY = direction.y() > 0 ? 1 : -1;
        int stepZ = direction.z() > 0 ? 1 : -1;

        double maxX = intBound(pos.x, direction.x);
        double maxY = intBound(pos.y, direction.y);
        double maxZ = intBound(pos.z, direction.z);

        double deltaX = direction.x == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / direction.x);
        double deltaY = direction.y == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / direction.y);
        double deltaZ = direction.z == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / direction.z);

        Vector3i currentPos = Vector3dUtil.toVector3i(pos);
        double distance = 0.0;

        while(distance <= 16) {
            BlockType block = world.getBlockType(currentPos);

            if(block != BlockType.EMPTY) {
                //Vector3d hitPoint = new Vector3d(pos.x + direction.x * distance, pos.y + direction.y * distance, pos.z + direction.z * distance);
                //DebugUtils.addLine(world, pos, hitPoint, new Vector3f(255, 0, 0), 0.1, 3, 0);
                return currentPos;
            }

            if (maxX < maxY) {
                if (maxX < maxZ) {
                    currentPos.x += stepX;
                    distance = maxX;
                    maxX += deltaX;
                } else {
                    currentPos.z += stepZ;
                    distance = maxZ;
                    maxZ += deltaZ;
                }
            } else {
                if (maxY < maxZ) {
                    currentPos.y += stepY;
                    distance = maxY;
                    maxY += deltaY;
                } else {
                    currentPos.z += stepZ;
                    distance = maxZ;
                    maxZ += deltaZ;
                }
            }
        }

        return null;
    }

    private static Vector3d convertRotationToVector3D(Rotation3f rotation) {
        double yaw = rotation.yaw();
        double pitch = rotation.pitch();

        //yaw = Math.toRadians(yaw);
        //pitch = Math.toRadians(pitch);

        double x = -Math.sin(yaw) * Math.cos(pitch);
        double y = Math.sin(pitch);
        double z = -Math.cos(yaw) * Math.cos(pitch);

        return new Vector3d(x, y, z).normalize();
    }

    private static double intBound(double s, double ds) {
        if (ds == 0) {
            return Double.POSITIVE_INFINITY;
        }

        if (ds > 0) {
            return (Math.floor(s + 1) - s) / ds;
        } else {
            return (s - Math.floor(s)) / -ds;
        }
    }
}
