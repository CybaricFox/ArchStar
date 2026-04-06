package com.CybaricFox.Modules.ArchMachines.MachineBehavior;

import java.util.HashMap;
import java.util.function.Supplier;

/*
    This registry is for registering new machine behaviours and mapping machine ids to those behaviours
 */
public final class MachineBehaviorRegistry {
    private static final HashMap<String, Supplier<MachineBehavior>> REGISTRY = new HashMap<>();

    public static void register(String blockId, Supplier<MachineBehavior> supplier) {
        REGISTRY.put(blockId, supplier);
    }

    public static MachineBehavior create(String blockId) {
        Supplier<MachineBehavior> supplier = REGISTRY.get(blockId);

        if(supplier == null) {
            throw new IllegalStateException("Machine Behaviour Registry: No machine behaviour is defined for " + blockId);
        }

        return supplier.get();
    }

    public static void override(String blockId, Supplier<MachineBehavior> supplier) {
        if(REGISTRY.get(blockId) == null) {
            register(blockId, supplier);
        } else {
            REGISTRY.remove(blockId);
            REGISTRY.put(blockId, supplier);
        }
    }
}
