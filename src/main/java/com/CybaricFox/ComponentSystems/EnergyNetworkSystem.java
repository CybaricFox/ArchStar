package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.EnergyCableComponent;
import com.CybaricFox.Components.Helpers.EnergyBlockType;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPlacementRotationMode;
import com.hypixel.hytale.protocol.BlockRotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockRotationUtil;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/*
    This system checks for when an energy block is created and destroyed.
    It then adds and removes it from its respective energy networks.
 */
public class EnergyNetworkSystem extends RefSystem<ChunkStore>{
    //Map of Ids to energy networks.
    private final ConcurrentHashMap<Integer, EnergyNetwork> energyNetworks = new ConcurrentHashMap<Integer, EnergyNetwork>();

    //The next network that's created will use this id
    private int nextNetworkID = 0;

    public EnergyNetworkSystem() {

    }

    private void addNetwork(EnergyNetwork network) {
        energyNetworks.put(network.getID(), network);
    }

    public void removeNetwork(int id) {
        energyNetworks.remove(id);
    }

    //Create a new network and add it to the map.
    private EnergyNetwork createNetwork(UUID worldUUID) {
        EnergyNetwork network = new EnergyNetwork(nextNetworkID, worldUUID);
        addNetwork(network);
        nextNetworkID++;
        return network;
    }

    //Returns the network with the id
    //Returns null if there is no network with that id
    public EnergyNetwork getNetwork(int id) {
        if(energyNetworks.containsKey(id)) {
            return  energyNetworks.get(id);
        }

        ArchStar.LOGGER.at(Level.WARNING).log("Energy Network " + id + " does not exist.");
        return null;
    }

    //Create a new network and add the entity to it.
    private int createNetwork(Ref<ChunkStore> ref, Vector3i location, WorldChunk chunk, CommandBuffer<ChunkStore> buffer) {
        EnergyNetwork network = createNetwork(chunk.getWorld().getWorldConfig().getUuid());
        network.addEntity(ref, location, buffer);
        addToTick(ref, chunk, buffer, location);

        return network.getID();
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        EssentialsContext context = new EssentialsContext(ref, commandBuffer);
        if(!context.isValid) return;

        EnergyComponent energy = commandBuffer.getComponent(ref, ArchStar.get().getEnergyComponentType());
        EnergyCableComponent cable = commandBuffer.getComponent(ref, ArchStar.get().getEnergyCableComponentType());

        if(energy != null) {
            handleEnergyBlock(ref, commandBuffer, context.pos, context.chunk, context.world);
            changeCableState(ref, commandBuffer, context.pos, true);
        } else if(cable != null) {
            handleCableBlock(ref, commandBuffer, context.pos, context.chunk, context.world, cable);
            changeCableState(ref, commandBuffer, context.pos, true);
        }
    }

    /*
        0 = North
        1 = South
        2 = East
        3 = West
        4 = Up
        5 = Down
     */
    private void changeCableState(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> buffer, Vector3i pos, boolean isInitial) {
        ArrayList<Vector3i> neighbors = getValidNeighbors(buffer.getExternalData().getWorld(), pos, buffer);

        if(!isInitial) {
            ref = FoxLibrary.getBlockEntity(buffer.getExternalData().getWorld(), pos);
        }

        if(neighbors.isEmpty()) {
            FoxLibrary.changeBlockState(ref, buffer, "default");
        }

        ArrayList<String> directions = new ArrayList<>();

        if(neighbors.size() == 1) {
            directions.add(getDirectionString(getDirectionOfNeighbor(pos, neighbors.getFirst())));
        }

        if(neighbors.size() > 1) {
            for(Vector3i neighbor : neighbors) {
                directions.add(getDirectionString(getDirectionOfNeighbor(pos, neighbor)));
            }
        }

        ArrayList<Boolean> sortedList = sortDirections(directions);
        String finalString = "";

        for(int i = 0; i < sortedList.size(); i++) {
            if(sortedList.get(i)) {
                finalString = switch (i) {
                    case 0 -> finalString.concat("North");
                    case 1 -> finalString.concat("South");
                    case 2 -> finalString.concat("East");
                    case 3 -> finalString.concat("West");
                    case 4 -> finalString.concat("Up");
                    case 5 -> finalString.concat("Down");
                    default -> finalString;
                };
            }
        }

        FoxLibrary.changeBlockState(ref, buffer, finalString);

        if(!isInitial || neighbors.isEmpty()) return;
        for(Vector3i neighbor : neighbors) {
            changeCableState(ref, buffer, neighbor, false);
        }
    }

