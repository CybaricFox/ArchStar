package com.CybaricFox.Modules.ArchLibrary;

import com.azuredoom.hytalecustomassetloader.model.AssetSourceKind;
import com.azuredoom.hytalecustomassetloader.spi.AssetIdExtractor;
import com.azuredoom.hytalecustomassetloader.spi.AssetParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetReader implements AssetParser<JsonObject>, AssetIdExtractor<JsonObject> {
    String targetId;

    public AssetReader(String targetId) {
        this.targetId = targetId;
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
}
