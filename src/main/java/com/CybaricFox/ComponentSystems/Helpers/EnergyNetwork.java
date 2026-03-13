package com.CybaricFox.ComponentSystems.Helpers;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyBlockType;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Energy.EnergyTransaction;
import com.CybaricFox.Components.Energy.EnergyTransactionContext;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/*
    An energy network contains a map of global positions to energy block type.
    Each entry is considered to be physically connected in the world
 */
public class EnergyNetwork {
    private final int id;
    private final UUID worldUUID;

    private final HashSet<Vector3i> producers = new HashSet<>();
    private final HashSet<Vector3i> consumers = new HashSet<>();
    private final HashSet<Vector3i> storages = new HashSet<>();

    private final ArrayList<EnergyTransactionContext> transactionQueue = new ArrayList<>();

    //Should this network be removed the next time the energy system makes a removal check?
    public boolean markedForRemoval = false;

    public EnergyNetwork(int id, UUID worldUUID) {
        this.id = id;
        this.worldUUID = worldUUID;
    }

    public void requestTransaction(EnergyTransaction transaction, EnergyComponent component) {
        transactionQueue.add(new EnergyTransactionContext(transaction, component));
    }

    public void runTransactions() {
        for(EnergyTransactionContext transaction : transactionQueue) {
            EnergyComponent requester = transaction.energyComponent;
            //The total amount of power in this transaction so far
            int total = 0;

            switch(transaction.transaction) {
                case WITHDRAW -> {
                    //The maximum amount that can be requested in 1 transaction is the lower of these 2 values
                    int maxRequest = Math.min(requester.getInputRate(), requester.getRemaining());

                    //Cannot withdraw if power buffer is full already
                    if(maxRequest == 0) {
                        continue;
                    }

                    total = calculateWithdrawal(total, maxRequest, requester.getInputRate(), EnergyBlockType.STORAGE);

                    //If max request has been reached, add the energy to the requester and move on to the next transaction
                    if(total == maxRequest) {
                        requester.addEnergy(total);
                        continue;
                    }

                    //Otherwise iterate over producers
                    total = calculateWithdrawal(total, maxRequest, requester.getInputRate(), EnergyBlockType.PRODUCER);

                    //No remaining block to withdraw from. Push the total.
                    requester.addEnergy(total);
                }
                case SEND -> {

                }
            }
        }

        //All transactions queried. clear the queue.
        transactionQueue.clear();
    }

    private int calculateWithdrawal(int totalSoFar, int maxRequest, int inputRate, EnergyBlockType type) {
        int total = totalSoFar;

        //Get all storages
        HashMap<Vector3i, EnergyComponent> storages = getAllOfType(type);

        if(!storages.isEmpty()) {
            for(EnergyComponent storage : storages.values()) {
                if(total == maxRequest) {
                    return total;
                }

                total += storage.transferEnergy(inputRate, maxRequest - total);
            }
        }

        return total;
    }

    public int getID() {
        return id;
    }

    public int getSize() {
        return producers.size() + consumers.size() + storages.size();
    }

    //Add an energy block to the map
    public void addEntity(Ref<ChunkStore> ref, Vector3i location, CommandBuffer<ChunkStore> buffer) {
        EnergyComponent energy = buffer.getComponent(ref, ArchStar.get().getEnergyComponentType());

        if(energy == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log(location.toString() + " does not contain a block entity with an energy component!");
            return;
        }

        switch(energy.getType()) {
            case PRODUCER -> producers.add(location);
            case CONSUMER -> consumers.add(location);
            case STORAGE -> storages.add(location);
            case NOT_SET -> {
                ArchStar.LOGGER.at(Level.WARNING).log("Attempted to add an energy block to network " + id + " but type =" + energy.getType() + "! The block will not be added.");
                return;
            }
        }

        energy.setNetworkID(id);
    }

    //Removes an entry from the map
    public void removeEntity(Vector3i location, EnergyBlockType type) {
        switch (type) {
            case PRODUCER -> producers.remove(location);
            case CONSUMER -> consumers.remove(location);
            case STORAGE -> storages.remove(location);
        }

        //If the map is empty, it is impossible to add new values, so remove it.
        if(isEmpty()) markedForRemoval = true;
    }

    private boolean isEmpty() {
        int size = producers.size() + consumers.size() + storages.size();

        return size == 0;
    }

    //Returns the energy block type of the given block location
    public EnergyBlockType getType(Vector3i key) {
        if(producers.contains(key)) {
            return EnergyBlockType.PRODUCER;
        }
        if(consumers.contains(key)) {
            return EnergyBlockType.CONSUMER;
        }
        if(storages.contains(key)) {
            return EnergyBlockType.STORAGE;
        }

        ArchStar.LOGGER.at(Level.INFO).log("Failed to find " + key + " in " + id + "!");
        return EnergyBlockType.NOT_SET;
    }

    //Returns true if the given location exists in this network
    public boolean queryNetwork(Vector3i location) {
        return producers.contains(location) || consumers.contains(location) || storages.contains(location);
    }

    //Returns a map of locations to energy component of all blockentities in this network that have the matching type.
    //Returns null if no entity of that type exists in this network.
    public HashMap<Vector3i, EnergyComponent> getAllOfType(EnergyBlockType type) {
        HashMap<Vector3i, EnergyComponent> matchingEntities= new HashMap<>();

        HashSet<Vector3i> targetSet = new HashSet<>();

        switch(type) {
            case PRODUCER -> targetSet.addAll(producers);
            case CONSUMER -> targetSet.addAll(consumers);
            case STORAGE -> targetSet.addAll(storages);
        }

        if(targetSet.isEmpty()) {
            return matchingEntities;
        }

        ArrayList<Vector3i> targetArray = new ArrayList<>(targetSet);

        for(Vector3i entry : targetArray) {
            Ref<ChunkStore> entity = ArchLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), entry);
            EnergyComponent component = entity.getStore().getComponent(entity, ArchStar.get().getEnergyComponentType());
            matchingEntities.put(entry, component);
        }

        return matchingEntities;
    }

    //Push all entities into a new network. This is used to combine networks.
    public void pushEntitiesToNetwork(EnergyNetwork newNetwork, CommandBuffer<ChunkStore> buffer) {
        ArrayList<Vector3i> listOfAll = new ArrayList<>();
        listOfAll.addAll(producers);
        listOfAll.addAll(consumers);
        listOfAll.addAll(storages);

        for(Vector3i entry : listOfAll) {
            newNetwork.addEntity(ArchLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), entry), entry, buffer);
        }

        clearAll();
    }

    private void clearAll() {
        producers.clear();
        consumers.clear();
        storages.clear();
        markedForRemoval = true;
    }

    //Push the specific entity to the given network and remove it from this network.
    public void pushEntityToNetwork(EnergyNetwork newNetwork, Vector3i key, CommandBuffer<ChunkStore> buffer) {
        if(getType(key) != EnergyBlockType.NOT_SET) {
            newNetwork.addEntity(ArchLibrary.getBlockEntity(Universe.get().getWorld(worldUUID), key), key, buffer);
            removeEntity(key, getType(key));
        }
    }
}
