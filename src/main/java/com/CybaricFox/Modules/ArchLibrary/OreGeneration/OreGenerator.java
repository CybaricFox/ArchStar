package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.AssetReader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.sun.source.tree.BinaryTree;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

public class OreGenerator{
    private final long BASE_SEED = 0xcbf29ce484222325L;
    private final long BASE_PRIME = 0x100000001b3L;

    public ArrayList<OreHolder> oreGenQueue = new ArrayList<>();

    private final OreConfig[] ores;

    public OreGenerator() {
        ArchLibrary.LOGGER.at(Level.INFO).log("ArchLibrary is loading the ore generation system.");
        AssetReader reader = new AssetReader("OreId", "Ores");
        ores = new OreConfig[reader.getLoadResult().mergedAssets().size()];

        int count = 0;
        for(JsonObject definition : reader.getLoadResult().mergedAssets().values()) {

            String[] hostBlocks;
            JsonArray rawBlocks = definition.get("HostBlocks").getAsJsonArray();
            hostBlocks = new String[rawBlocks.size()];
            for(int i = 0; i < rawBlocks.size(); i++) {
                hostBlocks[i] = rawBlocks.get(i).getAsString();
            }

            ArrayList<String> biomes = null;
            boolean biomesIsWhite = true;
            if(definition.has("Biomes") && !definition.get("Biomes").isJsonNull()) {
                if(definition.has("BiomeIsWhite")) {
                    biomesIsWhite = definition.get("BiomeIsWhite").getAsBoolean();
                }

                JsonArray rawBiomes = definition.get("Biomes").getAsJsonArray();
                if(!rawBiomes.isEmpty()) {
                    biomes = new ArrayList<>();
                    for(JsonElement biome : rawBiomes) {
                        biomes.add(biome.getAsString());
                    }
                }
            }

            int[] zones = null;
            boolean zonesIsWhite = true;
            if(definition.has("Zones") && !definition.get("Zones").isJsonNull()) {
                if(definition.has("ZoneIsWhite")) {
                    zonesIsWhite = definition.get("ZoneIsWhite").getAsBoolean();
                }

                JsonArray rawZones = definition.get("Zones").getAsJsonArray();
                if(!rawZones.isEmpty()) {
                    zones = new int[rawZones.size()];
                    for(int i = 0; i < rawZones.size(); i++) {
                        zones[i] = rawZones.get(i).getAsInt();
                    }
                }
            }

            OreConfig ore = new OreConfig(
                    definition.get("OreId").getAsString(),
                    definition.get("MinVeins").getAsInt(),
                    definition.get("MaxVeins").getAsInt(),
                    definition.get("MinVeinSize").getAsInt(),
                    definition.get("MaxVeinSize").getAsInt(),
                    definition.get("MinY").getAsInt(),
                    definition.get("MaxY").getAsInt(),
                    hostBlocks,
                    biomesIsWhite,
                    zonesIsWhite,
                    biomes,
                    zones
            );

            ores[count] = ore;
            count++;
        }
        ArchLibrary.LOGGER.at(Level.INFO).log("Ore generation system loaded successfully.");
    }

    public void processChunk(ChunkPreLoadProcessEvent event) {
        if(!event.isNewlyGenerated()) return;

        WorldChunk chunk = event.getChunk();

        oreGenQueue.addLast(new OreHolder(chunk));
    }

    public void generateOres(WorldChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        //SecureRandom is not needed here, Random is faster.
        Random rng = new Random();

        for(OreConfig ore : ores) {
            rng.setSeed(createSeed(chunk.getWorld().getWorldConfig().getSeed(), x, z, ore.getOre()));

            int pockets = rng.nextInt(ore.getMinPockets(), ore.getMaxPockets() + 1);

            for(int i = 0; i < pockets; i++) {
                generateVein(rng, chunk, ore);
            }
        }
    }

