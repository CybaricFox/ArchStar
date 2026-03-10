package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.Direction;
import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.ConveyorComponent;
import com.CybaricFox.Components.Blocks.InputComponent;
import com.CybaricFox.Components.Blocks.OutputComponent;
import com.CybaricFox.Components.Helpers.ConveyorState;
import com.hypixel.hytale.builtin.crafting.state.ProcessingBenchState;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.bench.ProcessingBench;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ConveyorSystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float v, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());
        if(blocks == null) {
            return;
        }

        if (blocks.getTickingBlocksCountCopy() != 0) {
            ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());
            if(section == null) {
                return;
            }

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
            if(blockComponentChunk == null) {
                return;
            }

            blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
            {
                Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(blockComponentChunk1, new Vector3i(localX, localY, localZ));
                if (blockRef == null) {
                    return BlockTickStrategy.IGNORED;
                }

                EssentialsContext context = new EssentialsContext(blockRef, commandBuffer);

                ConveyorComponent conveyorComponent = commandBuffer1.getComponent(blockRef, ArchStar.get().getConveyorComponentType());

                if(conveyorComponent == null) {
                    return BlockTickStrategy.IGNORED;
                }

                BlockTickStrategy strategy = BlockTickStrategy.IGNORED;

                switch(conveyorComponent.getType()) {
                    case CONVEYOR -> strategy = handleConveyor(conveyorComponent, context.world, new Vector3i(localX, localY, localZ), context.pos, commandBuffer1);
                    case IMPORT -> strategy = handleImport(conveyorComponent, context.world, new Vector3i(localX, localY, localZ), context.pos, commandBuffer1);
                    case EXPORT -> strategy = handleExport(conveyorComponent, context.world, new Vector3i(localX, localY, localZ), context.pos, commandBuffer1);
                }

                return strategy;
            });
        }
    }

    private CompletableFuture<Vector3i> getTargetBlock(World world, Vector3i localCoords, Vector3i globalCoords, boolean reverse) {
        CompletableFuture<Vector3i> future = new CompletableFuture<>();

        world.execute(() -> {
            int rotationIndex = world.getBlockRotationIndex(globalCoords.x, globalCoords.y, globalCoords.z);
            Direction direction = FoxLibrary.getForwardDirection(rotationIndex, reverse);
            Vector3i result = FoxLibrary.getCoordsFromDirection(direction, globalCoords);
            future.complete(result);
        });

        return future;
    }

    //Handles conveyor behaviour
    private BlockTickStrategy handleConveyor(ConveyorComponent conveyorComponent, World world, Vector3i localCoords, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer) {
        //Check the state of the target block
        Vector3i target;
        CompletableFuture<Vector3i> targetFuture;
        //Check that the conveyor has a target block in memory, otherwise set it manually
        if(conveyorComponent.getTargetBlock() == null) {
            targetFuture = getTargetBlock(world, localCoords, globalCoords, false);
            targetFuture.thenAccept(conveyorComponent::setTargetBlock);
            return BlockTickStrategy.CONTINUE;
        }

        target = conveyorComponent.getTargetBlock();
        Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(world, target);

        //Block reference can only be null if there is no block at that location
        if(blockRef == null) {
            switch(conveyorComponent.state) {
                case WAIT -> {
                    conveyorComponent.state = ConveyorState.TRANSFER;
                    conveyorComponent.startTimer();
                }
                case TRANSFER -> {
                    FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, globalCoords), buffer, "Move");
                    conveyorComponent.decrementTimer();

                    if(conveyorComponent.getTimer() == 0) {
                        ArrayList<ItemStack> items = new ArrayList<>();
                        ItemStack item = conveyorComponent.getItem();
                        items.add(item);

                        conveyorComponent.removeItem();
                        conveyorComponent.state = ConveyorState.OPEN;
                        FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, globalCoords), buffer, "default");

                        FoxLibrary.spawnItems(world, globalCoords, items);
                        return BlockTickStrategy.IGNORED;
                    }
                }
            }
            return BlockTickStrategy.CONTINUE;
        }

        ConveyorComponent targetConveyor = buffer.getComponent(blockRef, ArchStar.get().getConveyorComponentType());

        //Check that the target block has a conveyor component
        if(targetConveyor == null) {
            return BlockTickStrategy.CONTINUE;
        };

        switch(targetConveyor.state) {
            case OPEN, TRANSFER -> {
                return handleConveyorState(conveyorComponent, targetConveyor, target, buffer, world, globalCoords);
            }
        }

        return BlockTickStrategy.CONTINUE;
    }

    private BlockTickStrategy handleConveyorState(ConveyorComponent conveyorComponent, ConveyorComponent targetConveyor, Vector3i target, CommandBuffer<ChunkStore> buffer, World world, Vector3i pos) {
        switch(conveyorComponent.state) {
            case WAIT -> {
                conveyorComponent.state = ConveyorState.TRANSFER;
                conveyorComponent.startTimer();
            }
            case TRANSFER -> {
                FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "Move");
                conveyorComponent.decrementTimer();

                if(conveyorComponent.getTimer() == 0) {
                    ItemStack item = conveyorComponent.getItem();

                    //Do not output until the conveyor state becomes open
                    if(targetConveyor.state != ConveyorState.OPEN) return BlockTickStrategy.CONTINUE;

                    conveyorComponent.removeItem();
                    conveyorComponent.state = ConveyorState.OPEN;
                    FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "default");

                    targetConveyor.setItem(item);
                    targetConveyor.state = ConveyorState.WAIT;
                    ArchStar.get().getConveyorPlaceSystem().changeTickState(target, buffer, true);

                    return BlockTickStrategy.IGNORED;
                }
            }
            case IMPORT_TRANSFER_OUT -> {
                FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "Move");
                conveyorComponent.decrementTimer();

                if(conveyorComponent.getTimer() == 0) {
                    ItemStack item = conveyorComponent.getItem();

                    //Do not output until the conveyor state becomes open
                    if(targetConveyor.state != ConveyorState.OPEN) return BlockTickStrategy.CONTINUE;

                    conveyorComponent.removeItem();
                    conveyorComponent.state = ConveyorState.OPEN;
                    FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "default");

                    targetConveyor.setItem(item);
                    targetConveyor.state = ConveyorState.WAIT;
                    ArchStar.get().getConveyorPlaceSystem().changeTickState(target, buffer, true);
                }
            }
            case IMPORT_TRANSFER_IN -> {
                FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "Move");
                conveyorComponent.decrementTimer();

                if(conveyorComponent.getIOTimer() == 0) {
                    ItemStack item = conveyorComponent.getIOItem();

                    conveyorComponent.removeIOItem();
                    conveyorComponent.setItem(item);
                    conveyorComponent.state = ConveyorState.WAIT;
                    FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "default");
                }
            }
            case IMPORT_TRANSFER_IO -> {
                FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, pos), buffer, "Move");
                conveyorComponent.decrementTimer();

                if(conveyorComponent.getTimer() == 0) {
                    ItemStack item = conveyorComponent.getItem();

                    //Do not output until the conveyor state becomes open
                    if(targetConveyor.state != ConveyorState.OPEN) return BlockTickStrategy.CONTINUE;

                    conveyorComponent.removeItem();
                    conveyorComponent.state = ConveyorState.IMPORT_TRANSFER_IN;

                    targetConveyor.setItem(item);
                    targetConveyor.state = ConveyorState.WAIT;
                    ArchStar.get().getConveyorPlaceSystem().changeTickState(target, buffer, true);
                }
            }
        }

        return BlockTickStrategy.CONTINUE;
    }

    private BlockTickStrategy handleImport(ConveyorComponent conveyorComponent, World world, Vector3i localCoords, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer) {
        //Check the state of the target block
        Vector3i target;
        CompletableFuture<Vector3i> targetFuture;
        //Check that the conveyor has a target block in memory, otherwise set it manually
        if(conveyorComponent.getTargetBlock() == null) {
            targetFuture = getTargetBlock(world, localCoords, globalCoords, true);
            targetFuture.thenAccept(conveyorComponent::setTargetBlock);
            return BlockTickStrategy.CONTINUE;
        }

        target = conveyorComponent.getTargetBlock();
        Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(world, target);
        ConveyorComponent targetConveyor = null;
        if(blockRef != null) {
            targetConveyor = buffer.getComponent(blockRef, ArchStar.get().getConveyorComponentType());
        }

        //Check the state of the target block
        Vector3i importTarget;
        CompletableFuture<Vector3i> importTargetFuture;
        //Check that the conveyor has a target block in memory, otherwise set it manually
        if(conveyorComponent.getTargetMachine() == null) {
            importTargetFuture = getTargetBlock(world, localCoords, globalCoords, false);
            importTargetFuture.thenAccept(conveyorComponent::setTargetMachine);
            return BlockTickStrategy.CONTINUE;
        }

        importTarget = conveyorComponent.getTargetMachine();
        Ref<ChunkStore> importRef = FoxLibrary.getBlockEntity(world, importTarget);

        OutputComponent outputComponent = null;
        ItemContainerState containerState = null;
        ProcessingBenchState benchState = null;

        if(importRef != null) {
            outputComponent = buffer.getComponent(importRef, ArchStar.get().getOutputComponentType());
            //noinspection deprecation
            if (world.getState(importTarget.x, importTarget.y, importTarget.z, true) instanceof ItemContainerState itemContainerState) {
                containerState = itemContainerState;
            } else //noinspection deprecation
                if (world.getState(importTarget.x, importTarget.y, importTarget.z, true) instanceof ProcessingBenchState processingBenchState) {
                    benchState = processingBenchState;
                }
        }

        switch(conveyorComponent.state) {
            case OPEN -> {
                if(outputComponent == null && containerState == null && benchState == null) return BlockTickStrategy.CONTINUE;

                ItemStack item = null;

                if(outputComponent != null) {
                    short slot = outputComponent.getSlotWithFirstItem();
                    if(slot != -1) {
                        item = outputComponent.getItemStack(outputComponent.getSlotWithFirstItem());
                        outputComponent.getContainer().removeItemStackFromSlot(slot);
                    }
                }
                else if(containerState != null) {
                    for(short i = 0; i < containerState.getItemContainer().getCapacity(); i++) {
                        if(containerState.getItemContainer().getItemStack(i) != null) {
                            item = containerState.getItemContainer().getItemStack(i);
                            containerState.getItemContainer().removeItemStackFromSlot(i);
                            break;
                        }
                    }
                }
                else if(benchState != null) {
                    short avoid = (short) (benchState.getItemContainer().getContainer(0).getCapacity() + benchState.getItemContainer().getContainer(1).getCapacity());
                    for(short i = avoid; i < benchState.getItemContainer().getCapacity(); i++) {
                        if(benchState.getItemContainer().getItemStack(i) != null) {
                            item = benchState.getItemContainer().getItemStack(i);
                            benchState.getItemContainer().removeItemStackFromSlot(i);
                            break;
                        }
                    }
                }

                if(item == null) return BlockTickStrategy.CONTINUE;

                conveyorComponent.setIOItem(item);
                conveyorComponent.state = ConveyorState.IMPORT_TRANSFER_IN;
                conveyorComponent.startIOTimer();
            }
            case WAIT -> {
                if(targetConveyor != null) {
                    switch(targetConveyor.state) {
                        case OPEN, TRANSFER -> {
                            conveyorComponent.state = ConveyorState.IMPORT_TRANSFER_OUT;
                            conveyorComponent.startTimer();
                        }
                    }
                }
            }
            case IMPORT_TRANSFER_IN, IMPORT_TRANSFER_IO -> {
                if(targetConveyor == null) return BlockTickStrategy.CONTINUE;
                handleConveyorState(conveyorComponent, targetConveyor, target, buffer, world, globalCoords);
            }
            case IMPORT_TRANSFER_OUT -> {
                ItemStack item = null;

                if(outputComponent != null) {
                    short slot = outputComponent.getSlotWithFirstItem();
                    if(slot != -1) {
                        item = outputComponent.getItemStack(slot);
                        outputComponent.getContainer().removeItemStackFromSlot(slot);
                    }
                }
                else if(containerState != null) {
                    for(short i = 0; i < containerState.getItemContainer().getCapacity(); i++) {
                        if(containerState.getItemContainer().getItemStack(i) != null) {
                            item = containerState.getItemContainer().getItemStack(i);
                            containerState.getItemContainer().removeItemStackFromSlot(i);
                            break;
                        }
                    }
                }
                else if(benchState != null) {
                    short avoid = benchState.getItemContainer().getContainer(0).getCapacity();
                    for(short i = avoid; i < benchState.getItemContainer().getCapacity(); i++) {
                        if(benchState.getItemContainer().getItemStack(i) != null) {
                            item = benchState.getItemContainer().getItemStack(i);
                            benchState.getItemContainer().removeItemStackFromSlot(i);
                            break;
                        }
                    }
                }

                if(item != null) {
                    conveyorComponent.setIOItem(item);
                    conveyorComponent.startIOTimer();
                    conveyorComponent.state = ConveyorState.IMPORT_TRANSFER_IO;
                    return BlockTickStrategy.CONTINUE;
                }

                if(targetConveyor == null) return BlockTickStrategy.CONTINUE;
                handleConveyorState(conveyorComponent, targetConveyor, target, buffer, world, globalCoords);
            }
        }

        return BlockTickStrategy.CONTINUE;
    }

    private BlockTickStrategy handleExport(ConveyorComponent conveyorComponent, World world, Vector3i localCoords, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer) {
        //Check the state of the target block
        Vector3i target;
        CompletableFuture<Vector3i> targetFuture;
        //Check that the conveyor has a target block in memory, otherwise set it manually
        if(conveyorComponent.getTargetBlock() == null) {
            targetFuture = getTargetBlock(world, localCoords, globalCoords, false);
            targetFuture.thenAccept(conveyorComponent::setTargetBlock);
            return BlockTickStrategy.CONTINUE;
        }

        target = conveyorComponent.getTargetBlock();
        Ref<ChunkStore> blockRef = FoxLibrary.getBlockEntity(world, target);
        InputComponent targetMachine = null;
        ItemContainerState containerState = null;
        ProcessingBenchState benchState = null;

        if(blockRef != null) {
            targetMachine = buffer.getComponent(blockRef, ArchStar.get().getInputComponentType());
            //noinspection deprecation
            if (world.getState(target.x, target.y, target.z, true) instanceof ItemContainerState itemContainerState) {
                containerState = itemContainerState;
            } else //noinspection deprecation
                if (world.getState(target.x, target.y, target.z, true) instanceof ProcessingBenchState processingBenchState) {
                    benchState = processingBenchState;
                }
        }

        switch(conveyorComponent.state) {
            case WAIT -> {
                if(targetMachine == null && containerState == null && benchState == null) return BlockTickStrategy.CONTINUE;

                conveyorComponent.startTimer();
                conveyorComponent.state = ConveyorState.TRANSFER;
            }
            case TRANSFER -> {
                FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, globalCoords), buffer, "Move");
                conveyorComponent.decrementTimer();

                if(conveyorComponent.getTimer() == 0) {
                    if(targetMachine == null && containerState == null && benchState == null) return BlockTickStrategy.CONTINUE;

                    ItemStack item = conveyorComponent.getItem();

                    short slot = -1;
                    if(targetMachine != null) {
                        for(short i = 0; i < targetMachine.getCapacity(); i++) {
                            if(targetMachine.getContainer().canAddItemStackToSlot(i, item, false, false)) {
                                slot = i;
                                targetMachine.getContainer().addItemStackToSlot(slot, item);
                                break;
                            }
                        }
                    } else if(containerState != null){
                        for(short i = 0; i < containerState.getItemContainer().getCapacity(); i++) {
                            if(containerState.getItemContainer().canAddItemStackToSlot(i, item, false, false)) {
                                slot = i;
                                containerState.getItemContainer().addItemStackToSlot(i, item);
                                break;
                            }
                        }
                    } else if (benchState != null){
                        for(short i = 0; i < benchState.getItemContainer().getCapacity(); i++) {
                            if(benchState.getItemContainer().canAddItemStackToSlot(i, item, false, true)) {
                                slot = i;
                                benchState.getItemContainer().addItemStackToSlot(i, item);
                                break;
                            }
                        }
                    }

                    if(slot == -1) return BlockTickStrategy.CONTINUE;

                    conveyorComponent.removeItem();
                    conveyorComponent.state = ConveyorState.OPEN;
                    FoxLibrary.changeBlockState(FoxLibrary.getBlockEntity(world, globalCoords), buffer, "default");

                    return BlockTickStrategy.IGNORED;
                }
            }
        }

        return BlockTickStrategy.CONTINUE;
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }
}
