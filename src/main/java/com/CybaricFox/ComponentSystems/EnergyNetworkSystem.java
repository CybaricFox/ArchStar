package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ComponentSystems.HelperClasses.EnergyBlockContext;
import com.CybaricFox.ComponentSystems.HelperClasses.EnergyNetwork;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.EnergyBlockType;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
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

    //This queue may not be necessary but I already made it so
    //Queues creation of energy blocks on chunk load to prevent issues
    private final ArrayList<EnergyBlockContext> queue = new ArrayList<>();

    //The next network that's created will use this id
    private int nextNetworkID = 0;

    private boolean isQueueRunning = false;

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
    private void createNetwork(Ref<ChunkStore> ref, Vector3i location, WorldChunk chunk, CommandBuffer<ChunkStore> buffer) {
        createNetwork(chunk.getWorld().getWorldConfig().getUuid()).addEntity(ref, location, buffer);
        addToTick(ref, chunk, buffer, location);
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull AddReason reason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        queue.add(new EnergyBlockContext(ref, commandBuffer));

        //So queue may not be necessary but fuck it lol
        if(!isQueueRunning) {
            isQueueRunning = true;
            processQueue();
        }
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

        //* 32 converts from local chunk coords to global coords
        int x = ChunkUtil.xFromBlockInColumn(info.getIndex()) + (worldChunk.getX() * 32);
        int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int z = ChunkUtil.zFromBlockInColumn(info.getIndex()) + (worldChunk.getZ() * 32);

        Vector3i location = new Vector3i(x, y, z);

        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);

        int targetnetwork = -1;

        //Find the network that contains the block entity
        for(EnergyNetwork network : energyNetworks.values()) {
            if(network.queryNetwork(location)) {
                targetnetwork = network.getID();
            }
        }

        //Remove the entity
        energyNetworks.get(targetnetwork).removeEntity(location);

        //If this block has neighbors, recalibrate the network
        if(!neighbors.isEmpty() && neighbors.size() > 1) {
            recalibrateNetwork(targetnetwork, neighbors, world, commandBuffer);
        }

        checkForRemoval();
    }

    //Fixes up a network after an energy block is removed
    private void recalibrateNetwork(int networkID, ArrayList<Vector3i> targets, World world, CommandBuffer<ChunkStore> buffer) {
        HashSet<Vector3i> originNetwork = new HashSet<>(); //These entities are still part of the same network
        HashSet<Vector3i> remaining = new HashSet<>(targets); //These are the entities left to check
        ArrayList<Vector3i> toCheck = new ArrayList<>(); //This is a stack that checks each entity in order

        originNetwork.add(targets.getFirst()); //The first entity is always part of the network
        remaining.remove(targets.getFirst()); //We can safely remove the first entity
        toCheck.add(targets.getFirst()); //We need to start with the first entity

        //While the stack is not empty and there are still entities left to check
        while(!toCheck.isEmpty() && !remaining.isEmpty()) {
            //Get this entities neighbors
            ArrayList<Vector3i> neighbors = getValidNeighbors(world, toCheck.getFirst(), buffer);

            //If the network does not already contain the neighbor, add it to the stack
            for(Vector3i neighbor : neighbors) {
                if(!originNetwork.contains(neighbor)) {
                    toCheck.add(neighbor);
                }
            }

            //We can safely add all neighbors to the network
            originNetwork.addAll(neighbors);

            //If a target entity is a neighbor, we do not need to check if the entity is still part of the network.
            for(Vector3i target : targets) {
                if(neighbors.contains(target)) {
                    remaining.remove(target);
                }
            }

            //Remove the stack entry we just checked
            toCheck.removeFirst();
        }

        //If remaining is empty, then the network is still connected and everything is fine.
        if(!remaining.isEmpty()) {
            //Remaining is not empty. Continue recalibration!
            EnergyNetwork newNetwork = createNetwork(world.getWorldConfig().getUuid());

            //move the network to a new network
            for(Vector3i value : originNetwork) {
                energyNetworks.get(networkID).pushEntityToNetwork(newNetwork, value, buffer);
            }

            //Update the targets list to only contain the remaining targets
            ArrayList<Vector3i> newTargets = new ArrayList<>(remaining);

            //Continue calibration until all entities are accounted for
            recalibrateNetwork(networkID, newTargets, world, buffer);
        }
    }


    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), ArchStar.get().getEnergyComponentType());
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
        //Convert global coords to local coords
        Vector3i localCoords = FoxLibrary.convertToLocalCoords(location);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(location.x, location.z));

        if(chunk == null) {
            return false;
        }

        Ref<ChunkStore> chunkRef = chunk.getReference();

        BlockComponentChunk blockComponentChunk = buffer.getComponent(chunkRef, BlockComponentChunk.getComponentType());

        if (blockComponentChunk == null) {
            ArchStar.LOGGER.at(Level.WARNING).log("Attempted to load chunk at " + location + ", but there was no block component chunk!");
            return false;
        }

        int index = ChunkUtil.indexBlockInColumn(localCoords.x, localCoords.y, localCoords.z);

        Ref<ChunkStore> blockRef = blockComponentChunk.getEntityReference(index);

        if (blockRef == null) {
            return false;
        }

        EnergyComponent component = buffer.getComponent(blockRef, ArchStar.get().getEnergyComponentType());

        return component != null;
    }

    private void processQueue() {
        EnergyBlockContext context = queue.getFirst();

        CommandBuffer<ChunkStore> commandBuffer = context.buffer;
        Ref<ChunkStore> ref = context.ref;

        World world = commandBuffer.getStore().getExternalData().getWorld();

        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

        if(info == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("EnergyNetworkSystem: Failed to process queue! BlockState was null!");
            return;
        }

        WorldChunk worldChunk = commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

        if(worldChunk == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log("EnergyNetworkSystem: Failed to process queue! World was null!");
            return;
        }

        //* 32 converts from local chunk coords to global coords
        int x = ChunkUtil.xFromBlockInColumn(info.getIndex()) + (worldChunk.getX() * 32);
        int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int z = ChunkUtil.zFromBlockInColumn(info.getIndex()) + (worldChunk.getZ() * 32);

        Vector3i location = new Vector3i(x, y, z);

        //Check if neighbors are in the lookup table.
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);

        //If no valid neighbors, create a new energy network and add the entity
        if(neighbors.isEmpty()) {
            createNetwork(ref, location, worldChunk, commandBuffer);
            recurseQueue();
            return;
        }

        //Check if there are multiple valid neighbors
        boolean multipleValidNeighbors = (neighbors.size() > 1);
        //Flag to determine success
        boolean validNetworkFound = false;
        int networkID = -1;

        //Valid neighbors found, add this entity to its network
        for(Vector3i neighbor : neighbors) {
            for(EnergyNetwork network : energyNetworks.values()) {
                //Check if the entity is in the network, if it is, add this to the network.
                if(network.queryNetwork(neighbor)) {
                    network.addEntity(ref, location, commandBuffer);
                    validNetworkFound = true;
                    networkID = network.getID();
                    neighbors.removeFirst();
                    break;
                }
            }

            if(validNetworkFound) {
                break;
            }
        }

        if(validNetworkFound && multipleValidNeighbors) {
            //Check that all neighbors contains the same network id
            ArrayList<Vector3i> invalidNeighbors = new ArrayList<>();
            for(Vector3i neighbor : neighbors) {
                if(!energyNetworks.get(networkID).queryNetwork(neighbor)) {
                    invalidNeighbors.add(neighbor);
                }
            }

            //If there is a mismatch in network ids, combine networks.
            while(!invalidNeighbors.isEmpty()) {
                int networkToRemove = -1;

                //Since multiple invalid neighbors can have the same network id, check that this neighbor was not already merged to the network.
                if(energyNetworks.get(networkID).queryNetwork(invalidNeighbors.getFirst())) {
                    invalidNeighbors.removeFirst();
                    continue;
                }

                //Find the network that contains the invalid entity
                for(EnergyNetwork network : energyNetworks.values()) {
                    //Check if the network contains the invalid neighbor, and then merge the networks if it does.
                    if(network.queryNetwork(invalidNeighbors.getFirst())) {
                        network.pushEntitiesToNetwork(energyNetworks.get(networkID), commandBuffer);
                        networkToRemove = network.getID();
                        invalidNeighbors.removeFirst();
                        break;
                    }
                }

                if(networkToRemove != -1) {
                    energyNetworks.remove(networkToRemove);
                } else {
                    //If networkToRemove == -1, then the neighbor is not intialized. We should skip it to prevent infinite looping.
                    invalidNeighbors.removeFirst();
                }
            }
        }

        if(!validNetworkFound) {
            //If no valid network is found, create a new network anyway.
            createNetwork(ref, location, worldChunk, commandBuffer);
            recurseQueue();
            return;
        }

        //Finally, set it to tick
        addToTick(ref, worldChunk, commandBuffer, location);
        recurseQueue();
    }

    private void recurseQueue() {
        queue.removeFirst();
        if(queue.isEmpty()) {
            isQueueRunning = false;
        } else {
            processQueue();
        }
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
