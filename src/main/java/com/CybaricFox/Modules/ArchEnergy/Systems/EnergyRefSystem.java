package com.CybaricFox.Modules.ArchEnergy.Systems;

import com.CybaricFox.Modules.ArchEnergy.EnergyBlockType;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyCableComponent;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchEnergy.EnergyNetwork;
import com.CybaricFox.Modules.ArchLibrary.Direction;
import com.CybaricFox.Modules.ArchLibrary.EssentialsContext;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
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
public class EnergyRefSystem extends RefSystem<ChunkStore>{
    //Map of Ids to energy networks.
    private final ConcurrentHashMap<Integer, EnergyNetwork> energyNetworks = new ConcurrentHashMap<Integer, EnergyNetwork>();

    //The next network that's created will use this id
    private int nextNetworkID = 0;

    public EnergyRefSystem() {

    }

    public ArrayList<EnergyNetwork> getAllNetworks() {
        return new ArrayList<>(energyNetworks.values());
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

        //All ticking is handled by energy networks. Do not handle ticks from machines.
        context.chunk.setTicking(context.pos.x, context.pos.y, context.pos.z, false);

        EnergyComponent energy = commandBuffer.getComponent(ref, EnergyComponent.getComponentType());
        EnergyCableComponent cable = commandBuffer.getComponent(ref, EnergyCableComponent.getComponentType());

        if(energy != null) {
            handleEnergyBlock(ref, commandBuffer, context.pos, context.chunk, context.world);
            changeCableState(ref, commandBuffer, context.pos, true);
        } else if(cable != null) {
            handleCableBlock(commandBuffer, context.pos, context.world);
            changeCableState(ref, commandBuffer, context.pos, true);
        }


    }
    
    private void changeCableState(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> buffer, Vector3i pos, boolean isInitial) {
        ArrayList<Vector3i> neighbors = getValidNeighbors(buffer.getExternalData().getWorld(), pos, buffer);

        if(!isInitial) {
            ref = ArchLibrary.getBlockEntity(buffer.getExternalData().getWorld(), pos);
        }

        if(neighbors.isEmpty()) {
            ArchLibrary.changeBlockState(ref, buffer, "default");
            return;
        }

        ArrayList<Direction> directions = new ArrayList<>();
        ArrayList<Vector3i> vectorsForDirections = ArchLibrary.getNeighborVectors(pos);
        
        for(Vector3i neighbor : neighbors) {
            for(int i = 0; i < 6; i++) {
                if(vectorsForDirections.get(i).equals(neighbor)) {
                    switch(i) {
                        case 0 -> directions.addLast(Direction.NORTH);
                        case 1 -> directions.addLast(Direction.SOUTH);
                        case 2 -> directions.addLast(Direction.EAST);
                        case 3 -> directions.addLast(Direction.WEST);
                        case 4 -> directions.addLast(Direction.UP);
                        case 5 -> directions.addLast(Direction.DOWN);
                    }
                    
                    break;
                }
            }
        }
        
        String finalString = "";
        
        for(Direction direction : directions) {
            switch (direction) {
                case NORTH -> finalString = finalString.concat("North");
                case SOUTH -> finalString = finalString.concat("South");
                case EAST -> finalString = finalString.concat("East");
                case WEST -> finalString = finalString.concat("West");
                case UP -> finalString = finalString.concat("Up");
                case DOWN -> finalString = finalString.concat("Down");
            }
        }
        
        ArchLibrary.changeBlockState(ref, buffer, finalString);

        if(!isInitial || neighbors.isEmpty()) return;
        for(Vector3i neighbor : neighbors) {
            changeCableState(ref, buffer, neighbor, false);
        }
    }
    
    //Handles adding energy blocks to the network system. Systems may need to recalibrate.
    private void handleEnergyBlock(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> commandBuffer, Vector3i location, WorldChunk worldChunk, World world) {
        //Go along every adjacent cable and find the nearest energy block
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);
        
        //if no neighbors, just create a new network
        if(neighbors.isEmpty()) {
            createNetwork(ref, location, worldChunk, commandBuffer);
            return;
        }

        int targetNetwork = confirmAndCombineNetworks(neighbors, world, commandBuffer, location);

