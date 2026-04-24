package com.CybaricFox.Modules.ArchMachines.Interactions;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.CommonPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/*
    Interaction for opening powered processing blocks
 */
public class OpenMachineInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<OpenMachineInteraction> CODEC;

    public OpenMachineInteraction() {
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());
        if(playerComponent == null) return;

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, pos);

        MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
        if(machineBehaviorComponent == null) {
            ArchStar.LOGGER.at(Level.SEVERE).log(playerComponent.getDisplayName() + " tried to interact with a machine interface but the block is not a machine!");
            return;
        }

        String sound = machineBehaviorComponent.getOpenSound();
        if(sound != null) {
            int soundIndex = SoundEvent.getAssetMap().getIndex(sound);
            SoundUtil.playSoundEvent3dToPlayer(ref, soundIndex, SoundCategory.UI, pos.toVector3d(), store);
        }
        playerComponent.getPageManager().openCustomPage(ref, store, machineBehaviorComponent.getPage(commandBuffer.getComponent(ref, PlayerRef.getComponentType()), CommonPage.CommonData.CODEC, pos));
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    static {
        CODEC = (BuilderCodec.builder(OpenMachineInteraction.class, OpenMachineInteraction::new, SimpleBlockInteraction.CODEC)).build();
    }
}