    private ArrayList<Boolean> sortDirections(ArrayList<String> unsorted) {
        ArrayList<Boolean> sorted = new ArrayList<>();

        sorted.add(false);
        sorted.add(false);
        sorted.add(false);
        sorted.add(false);
        sorted.add(false);
        sorted.add(false);

        for(String direction : unsorted) {
            switch(direction) {
                case "North" :
                    sorted.set(0, true);
                    break;
                case "South":
                    sorted.set(1, true);
                    break;
                case "East":
                    sorted.set(2, true);
                    break;
                case "West":
                    sorted.set(3, true);
                    break;
                case "Up":
                    sorted.set(4, true);
                    break;
                case "Down":
                    sorted.set(5, true);
                    break;
            }
        }

        return sorted;
    }

    private String getDirectionString(int direction) {
        return switch (direction) {
            case 1 -> "Up";
            case 2 -> "Down";
            case 3 -> "North";
            case 4 -> "East";
            case 5 -> "South";
            case 6 -> "West";
            default -> null;
        };
    }

    private int getDirectionOfNeighbor(Vector3i pos, Vector3i neighbor) {
        int x = neighbor.x - pos.x;
        int y = neighbor.y - pos.y;
        int z = neighbor.z - pos.z;

        if(x != 0) {
            if(x > 0) {
                return 4;
            } else {
                return 6;
            }
        }
        if(y != 0) {
            if(y > 0) {
                return 1;
            } else {
                return 2;
            }
        }
        if(z != 0) {
            if(z > 0) {
                return 5;
            } else {
                return 3;
            }
        }

        return 0;
    }

    //Handles adding energy blocks to the network system. Systems may need to recalibrate.
    private void handleEnergyBlock(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> commandBuffer, Vector3i location, WorldChunk worldChunk, World world) {
        //Go along every adjacent cable and find the nearest energy block
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);


        //if no neighbors, just create a new network
        if(neighbors.isEmpty()) {
            createNetwork(ref, location, worldChunk, commandBuffer);
            addToTick(ref, worldChunk, commandBuffer, location);
            return;
        }

        int targetNetwork = confirmAndCombineNetworks(neighbors, world, commandBuffer);

