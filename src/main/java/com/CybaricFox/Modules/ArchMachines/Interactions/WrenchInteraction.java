package com.CybaricFox.Modules.ArchMachines.Interactions;

import com.CybaricFox.Modules.ArchEnergy.Components.EnergyCableComponent;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.InventoryUtils;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;

/*
    Interaction for opening powered processing blocks
 */
public class WrenchInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<WrenchInteraction> CODEC;

    public WrenchInteraction() {
    }

    @Override
    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
        //Item must be valid and have durability
        if(itemStack == null) return;
        if(itemStack.getDurability() <= 0) {
            return;
        }

        Ref<EntityStore> ref = interactionContext.getEntity();
        Store<EntityStore> store = ref.getStore();
        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, pos);
        if(blockRef == null) return;
        BlockType blockType = world.getBlockType(pos);
        if(blockType == null) return;
        Item item = blockType.getItem();
        if(item == null) return;

        if(!item.getData().getRawTags().containsKey("ArchStar")) return;

        String[] tags = blockType.getItem().getData().getRawTags().get("ArchStar");
        boolean isWrenchable = false;
        boolean costsDurability = false;
        for (String tag : tags) {
            switch (tag) {
                case "Wrenchable" -> isWrenchable = true;
                case "Wrenchable_Durability" -> costsDurability = true;
            }
        }

        if(!isWrenchable && !costsDurability) return;

        InventoryComponent inventoryComponent = commandBuffer.getComponent(ref, InventoryComponent.getComponentTypeById(-1));
        if(inventoryComponent == null) return;
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if(chunk == null) return;

        //Break the block and spawn the machine as an item
        world.execute(() -> {
            chunk.setBlock(pos.x, pos.y, pos.z, BlockType.EMPTY);
            ArchLibrary.spawnItems(world, pos, new ArrayList<>(Collections.singleton(new ItemStack(blockType.getItem().getId()))));
        });

        if(costsDurability) {
            //Decrement item durability
            byte activeSlot = InventoryUtils.getActiveSlot(ref, -1, store);
            inventoryComponent.getInventory().setItemStackForSlot(activeSlot, itemStack.withIncreasedDurability(-1));
        }
    }

    @Override
    protected void simulateInteractWithBlock(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nullable ItemStack itemStack, @Nonnull World world, @Nonnull Vector3i vector3i) {

    }

    static {
        CODEC = (BuilderCodec.builder(WrenchInteraction.class, WrenchInteraction::new, SimpleBlockInteraction.CODEC)).build();
    }
}
