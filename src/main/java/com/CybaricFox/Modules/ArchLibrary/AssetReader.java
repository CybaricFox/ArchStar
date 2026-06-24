package com.CybaricFox.Modules.ArchLibrary;

import com.CybaricFox.ArchStar;
import com.azuredoom.hytalecustomassetloader.AssetDiscoveryOptions;
import com.azuredoom.hytalecustomassetloader.AssetLoadResult;
import com.azuredoom.hytalecustomassetloader.AssetLoader;
import com.azuredoom.hytalecustomassetloader.model.AssetSourceKind;
import com.azuredoom.hytalecustomassetloader.spi.AssetIdExtractor;
import com.azuredoom.hytalecustomassetloader.spi.AssetLogger;
import com.azuredoom.hytalecustomassetloader.spi.AssetParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.logging.Level;

public class AssetReader implements AssetParser<JsonObject>, AssetIdExtractor<JsonObject> {
    String targetId;
    private AssetLoadResult<JsonObject> loadResult;

    public AssetReader(String targetId, String folderLocation) {
        newRead(targetId, folderLocation);
    }

    @Override
    public String getId(JsonObject jsonObject) {
        return jsonObject.get(targetId).getAsString();
    }

    @Override
    public JsonObject parse(InputStream inputStream, String s, AssetSourceKind assetSourceKind) {
        try(InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to read " + s + ": " + e.getMessage());
        }
    }

    public AssetLoadResult<JsonObject> getAssetLoaderResult(String resourceFolder) {
        AssetLoader<JsonObject> loader = new AssetLoader<>(
                ArchStar.get().getClass().getClassLoader(),
                new AssetDiscoveryOptions(
                        "Server/ArchStarCustom/" + resourceFolder,
                        ".json",
                        Paths.get("mods").toAbsolutePath().normalize(),
                        true,
                        false
                ),
                this,
                this,
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

    public void newRead(String targetId, String folderLocation) {
        this.targetId = targetId;
        loadResult = getAssetLoaderResult(folderLocation);
    }

    public AssetLoadResult<JsonObject> getLoadResult() {
        return loadResult;
    }
}
