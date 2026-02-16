package com.CybaricFox.API;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageEvent;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
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

import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;

//General Library class for common functions
public class FoxLibrary {
    private static final Random rng = new Random();

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
        //Convert global coords to local coords
        Vector3i localCoords = FoxLibrary.convertToLocalCoords(globalPos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(globalPos.x, globalPos.z));
        if(chunk == null) return null;

        Ref<ChunkStore> chunkRef = chunk.getReference();

        BlockComponentChunk blockComponentChunk = chunkRef.getStore().getComponent(chunkRef, BlockComponentChunk.getComponentType());

        chunk.getBlockType(localCoords);

        //Get the index of the block in the chunk
        int index = ChunkUtil.indexBlockInColumn(localCoords.x, localCoords.y, localCoords.z);

        return blockComponentChunk.getEntityReference(index);
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
        //Get the global position
        BlockModule.BlockStateInfo info = buffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if(info == null) {
            return;
        }
        WorldChunk chunk = buffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
        if(chunk == null) {
            return;
        }

        Vector3i pos = FoxLibrary.getGlobalCoordsFromChunk(info, chunk);

        //Get the block state
        World world = buffer.getExternalData().getWorld();
        BlockType type = world.getBlockType(pos);

        world.setBlockInteractionState(pos, type, stateName);
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
}