    private void generateVein(Random rng, WorldChunk chunk, OreConfig ore) {
        boolean isValidFirstLocation = false;

        //Get source location
        int x = rng.nextInt(0, 32);
        int z = rng.nextInt(0, 32);
        int y = rng.nextInt(ore.getMinY(), ore.getMaxY() + 1);

        //Always try to make it so the original location picked for a vein is always valid.
        int originSafety = 0; //Prevents infinite looping
        while(!isValidFirstLocation) {
            originSafety++;
            BlockType type = chunk.getBlockType(new Vector3i(x, y, z));

            if(type != null) {
                String existingId = type.getId();

                for(String block : ore.getHostBlocks()) {
                    if(block.equals(existingId)) {
                        isValidFirstLocation = true;
                        break;
                    }
                }
            }

            //If invalid first location, reroll the starting location.
            if(!isValidFirstLocation) {
                x = rng.nextInt(0, 32);
                z = rng.nextInt(0, 32);
                y = rng.nextInt(ore.getMinY(), ore.getMaxY() + 1);
            }

            //If safety threshold reached, force the vein to generate where it currently is, even if it's not a good location.
            if(originSafety > 10 && !isValidFirstLocation) {
                //ArchLibrary.LOGGER.at(Level.WARNING).log("Failed to find a valid host block for " + ore.getOre() + " within safety parameters! Vein will be force generated. This can be safely ignored.");
                isValidFirstLocation = true;
            }
        }

        //If the starting location is an invalid biome or zone, this vein will not generate.
        ZoneBiomeResult result = getZoneBiomeResult(chunk, x, z);
        boolean isBiomeValid = isBiomeValid(ore, result);
        boolean isZoneValid = isZoneValid(ore, result);

        if(!isBiomeValid || !isZoneValid) return;

        //Get vein size
        int veinSize = rng.nextInt(ore.getMinVeinSize(), ore.getMaxVeinSize() + 1);
        Vector3i[] previousLocations = new Vector3i[veinSize];

        for(int i = 0; i < veinSize; i++) {
            boolean isValid = false;

            //Check the block at this location. If its in the array, it may be replaced
            //If its not in the array, this block will not generate.
            BlockType type = chunk.getBlockType(new Vector3i(x, y, z));
            if(type != null) {
                String existingId = type.getId();

                for(String block : ore.getHostBlocks()) {
                    if(block.equals(existingId)) {
                        //Generate the ore
                        chunk.setBlock(x, y, z, ore.getOre());
                        //Add the location to previous Locations visited
                        previousLocations[i] = new Vector3i(x, y, z);
                        break;
                    }
                }
            }

            int safety = 0;

            while(!isValid) {
                safety++;
                isValid = true;

                //Save a copy of the original location
                int previousX = x;
                int previousY = y;
                int previousZ = z;

                //Add -1 to 1 to each value.
                x += rng.nextInt(-1, 2);
                y += rng.nextInt(-1, 2);
                z += rng.nextInt(-1, 2);

                //Ensure values are within chunk
                x = Math.clamp(x, 0, 32);
                y = Math.clamp(y, ore.getMinY(), ore.getMaxY());
                z = Math.clamp(z, 0, 32);

                //Values cannot all be 0 at the same time
                if(previousX == x && previousY == y && previousZ == z) {
                    isValid = false;
                } else {
                    //Cannot visit a location that has already been visited during this vein generation
                    for(Vector3i location : previousLocations) {
                        if(location == null) continue;
                        if(location.equals(x, y, z)) {
                            x = previousX;
                            y = previousY;
                            z = previousZ;
                            isValid = false;
                        }
                    }
                }

                //Pretty certain this can never trigger but its better to have this just in case.
                if(safety > 100) {
                    Ref<ChunkStore> ref = chunk.getBlockComponentEntity(x, y, z);
                    if(ref == null) {
                        ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + ore.getOre() + " ore vein at origin location: ERROR, but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                        return;
                    }

                    BlockModule.BlockStateInfo info = ref.getStore().getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                    if(info == null) {
                        ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + ore.getOre() + " ore vein at origin location: ERROR, but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                        return;
                    }

                    ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + ore.getOre() + " ore vein at origin location: " + x + ", " + y + ", " + z + ", but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                }
            }
        }
    }

    private ZoneBiomeResult getZoneBiomeResult(WorldChunk chunk, int x, int z) {

       // if (gen instanceof ChunkGenerator) {
       //     ChunkGenerator generator = (ChunkGenerator)gen;
       //     Arrays.stream(generator.getZonePatternProvider().getZones()).flatMap((zone) -> Arrays.stream(zone.biomePatternGenerator().getBiomes())).forEach((biome) -> result.suggest(biome.getName()));
       // }

        int seed = (int) chunk.getWorld().getWorldConfig().getSeed();

        IWorldGen gen = chunk.getWorld().getChunkStore().getGenerator();

        if(gen instanceof ChunkGenerator generator) {
            return generator.getZoneBiomeResultAt(seed, (chunk.getX() * 32) + x, (chunk.getZ() * 32) + z);
        } else {
            return null;
        }
    }

    private boolean isBiomeValid(OreConfig ore, ZoneBiomeResult result) {
        if(ore.getBiomes() == null) return true;
        if(ore.getBiomes().isEmpty()) return true;
        if(result == null) return false;

        String biomeName = result.getBiome().getName();

        if(ore.getBiomes().contains(biomeName)) {
            return ore.biomeIsWhite;
        } else {
            return !ore.biomeIsWhite;
        }
    }

    private boolean isZoneValid(OreConfig ore, ZoneBiomeResult result) {
        if(ore.getZones() == null) return true;
        if(result == null) return false;

        String zone = result.getZoneResult().getZone().name();

        int end = zone.indexOf("_");
        if(end == -1) return false;

        String sub = zone.substring(4, end);
        int value = Integer.parseInt(sub);

        for(int entry : ore.getZones()) {
            if(entry == value) {
                return ore.zoneIsWhite;
            }
        }

        return !ore.zoneIsWhite;
    }

    private long createSeed(long seed, int x, int z, String oreId) {
        long oreSeed = BASE_SEED;

        oreSeed = mix(oreSeed, seed);
        oreSeed = mix(oreSeed, x);
        oreSeed = mix(oreSeed, z);
        oreSeed = mix(oreSeed, "ArchStar".hashCode());
        oreSeed = mix(oreSeed, oreId.hashCode());

        return oreSeed;
    }

    private long mix(long seed, long value) {
        seed ^= value;
        seed *= BASE_PRIME;
        return seed;
    }
}
