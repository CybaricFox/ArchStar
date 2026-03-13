package com.CybaricFox.Components.Energy;

import com.CybaricFox.Components.Energy.EnergyBehaviour.EnergyBehaviour;

import java.util.HashMap;
import java.util.function.Supplier;

/*
    This registry is for registering new machine behaviours and mapping machine ids to those behaviours
 */
public final class EnergyBehaviourRegistry {
    private static final HashMap<String, Supplier<EnergyBehaviour>> REGISTRY = new HashMap<>();

    public static void register(String blockId, Supplier<EnergyBehaviour> supplier) {
        REGISTRY.put(blockId, supplier);
    }

    public static EnergyBehaviour create(String blockId) {
        Supplier<EnergyBehaviour> supplier = REGISTRY.get(blockId);

        if(supplier == null) {
            throw new IllegalStateException("Energy Behaviour Registry: No energy behaviour is defined for " + blockId);
        }

        return supplier.get();
    }

    public static void override(String blockId, Supplier<EnergyBehaviour> supplier) {
        if(REGISTRY.get(blockId) == null) {
            register(blockId, supplier);
        } else {
            REGISTRY.remove(blockId);
            REGISTRY.put(blockId, supplier);
        }
    }
}
