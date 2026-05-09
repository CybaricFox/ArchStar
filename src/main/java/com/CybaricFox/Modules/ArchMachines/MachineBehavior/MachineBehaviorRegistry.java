package com.CybaricFox.Modules.ArchMachines.MachineBehavior;

import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.AssetReader;
import com.azuredoom.hytalecustomassetloader.AssetDiscoveryOptions;
import com.azuredoom.hytalecustomassetloader.AssetLoadResult;
import com.azuredoom.hytalecustomassetloader.AssetLoader;
import com.azuredoom.hytalecustomassetloader.spi.AssetLogger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.logging.Level;

/*
    This registry is for registering new machine behaviours and mapping machine ids to those behaviours
 */
public final class MachineBehaviorRegistry {
    private static final HashMap<String, String> REGISTRY = new HashMap<>();
    private static final HashMap<String, MachineBehavior> BASE_BEHAVIOR_REGISTRY = new HashMap<>();

    public static void registerMachine(String blockId, String behaviorId) {
        REGISTRY.put(blockId, behaviorId);
    }

    public static void registerBaseBehavior(String behaviorId, MachineBehavior behavior) {
        if(BASE_BEHAVIOR_REGISTRY.containsKey(behaviorId)) {
            ArchLibrary.LOGGER.at(Level.WARNING).log("Machine Behavior Registry already registered " + behaviorId + ". The value will be overridden!");
        }

        BASE_BEHAVIOR_REGISTRY.put(behaviorId, behavior);
    }

    private static MachineBehavior getBehavior(String behaviorId) {
        MachineBehavior behavior = BASE_BEHAVIOR_REGISTRY.get(behaviorId);
        if(behavior == null) {
            throw new NullPointerException("Machine Behavior Registry does not contain a base behavior of " + behaviorId);
        }
        return behavior.createInstance();
    }

    public static MachineBehavior create(String blockId) {
        if(!REGISTRY.containsKey(blockId)) {
            throw new IllegalStateException("Machine Behaviour Registry: No machine behaviour is defined for " + blockId);
        }

        return getBehavior(REGISTRY.get(blockId));
    }

    public static void getJsonAssets() {
        AssetLoadResult<JsonObject> itemResult = getAssetLoaderResult("Machines/Items", new AssetReader("itemId"));
        for(JsonObject definition : itemResult.mergedAssets().values()) {
            String itemId = definition.get("itemId").getAsString();
            JsonElement behaviorElement = definition.get("behaviorId");
            if(behaviorElement.isJsonNull()) continue;
            String behavior = behaviorElement.getAsString();

            registerMachine(itemId, behavior);
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
}
