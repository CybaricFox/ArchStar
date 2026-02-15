package com.CybaricFox.ComponentSystems.Helpers;

import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Helpers.EnergyBlockType;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/*
    An energy network contains a map of global positions to energy block type.
    Each entry is considered to be physically connected in the world
 */
public class EnergyNetwork {
    private int id;
    private final UUID worldUUID;

    private final ConcurrentHashMap<Vector3i, EnergyBlockType> entities = new ConcurrentHashMap<>();

    //Should this network be removed the next time the energy system makes a removal check?
    public boolean markedForRemoval = false;

    public EnergyNetwork(int id, UUID worldUUID) {
        this.id = id;
        this.worldUUID = worldUUID;
    }

    public int getID() {
        return id;
    }

    //Add an energy block to the map
    public void addEntity(Ref<ChunkStore> ref, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        EnergyComponent energy = buffer.getComponent(ref, ArchStar.get().getEnergyComponentType());

        if(energy == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log(location.toString() + " does not contain a block entity with an energy component!");
            return;
        }

        entities.put(location, energy.getType());

        energy.setNetworkID(id);
    }

    //Removes an entry from the map
    public void removeEntity(Vector3i location) {
        entities.remove(location);

        //If the map is empty, it is impossible to add new values, so remove it.
        if(entities.isEmpty()) markedForRemoval = true;
    }

    //Returns the energy block type of the given block location
    public EnergyBlockType getType(Vector3i key) {
        if(entities.containsKey(key)) {
            return entities.get(key);
        } else {
            ArchStar.LOGGER.at(Level.INFO).log("Failed to find " + key + " in " + id + "!");
            return null;
        }
    }

    //Returns true if the given location exists in this network
    public boolean queryNetwork(Vector3i location) {
        return entities.containsKey(location);
    }

    //Returns a map of locations to energy component of all blockentities in this network that have the matching type.
    //Returns null if no entity of that type exists in this network.
    public HashMap<Vector3i, EnergyComponent> getAllOfType(EnergyBlockType type) {
        HashMap<Vector3i, EnergyComponent> matchingEntities= new HashMap<>();

        for(ConcurrentHashMap.Entry<Vector3i, EnergyBlockType> entry : entities.entrySet()) {
            if(entry.getValue() == type) {
                Ref<ChunkStore> entity = FoxLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), entry.getKey());

                EnergyComponent component = entity.getStore().getComponent(entity, ArchStar.get().getEnergyComponentType());

                matchingEntities.put(entry.getKey(), component);
            }
        }

        if(!matchingEntities.isEmpty()) {
            return matchingEntities;
        }

        return null;
    }

    //Push all entities into a new network. This is used to combine networks.
    public void pushEntitiesToNetwork(EnergyNetwork newNetwork, CommandBuffer<ChunkStore> buffer) {
        for(Vector3i entry : entities.keySet()) {
            newNetwork.addEntity(FoxLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), entry), entry, buffer);
        }

        entities.clear();
        markedForRemoval = true;
    }

    //Push the specific entity to the given network and remove it from this network.
    public void pushEntityToNetwork(EnergyNetwork newNetwork, Vector3i key, CommandBuffer<ChunkStore> buffer) {
        if(entities.containsKey(key)) {
            newNetwork.addEntity(FoxLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), key), key, buffer);
            removeEntity(key);
        }
    }
}
