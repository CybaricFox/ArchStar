package com.CybaricFox.Interactions;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.UI.Pages.CommonPage;
import com.CybaricFox.UI.Pages.PoweredProcessingPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
    Interaction for opening powered processing blocks
 */
public class OpenPoweredProcessorInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<OpenPoweredProcessorInteraction> CODEC;

    public OpenPoweredProcessorInteraction() {
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());

        if (playerComponent != null) {
            int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Furnace_Bench_Open");
            SoundUtil.playSoundEvent3dToPlayer(ref, soundIndex, SoundCategory.UI, pos.toVector3d(), store);
            playerComponent.getPageManager().openCustomPage(ref, store, new PoweredProcessingPage(commandBuffer.getComponent(ref, PlayerRef.getComponentType()), CommonPage.CommonData.CODEC, pos));
        }
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    static {
        CODEC = (BuilderCodec.builder(OpenPoweredProcessorInteraction.class, OpenPoweredProcessorInteraction::new, com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction.CODEC)).build();
    }
}
