package com.CybaricFox.Modules.ArchLibrary.OreGeneration;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

public class OreGenerator{
    private final long BASE_SEED = 0xcbf29ce484222325L;
    private final long BASE_PRIME = 0x100000001b3L;

    public ArrayList<OreHolder> oreGenQueue = new ArrayList<>();

    private OreConfig tempOre = new OreConfig("Ore_Tin_Stone", 0, 26, 1, 6, 40, 80, new String[]{"Rock_Stone"});

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

        rng.setSeed(createSeed(chunk.getWorld().getWorldConfig().getSeed(), x, z, tempOre.getOre()));

        int pockets = rng.nextInt(tempOre.getMinPockets(), tempOre.getMaxPockets() + 1);

        for(int i = 0; i < pockets; i++) {
            generateVein(rng, tempOre.getMinVeinSize(), tempOre.getMaxVeinSize(), tempOre.getMinY(), tempOre.getMaxY(), tempOre.getOre(), tempOre.getHostBlocks(), chunk);
        }
    }

    private void generateVein(Random rng, int minVeinSize, int maxVeinSize, int minY, int maxY, String id, String[] hostBlocks, WorldChunk chunk) {
        boolean isValidFirstLocation = false;

        //Get source location
        int x = rng.nextInt(0, 33);
        int z = rng.nextInt(0, 33);
        int y = rng.nextInt(minY, maxY + 1);

        //Always try to make it so the original location picked for a vein is always valid.
        int originSafety = 0; //Prevents infinite looping
        while(!isValidFirstLocation) {
            originSafety++;
            BlockType type = chunk.getBlockType(new Vector3i(x, y, z));

            if(type != null) {
                String existingId = type.getId();

                for(String block : hostBlocks) {
                    if(block.equals(existingId)) {
                        isValidFirstLocation = true;
                        break;
                    }
                }
            }

            //If invalid first location, reroll the starting location.
            if(!isValidFirstLocation) {
                x = rng.nextInt(0, 33);
                z = rng.nextInt(0, 33);
                y = rng.nextInt(minY, maxY + 1);
            }

            //If safety threshold reached, force the vein to generate where it currently is, even if it's not a good location.
            if(originSafety > 10 && !isValidFirstLocation) {
                ArchLibrary.LOGGER.at(Level.WARNING).log("Failed to find a valid host block for " + id + " within safety parameters! Vein will be force generated. This can be safely ignored.");
                isValidFirstLocation = true;
            }
        }

        //Get vein size
        int veinSize = rng.nextInt(minVeinSize, maxVeinSize + 1);
        Vector3i[] previousLocations = new Vector3i[veinSize];

        for(int i = 0; i < veinSize; i++) {
            boolean isValid = false;

            //Check the block at this location. If its in the array, it may be replaced
            //If its not in the array, this block will not generate.
            BlockType type = chunk.getBlockType(new Vector3i(x, y, z));
            if(type != null) {
                String existingId = type.getId();

                for(String block : hostBlocks) {
                    if(block.equals(existingId)) {
                        //Generate the ore
                        chunk.setBlock(x, y, z, id);
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
                y = Math.clamp(y, minY, maxY);
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
                        ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + id + " ore vein at origin location: ERROR, but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                        return;
                    }

                    BlockModule.BlockStateInfo info = ref.getStore().getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
                    if(info == null) {
                        ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + id + " ore vein at origin location: ERROR, but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                        return;
                    }

                    ArchLibrary.LOGGER.at(Level.SEVERE).log("Attempted to generate a " + id + " ore vein at origin location: " + x + ", " + y + ", " + z + ", but safety was triggered! Ore veins larger than 26 have a risk of failing. If your ore vein is less than 26, there may be a major issue. Please report this to the github page.");
                }
            }
        }
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
