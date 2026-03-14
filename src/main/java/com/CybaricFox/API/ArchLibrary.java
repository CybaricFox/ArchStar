package com.CybaricFox.API;

import com.CybaricFox.ArchStar;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.FillerBlockUtil;

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

//General Library class for common functions
public class ArchLibrary {
    private static final Random rng = new Random();

    public static boolean activateDebug = false;

    //Converts global coordinates to local chunk coordinates
    public static Vector3i convertToLocalCoords(Vector3i globalCoords) {
        return new Vector3i(
                Math.floorMod(globalCoords.x, 32),
                globalCoords.y,
                Math.floorMod(globalCoords.z, 32)
        );
    }

    //Converts a chunk coordinate into world coordinate
    public static Vector3i getGlobalCoordsFromChunk(BlockModule.BlockStateInfo info, WorldChunk worldChunk) {
        int x = ChunkUtil.xFromBlockInColumn(info.getIndex()) + (worldChunk.getX() * 32);
        int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int z = ChunkUtil.zFromBlockInColumn(info.getIndex()) + (worldChunk.getZ() * 32);

        return new Vector3i(x, y, z);
    }

    //Returns a block entity at the world location if one exists
    public static Ref<ChunkStore> getBlockEntity(World world, Vector3i globalPos) {
        if(globalPos == null) return null;

        //Convert global coords to local coords
        Vector3i localCoords = ArchLibrary.convertToLocalCoords(globalPos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(globalPos.x, globalPos.z));
        if(chunk == null) return null;

        Vector3i realLocation = localCoords;

        @SuppressWarnings("removal") int filler = chunk.getFiller(realLocation.x, realLocation.y, realLocation.z);
        if (filler != 0) {
            realLocation = new Vector3i(realLocation.x - FillerBlockUtil.unpackX(filler), realLocation.y - FillerBlockUtil.unpackY(filler), realLocation.z - FillerBlockUtil.unpackZ(filler));
        }

        return chunk.getBlockComponentEntity(realLocation.x, realLocation.y, realLocation.z);
    }
    public static Ref<ChunkStore> getBlockEntity(BlockComponentChunk chunk, Vector3i localCoords) {
        return chunk.getEntityReference(ChunkUtil.indexBlockInColumn(localCoords.x, localCoords.y, localCoords.z));
    }

    public static void spawnItems(World world, Vector3i pos, ArrayList<ItemStack> items) {
        if(items.isEmpty()) return;

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Vector3d centerPos = new Vector3d(pos.x + 0.5, pos.y + 0.7, pos.z + 0.5);

            for(ItemStack item : items) {
                //The more items that are dropped, the larger they are spread out. Single item never spreads.
                Vector3f velocity = new Vector3f(getItemDropVelocity(items.size()), -1, getItemDropVelocity(items.size()));
                Holder<EntityStore> holder = ItemComponent.generateItemDrop(store, item, centerPos, new Vector3f(), velocity.x, velocity.y, velocity.z);
                store.addEntity(holder, AddReason.SPAWN);
            }
        });
    }

    public static void changeBlockState(Ref<ChunkStore> ref, CommandBuffer<ChunkStore> buffer, String stateName) {
        EssentialsContext context = new EssentialsContext(ref, buffer);

        //Get the block state
        BlockType type = context.world.getBlockType(context.pos);

        context.world.setBlockInteractionState(context.pos, type, stateName);
    }
    public static void changeBlockState(EssentialsContext context, String stateName) {
        //Get the block state
        BlockType type = context.world.getBlockType(context.pos);

        context.world.setBlockInteractionState(context.pos, type, stateName);
    }

    public static void registerCustomPagePackets(HytaleLogger LOGGER) {
        PacketAdapters.registerInbound((PacketHandler handler, Packet packet) -> {
            if(packet instanceof CustomPageEvent) {
                if(((CustomPageEvent) packet).data != null) {
                    LOGGER.at(Level.INFO).log("Packet Received: " + ((CustomPageEvent) packet).data);
                }
            }
        });
    }

    private static float getItemDropVelocity(int modifier) {
        if(modifier == 1) return 0;

        int bound = modifier - 1;

        return rng.nextFloat(-bound, bound);
    }

    //Returned in order of North South East West Up Down
    public static ArrayList<Vector3i> getNeighborVectors(Vector3i location) {
        ArrayList<Vector3i> neighbors = new ArrayList<>();

        //Global coords
        neighbors.addLast(new Vector3i(location.x, location.y, location.z - 1));
        neighbors.addLast(new Vector3i(location.x, location.y, location.z + 1));
        neighbors.addLast(new Vector3i(location.x + 1, location.y, location.z));
        neighbors.addLast(new Vector3i(location.x - 1, location.y, location.z));
        neighbors.addLast(new Vector3i(location.x, location.y + 1, location.z));
        neighbors.addLast(new Vector3i(location.x, location.y - 1, location.z));
        return neighbors;
    }

    //Used to get messages that are normally silent
    public static void printDebugMessage(Level level, String message) {
        if(activateDebug) {
            ArchStar.LOGGER.at(level).log(message);
        }
    }
}
