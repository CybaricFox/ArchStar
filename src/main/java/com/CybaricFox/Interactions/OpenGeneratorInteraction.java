package com.CybaricFox.Interactions;

import com.CybaricFox.UI.Pages.CommonPage;
import com.CybaricFox.UI.Pages.GeneratorPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/*
    Interaction for opening generator blocks
 */
public class OpenGeneratorInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<OpenGeneratorInteraction> CODEC;

    public OpenGeneratorInteraction() {
    }

    protected void interactWithBlock(@Nonnull World world, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull Vector3i pos, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player playerComponent = commandBuffer.getComponent(ref, Player.getComponentType());

        if (playerComponent != null) {

            playerComponent.getPageManager().openCustomPage(ref, store, new GeneratorPage(commandBuffer.getComponent(ref, PlayerRef.getComponentType()), CommonPage.CommonData.CODEC, pos));

            //playerComponent.sendMessage(Message.raw("Max Energy: " + energyComponent.getMaxEnergy() + "\nCurrent Energy: " + energyComponent.getCurrentEnergy() + "\nInput Rate: " + energyComponent.getInputRate()));

            /*
            BlockState state = world.getState(pos.x, pos.y, pos.z, true);
            if (!(state instanceof GeneratorState)) {
                playerComponent.sendMessage(Message.translation("server.interactions.invalidBlockState").param("interaction", this.getClass().getSimpleName()).param("blockState", state != null ? state.getClass().getSimpleName() : "null"));
            } else {
                GeneratorState benchState = (GeneratorState)state;
                BlockType blockType = world.getBlockType(pos.x, pos.y, pos.z);
                Bench blockTypeBench = blockType.getBench();
                if ((blockTypeBench == null || !blockTypeBench.equals(benchState.getBench())) && !benchState.initialize(blockType)) {
                    GeneratorState.LOGGER.at(Level.WARNING).log("Failed to re-initialize: %s, %s", blockType.getId(), pos);
                    int x = pos.getX();
                    int z = pos.getZ();
                    world.getChunk(ChunkUtil.indexChunkFromBlock(x, z)).setState(x, pos.getY(), z, (BlockState)null);
                } else {
                    UUIDComponent uuidComponent = (UUIDComponent)commandBuffer.getComponent(ref, UUIDComponent.getComponentType());

                    assert uuidComponent != null;

                    UUID uuid = uuidComponent.getUuid();
                    GeneratorWindow window = new GeneratorWindow(benchState);
                    Map<UUID, GeneratorWindow> windows = benchState.getWindows();
                    if (windows.putIfAbsent(uuid, window) == null) {
                        benchState.updateFuelValues();
                        if (playerComponent.getPageManager().setPageWithWindows(ref, store, Page.Bench, true, new Window[]{window})) {
                            window.registerCloseEvent((event) -> {
                                windows.remove(uuid, window);
                                BlockType currentBlockType = world.getBlockType(pos);
                                String interactionState = BlockAccessor.getCurrentInteractionState(currentBlockType);
                                if (windows.isEmpty() && !"Processing".equals(interactionState) && !"ProcessCompleted".equals(interactionState)) {
                                    world.setBlockInteractionState(pos, currentBlockType, "default");
                                }

                                int soundEventIndex = blockType.getBench().getLocalCloseSoundEventIndex();
                                if (soundEventIndex != 0) {
                                    SoundUtil.playSoundEvent2d(ref, soundEventIndex, SoundCategory.UI, commandBuffer);
                                }
                            });
                            int soundEventIndex = blockType.getBench().getLocalOpenSoundEventIndex();
                            if (soundEventIndex == 0) {
                                return;
                            }

                            SoundUtil.playSoundEvent2d(ref, soundEventIndex, SoundCategory.UI, commandBuffer);
                        } else {
                            windows.remove(uuid, window);
                        }
                    }

                }
            }
             */
        }
    }

    protected void simulateInteractWithBlock(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nullable ItemStack itemInHand, @Nonnull World world, @Nonnull Vector3i targetBlock) {
    }

    static {
        CODEC = (BuilderCodec.builder(OpenGeneratorInteraction.class, OpenGeneratorInteraction::new, SimpleBlockInteraction.CODEC)).build();
    }
}