        //Finally, add this entity to the network
        if(targetNetwork == -1) {
            createNetwork(ref, location, worldChunk, commandBuffer);
        } else {
            getNetwork(targetNetwork).addEntity(ref, location, commandBuffer);
            addToTick(ref, worldChunk, commandBuffer, location);
        }
    }

    private void handleCableBlock(CommandBuffer<ChunkStore> commandBuffer, Vector3i location, World world) {
        //Go along every adjacent cable and find the nearest energy block
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, location, commandBuffer);

        //if no neighbors, then theres nothing to do here
        if(neighbors.isEmpty()) {
            return;
        }

        //Confirm networks are combined if needed
        confirmAndCombineNetworks(neighbors, world, commandBuffer, location);
    }

    private int confirmAndCombineNetworks(ArrayList<Vector3i> neighbors, World world, CommandBuffer<ChunkStore> commandBuffer, Vector3i origin) {
        ArrayList<Integer> foundNetworks = new ArrayList<>();

        //Get the network id of every branch of this block
        for(Vector3i neighbor : neighbors) {
            //This block does not have a network assigned to it
            if(getNetworkFromVector(neighbor) == -1) {
                foundNetworks.add(findConnectedNetwork(neighbor, commandBuffer, world, origin));
                //This block does have a network assigned to it
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
    private int findConnectedNetwork(Vector3i pos, CommandBuffer<ChunkStore> buffer, World world, Vector3i origin) {
        ArrayList<Vector3i> neighbors = getValidNeighbors(world, pos, buffer);

        //If no valid neighbors, return
        if(neighbors.isEmpty()) {
            return -1;
        }

        //Iterate over the stack 1 cable at a time. Save all found cables in the set to prevent loops
        ArrayList<Vector3i> stack = new ArrayList<>();
        HashSet<Vector3i> set = new HashSet<>();

        if(origin != null) {
            set.add(origin);
        }

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
        EnergyComponent energyComponent = buffer.getComponent(ref, EnergyComponent.getComponentType());

        if(energyComponent.getType() == EnergyBlockType.PRODUCER || energyComponent.getType() == EnergyBlockType.CONSUMER) {
            chunk.setTicking(location.x, location.y, location.z, true);
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<ChunkStore> ref, @Nonnull RemoveReason removeReason, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        EssentialsContext context = new EssentialsContext(ref, commandBuffer);
        if(!context.isValid) return;

        EnergyComponent energy = commandBuffer.getComponent(ref, EnergyComponent.getComponentType());
        EnergyCableComponent cable = commandBuffer.getComponent(ref, EnergyCableComponent.getComponentType());

        ArrayList<Vector3i> neighbors = getValidNeighbors(context.world, context.pos, commandBuffer);

        if(energy != null) {
            getNetwork(energy.getNetworkID()).removeEntity(context.pos, energy.getType());
            recalibrateNetwork(energy.getNetworkID(), context.pos, context.world, commandBuffer);

            for(Vector3i neighbor : neighbors) {
                changeCableState(ref, commandBuffer, neighbor, false);
            }
        } else if(cable != null) {
            recalibrateNetwork(findConnectedNetwork(context.pos, commandBuffer, context.world, null), context.pos, context.world, commandBuffer);
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

            networkSize -= size;

            //All machines are accounted for. No need to recalibrate the network.
            if(networkSize == 0) {
                return;
            }

            //Move the found machines to a new network
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
        return Query.or(EnergyComponent.getComponentType(), EnergyCableComponent.getComponentType());
    }

    //Returns a list of all valid neighbors
    private ArrayList<Vector3i> getValidNeighbors(World world, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        ArrayList<Vector3i> neighbors = new ArrayList<>();

        for(Vector3i neighbor : ArchLibrary.getNeighborVectors(location)) {
            if(isValidNeighbor(world, neighbor, buffer)) neighbors.add(neighbor);
        }
        
        return neighbors;
    }

    //Check that the block at the location is compatible
    private boolean isValidNeighbor(World world, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, location);
        if (blockRef == null) return false;

        EnergyComponent energy = buffer.getComponent(blockRef, EnergyComponent.getComponentType());
        EnergyCableComponent cable = buffer.getComponent(blockRef, EnergyCableComponent.getComponentType());

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
