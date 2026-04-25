package com.CybaricFox.Modules.ArchMachines.Upgrade;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.MachineBehavior.MachineBehavior;
import com.CybaricFox.Modules.ArchMachines.Upgrade.Upgrades.BasicModulationUpgrade;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.logging.Level;

/*
    This registry is for registering upgrades for machines and items
 */
public final class UpgradeRegistry {
    private static final HashMap<String, ArrayList<String>> REGISTRY = new HashMap<>();
    private static final HashMap<String, BaseUpgrade> UPGRADE_REGISTRY = new HashMap<>();

    public static void registerItem(String itemId, ArrayList<String> upgrades) {
        if(REGISTRY.containsKey(itemId)) {
            ArrayList<String> newUpgrades = REGISTRY.get(itemId);
            for(String upgrade : upgrades) {
                if(newUpgrades.contains(upgrade)) continue;
                newUpgrades.add(upgrade);
            }
            REGISTRY.put(itemId, newUpgrades);
        } else {
            REGISTRY.put(itemId, upgrades);
        }
    }
    public static BaseUpgrade registerUpgrade(BaseUpgrade upgrade) {
        UPGRADE_REGISTRY.put(upgrade.getId(), upgrade);
        return upgrade;
    }

    public static ArrayList<String> getUpgrades(String blockId) {
        if(REGISTRY.containsKey(blockId)) {
            return REGISTRY.get(blockId);
        } else {
            ArchLibrary.LOGGER.at(Level.WARNING).log(blockId + " is not registered to the upgrade registry.");
            return null;
        }
    }

    public static void removeUpgradeFromItem(String itemId, String upgradeId) {
        if(REGISTRY.containsKey(itemId)) {
            ArrayList<String> newUpgrades = REGISTRY.get(itemId);
            newUpgrades.remove(upgradeId);
            REGISTRY.put(itemId, newUpgrades);
        }
    }

    public static BaseUpgrade getUpgrade(String upgradeId) {
        if(UPGRADE_REGISTRY.containsKey(upgradeId)) {
            return UPGRADE_REGISTRY.get(upgradeId);
        } else {
            throw new ArrayStoreException(upgradeId + " is not registered to the upgrade registry!");
        }
    }
    public static BaseUpgrade getUpgradeByType(String upgradeId, UpgradeType type) {
        if(UPGRADE_REGISTRY.containsKey(upgradeId)) {
            if(UPGRADE_REGISTRY.get(upgradeId).type == type) {
                return UPGRADE_REGISTRY.get(upgradeId);
            } else {
                return null;
            }
        } else {
            throw new ArrayStoreException(upgradeId + " is not registered to the upgrade registry!");
        }
    }
}
