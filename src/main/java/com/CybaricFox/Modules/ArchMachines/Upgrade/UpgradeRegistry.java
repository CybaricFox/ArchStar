package com.CybaricFox.Modules.ArchMachines.Upgrade;

import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.AssetReader;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.azuredoom.hytalecustomassetloader.AssetDiscoveryOptions;
import com.azuredoom.hytalecustomassetloader.AssetLoadResult;
import com.azuredoom.hytalecustomassetloader.AssetLoader;
import com.azuredoom.hytalecustomassetloader.spi.AssetLogger;
import com.google.gson.*;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;

/*
    This registry is for registering upgrades for machines and items
 */
public final class UpgradeRegistry {
    private static final HashMap<String, ArrayList<String>> REGISTRY = new HashMap<>();
    private static final HashMap<String, BaseUpgrade> UPGRADE_REGISTRY = new HashMap<>();
    private static final HashMap<String, BaseUpgrade> BASE_UPGRADE_REGISTRY = new HashMap<>();

    private static final HashMap<String, ArrayList<JsonObject>> rawAssetDataMap = new HashMap<>();

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
        //Do not register invalid upgrades
        if(upgrade == null) return null;

        //Warn the user that an override has occurred
        if(UPGRADE_REGISTRY.containsKey(upgrade.getId())) {
            ArchLibrary.LOGGER.at(Level.WARNING).log(upgrade.getId() + " already exists in the registry. The upgrade will be overridden!");
        }

        UPGRADE_REGISTRY.put(upgrade.getId(), upgrade);
        return upgrade;
    }
    public static void registerBaseUpgrade(BaseUpgrade upgrade) {
        BASE_UPGRADE_REGISTRY.put(upgrade.getId(), upgrade);
    }

    public static BaseUpgrade getBaseUpgrade(String id) {
        if(BASE_UPGRADE_REGISTRY.containsKey(id)) {
            return BASE_UPGRADE_REGISTRY.get(id);
        } else {
            throw new IllegalArgumentException(id + " is not a registered base upgrade.");
        }
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

    public static void getJsonAssets() {
        //Load upgrade assets
        AssetLoadResult<JsonObject> upgradeResult = getAssetLoaderResult("Machines/Upgrades", new AssetReader("id"));

        for(JsonObject definition : upgradeResult.mergedAssets().values()) {
            String baseId = definition.get("baseUpgradeId").getAsString();

            if(rawAssetDataMap.containsKey(baseId)) {
                rawAssetDataMap.get(baseId).add(definition);
            } else {
                rawAssetDataMap.put(baseId, new ArrayList<>());
                rawAssetDataMap.get(baseId).add(definition);
            }
        }

        AssetLoadResult<JsonObject> itemResult = getAssetLoaderResult("Machines/Items", new AssetReader("itemId"));
        for(JsonObject definition : itemResult.mergedAssets().values()) {
            String itemId = definition.get("itemId").getAsString();
            ArrayList<String> upgrades = new ArrayList<>();

            JsonArray array = definition.get("upgrades").getAsJsonArray();
            if(array.isEmpty()) continue;

            for(int i = 0; i < array.size(); i++) {
                upgrades.add(array.get(i).getAsString());
            }

            REGISTRY.put(itemId, upgrades);
        }
    }

    private static AssetLoadResult<JsonObject> getAssetLoaderResult(String resourceFolder, AssetReader reader) {
        AssetLoader<JsonObject> loader = new AssetLoader<>(
                ArchStar.get().getClass().getClassLoader(),
                new AssetDiscoveryOptions(
                        "Server/ArchStarCustom/" + resourceFolder,
                        ".json",
                        Paths.get("mods").toAbsolutePath().normalize(),
                        true,
                        false
                ),
                reader,
                reader,
                new AssetLogger() {
                    @Override
                    public void info(String s) {
                        ArchLibrary.LOGGER.at(Level.INFO).log(s);
                    }

                    @Override
                    public void warn(String s) {
                        ArchLibrary.LOGGER.at(Level.WARNING).log(s);
                    }
                }
        );

        return loader.loadAll();
    }

    //Loads assets in the raw asset map that correspond to the given base upgrade id.
    public static void loadAssets(String id) {
        if(!rawAssetDataMap.containsKey(id)) {
            ArchLibrary.LOGGER.at(Level.WARNING).log(id + " does not exist in the raw data map!");
            return;
        }
        if(!BASE_UPGRADE_REGISTRY.containsKey(id)) {
            ArchLibrary.LOGGER.at(Level.SEVERE).log(id + " does not exist in the base upgrade registry!");
            return;
        }

        ArrayList<JsonObject> assets = rawAssetDataMap.get(id);
        BaseUpgrade baseUpgrade = BASE_UPGRADE_REGISTRY.get(id);

        for(JsonObject asset : assets) {
            registerUpgrade(baseUpgrade.parse(asset));
        }
    }
}