        //Finally, add this entity to the network
        if(targetNetwork == -1) {
            createNetwork(ref, location, worldChunk, commandBuffer);
            addToTick(ref, worldChunk, commandBuffer, location);
        } else {
            getNetwork(targetNetwork).addEntity(ref, location, commandBuffer);
            addToTick(ref, worldChunk, commandBuffer, location);
        }
    }

    private void handleCableBlock(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> commandBuffer, Vector3i location, WorldChunk worldChunk, World world, EnergyCableComponent cable) {
        //Go along every adjacent cable and find the nearest energy block
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);

        //if no neighbors, then theres nothing to do here
        if(neighbors.isEmpty() || neighbors.size() == 1) {
            return;
        }

        //Confirm networks are combined if needed
        confirmAndCombineNetworks(neighbors, world, commandBuffer);
    }

    private int confirmAndCombineNetworks(ArrayList<Vector3i> neighbors, World world, CommandBuffer<ChunkStore> commandBuffer) {
        ArrayList<Integer> foundNetworks = new ArrayList<>();

        //Get the network id of every branch of this block
        for(Vector3i neighbor : neighbors) {
            if(getNetworkFromVector(neighbor) == -1) {
                foundNetworks.add(findConnectedNetwork(neighbor, commandBuffer, world));
            } else {
                foundNetworks.add(getNetworkFromVector(neighbor));
            }
        }

        //Check that every branch has the same network id
        int targetNetwork = -1;
        for(Integer network : foundNetworks) {
            if(network == -1) continue;

            //If target network is not set, set it and continue
            if(targetNetwork == -1) {
                targetNetwork = network;
                continue;
            }

            //These branches are connected!
            if(targetNetwork == network) continue;

            //WE ARE COMBINING NETWORKS!
            if(!energyNetworks.containsKey(network)) continue;
            getNetwork(network).pushEntitiesToNetwork(getNetwork(targetNetwork), commandBuffer);
            removeNetwork(network);
        }

        return targetNetwork;
    }

    //Travel across cables until a machine is found with a network id
    private int findConnectedNetwork(Vector3i pos, CommandBuffer<ChunkStore> buffer, World world) {
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, pos, buffer);

        //If no valid neighbors, return
        if(neighbors.isEmpty() || neighbors.size() == 1) {
            return - 1;
        }

        //Iterate over the stack 1 cable at a time. Save all found cables in the set to prevent loops
        ArrayList<Vector3i> stack = new ArrayList<>();
        HashSet<Vector3i> set = new HashSet<>();

        stack.add(pos);
        set.add(pos);

        int targetNetwork = -1;

        while(!stack.isEmpty()) {
            //Get the neighbors of the block in the stack
            ArrayList<Vector3i> newNeighbors = getValidNeighbors(world, stack.getFirst(), buffer);

            //If no neighbors, continue to the next stack
            if(newNeighbors.isEmpty()) {
                stack.removeFirst();
                continue;
            }

            //For every neighbor, check if it is part of a network
            for(Vector3i neighbor : newNeighbors) {
                //Do not check that which we've already checked
                if(set.contains(neighbor)) {
                    continue;
                }

                //Add it to the set to prevent looping
                set.add(neighbor);
                stack.addLast(neighbor);

                //Check if this block is part of a network
                targetNetwork = getNetworkFromVector(neighbor);

                //if so, return it.
                if(targetNetwork != -1) {
                    return targetNetwork;
                }
            }

            stack.removeFirst();
        }

        //No network was found
        return -1;
    }

    private ArrayList<Vector3i> findAllEnergyBlocks(Vector3i pos, World world, CommandBuffer<ChunkStore> buffer, int networkID, Vector3i origin) {
        ArrayList<Vector3i> foundBlocks = new ArrayList<>();

        if(getNetwork(networkID).queryNetwork(pos)) {
            foundBlocks.add(pos);
        }

        //Iterate over the stack 1 cable at a time. Save all found cables in the set to prevent loops

        ArrayList<Vector3i> stack = new ArrayList<>();
        HashSet<Vector3i> set = new HashSet<>();

        stack.add(pos);
        set.add(pos);
        set.add(origin);

        while(!stack.isEmpty()) {
            //Get the neighbors of the block in the stack
            ArrayList<Vector3i> newNeighbors = getValidNeighbors(world, stack.getFirst(), buffer);

            //If no neighbors, continue to the next stack
            if(newNeighbors.isEmpty()) {
                stack.removeFirst();
                continue;
            }

            //For every neighbor, check if it is part of a network
            for(Vector3i neighbor : newNeighbors) {
                //Do not check that which we've already checked
                if(set.contains(neighbor)) {
                    continue;
                }

                //Add it to the set to prevent looping
                set.add(neighbor);
                stack.addLast(neighbor);

                //Check if this block is part of a network
                if(getNetwork(networkID).queryNetwork(neighbor)) {
                    foundBlocks.add(neighbor);
                }
            }

            stack.removeFirst();
        }

        return foundBlocks;
    }

    private int getNetworkFromVector(Vector3i pos) {
        for(EnergyNetwork network : energyNetworks.values()) {
            //Check if the network contains the invalid neighbor, and then merge the networks if it does.
            if(network.queryNetwork(pos)) {
                return network.getID();
            }
        }

        return -1;
    }

    //Adds the block entity to tick
    private void addToTick(Ref<ChunkStore> ref, WorldChunk chunk, CommandBuffer<ChunkStore> buffer, Vector3i location) {
        EnergyComponent energyComponent = buffer.getComponent(ref, ArchStar.get().getEnergyComponentType());

        if(energyComponent.getType() == EnergyBlockType.PRODUCER || energyComponent.getType() == EnergyBlockType.CONSUMER) {
            chunk.setTicking(location.x, location.y, location.z, true);
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        World world = commandBuffer.getExternalData().getWorld();

        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if(info == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("EnergyNetworkSystem: Failed to remove entity! BlockState was null!");
            return;
        }

        WorldChunk worldChunk = commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
        if(worldChunk == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("EnergyNetworkSystem: Failed to remove entity! World was null!");
            return;
        }

        Vector3i location = FoxLibrary.getGlobalCoordsFromChunk(info, worldChunk);

        EnergyComponent energy = commandBuffer.getComponent(ref, ArchStar.get().getEnergyComponentType());
        EnergyCableComponent cable = commandBuffer.getComponent(ref, ArchStar.get().getEnergyCableComponentType());

        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);

        if(energy != null) {
            getNetwork(energy.getNetworkID()).removeEntity(location);
            recalibrateNetwork(energy.getNetworkID(), location, world, commandBuffer);

            for(Vector3i neighbor : neighbors) {
                changeCableState(ref, commandBuffer, neighbor, false);
            }
        } else if(cable != null) {
            recalibrateNetwork(findConnectedNetwork(location, commandBuffer, world), location, world, commandBuffer);
            for(Vector3i neighbor : neighbors) {
                changeCableState(ref, commandBuffer, neighbor, false);
            }
        }

        checkForRemoval();
    }

    //Fixes up a network after an energy block is removed
    private void recalibrateNetwork(int networkID, Vector3i pos, World world, CommandBuffer<ChunkStore> buffer) {
        if(networkID == -1) return;

        int networkSize = getNetwork(networkID).getSize();
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, pos, buffer);

        for(Vector3i neighbor : neighbors) {
            //Find all energy blocks that are connected to this branch
            ArrayList<Vector3i> foundTargets = findAllEnergyBlocks(neighbor, world, buffer, networkID, pos);
            int size = foundTargets.size();

            //All machines are accounted for. No need to recalibrate the network.
            if(networkSize == size) {
                return;
            }

            //Move the found machines to a new network
            networkSize -= size;
            EnergyNetwork network = createNetwork(world.getWorldConfig().getUuid());

            for(Vector3i target : foundTargets) {
                getNetwork(networkID).pushEntityToNetwork(network, target, buffer);
            }
        }

        if(networkSize != 0) {
            ArchStar.LOGGER.at(Level.SEVERE).log(networkID + " was split but network size is not 0! Size leftover: " + networkSize);
        }
    }


    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Query.or(ArchStar.get().getEnergyComponentType(), ArchStar.get().getEnergyCableComponentType()));
    }

    //Returns a list of all valid neighbors
    private ArrayList<Vector3i> getValidNeighbors(World world, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        ArrayList<Vector3i> neighbors = new ArrayList<>();

        //Global coords
        Vector3i up = new Vector3i(location.x, location.y + 1, location.z);
        Vector3i down = new Vector3i(location.x, location.y - 1, location.z);
        Vector3i north = new Vector3i(location.x + 1, location.y, location.z);
        Vector3i east = new Vector3i(location.x, location.y, location.z + 1);
        Vector3i south = new Vector3i(location.x - 1, location.y, location.z);
        Vector3i west = new Vector3i(location.x, location.y, location.z - 1);

        if(isValidNeighbor(world, up, buffer)) {
            neighbors.add(up);
        }
        if(isValidNeighbor(world, down, buffer)) {
            neighbors.add(down);
        }
        if(isValidNeighbor(world, north, buffer)) {
            neighbors.add(north);
        }
        if(isValidNeighbor(world, east, buffer)) {
            neighbors.add(east);
        }
        if(isValidNeighbor(world, south, buffer)) {
            neighbors.add(south);
        }
        if(isValidNeighbor(world, west, buffer)) {
            neighbors.add(west);
        }

        return neighbors;
    }

    //Check that the block at the location is compatible
    private boolean isValidNeighbor(World world, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(world, location);
        if (blockRef == null) {
            return false;
        }

        EnergyComponent energy = buffer.getComponent(blockRef, ArchStar.get().getEnergyComponentType());
        EnergyCableComponent cable = buffer.getComponent(blockRef, ArchStar.get().getEnergyCableComponentType());

        return energy != null || cable != null;
    }

    //Check for any energy networks that need to be removed
    private void checkForRemoval() {
        //These networks will be removed
        ArrayList<Integer> values = new ArrayList<>();

        //For every network, if the network is marked for removal, add it to the array.
        for (EnergyNetwork network : energyNetworks.values()) {
            if (network.markedForRemoval) values.add(network.getID());
        }

        //Delete the array
        for (Integer id : values) {
            energyNetworks.remove(id);
        }
    }
}
