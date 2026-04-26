package com.CybaricFox.Modules.ArchTransport.Systems;

import com.CybaricFox.Modules.ArchMachines.Components.FuelComponent;
import com.CybaricFox.Modules.ArchMachines.Components.InputComponent;
import com.CybaricFox.Modules.ArchMachines.Components.OutputComponent;
import com.CybaricFox.Modules.ArchLibrary.*;
import com.CybaricFox.Modules.ArchTransport.*;
import com.hypixel.hytale.builtin.crafting.component.ProcessingBenchBlock;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.ItemResourceType;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ConveyorSystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float v, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        TickContext tickContext = new TickContext(index, archetypeChunk, commandBuffer);
        if(!tickContext.isValid) return;

        tickContext.blockSection.forEachTicking(tickContext.blockComponentChunk, commandBuffer, tickContext.chunkSection.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
        {
            Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(blockComponentChunk1, new Vector3i(localX, localY, localZ));
            if (blockRef == null) {
                return BlockTickStrategy.IGNORED;
            }

            EssentialsContext context = new EssentialsContext(blockRef, commandBuffer);
            if(!context.isValid) return BlockTickStrategy.IGNORED;

            ConveyorComponent conveyorComponent = commandBuffer1.getComponent(blockRef, ConveyorComponent.getComponentType());

            if(conveyorComponent == null) {
                return BlockTickStrategy.IGNORED;
            }

            if(!conveyorComponent.isValid) {
                setTargetBlock(context.world, context.pos, conveyorComponent);
                return BlockTickStrategy.CONTINUE;
            }

            if(!conveyorComponent.isEmpty() && conveyorComponent.getType() != ConveyorType.ROUTER) {
                ArchLibrary.changeBlockState(blockRef, commandBuffer1, "Move");

                Ref<ChunkStore> targetConveyor = ArchLibrary.getBlockEntity(context.world, conveyorComponent.getTargetBlock());
                if(targetConveyor != null) {
                    ConveyorComponent conveyer = commandBuffer1.getComponent(targetConveyor, ConveyorComponent.getComponentType());
                    if(conveyer != null && conveyer.getType() != ConveyorType.ROUTER) {
                        ArchLibrary.changeBlockState(targetConveyor, commandBuffer1, "Move");
                    }
                }
            }

            BlockTickStrategy strategy = BlockTickStrategy.IGNORED;

            switch(conveyorComponent.getType()) {
                case CONVEYOR -> strategy = handleConveyor(conveyorComponent, context.world, commandBuffer1, context.chunk, context.pos);
                case IMPORT -> strategy = handleImport(conveyorComponent, context.world, context.pos, commandBuffer1, context.chunk);
                case EXPORT -> strategy = handleExport(conveyorComponent, context.world, context.pos, commandBuffer1);
                case ROUTER -> strategy = handleRouter(conveyorComponent, context.world, context.pos, commandBuffer1, context.chunk);
            }

            return strategy;
        });
    }

    private BlockTickStrategy handleRouter(ConveyorComponent conveyorComponent, World world, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer, WorldChunk chunk) {
        //Setup router if not yet setup
        ConveyorRouter router = conveyorComponent.getRouterData();
        if(router == null) {
            conveyorComponent.setRouterData(globalCoords, world);
            return BlockTickStrategy.CONTINUE;
        }

        //Handle output to target
        ArrayList<ConveyorInstance> readyItems = conveyorComponent.decrementTimers();
        if(readyItems.isEmpty()) return BlockTickStrategy.CONTINUE;

        router.setOUT(globalCoords, world);
        if(router.noOut() || router.noPath) {
            ejectItems(readyItems, DirectionLibrary.getCoordsFromDirection(Direction.UP, globalCoords), world);
            conveyorComponent.removeReadyItems();
            if(conveyorComponent.isEmpty()) {
                return BlockTickStrategy.IGNORED;
            } else {
                return BlockTickStrategy.CONTINUE;
            }
        }

        for(ConveyorInstance instance : readyItems) {
            boolean structureFailed = false;
            Vector3i targetLocation = DirectionLibrary.getCoordsFromDirection(router.getNextDirection(instance.from), globalCoords);
            if(targetLocation == null) {
                //This can only be NOT_SET if out is null, so we can safely stop here
                //This can only ever run on the first iteration
                return BlockTickStrategy.CONTINUE;
            }
            Direction direction = router.getLastDirection();

            //These are guaranteed because of the router object
            Ref<ChunkStore> targetRef = ArchLibrary.getBlockEntity(world, targetLocation);
            ConveyorComponent targetConveyorComponent = buffer.getComponent(targetRef, ConveyorComponent.getComponentType());

            if(!targetConveyorComponent.canAddItem()) {
                ArrayList<ConveyorInstance> temp = new ArrayList<>();
                temp.add(instance);
                ejectItems(temp, targetLocation, world);
                structureFailed = true;
            } else {
                ConveyorInstance newInstance = instance.clone();
                newInstance.from = DirectionLibrary.getBackwardDirection(direction);
                newInstance.to = targetConveyorComponent.getForwardDirection();

                targetConveyorComponent.addItem(newInstance);
                ArchTransportModule.get().getConveyorRefSystem().changeTickState(targetLocation, buffer, true);
            }

            if(structureFailed) {
                structureFailure(targetConveyorComponent, targetLocation, world, chunk);
            }
        }

        conveyorComponent.removeReadyItems();

        if(conveyorComponent.isEmpty()) {
            return BlockTickStrategy.IGNORED;
        } else {
            return BlockTickStrategy.CONTINUE;
        }
    }

    private void setTargetBlock(World world, Vector3i globalCoords, ConveyorComponent component) {
        CompletableFuture<Vector3i> future = new CompletableFuture<>();
        CompletableFuture<Direction> futureDirection = new CompletableFuture<>();

        world.execute(() -> {
            int rotationIndex = world.getBlockRotationIndex(globalCoords.x, globalCoords.y, globalCoords.z);
            Direction direction = DirectionLibrary.getForwardDirection(rotationIndex);
            if(component.getType() == ConveyorType.IMPORT) {
                direction = DirectionLibrary.getBackwardDirection(direction);
            }
            if(component.getType() == ConveyorType.ROUTER) {
                direction = Direction.NOT_SET;
            }
            Vector3i result = DirectionLibrary.getCoordsFromDirection(direction, globalCoords);
            future.complete(result);
            futureDirection.complete(direction);
        });

        future.thenAccept(component::setTarget);
        futureDirection.thenAccept(component::setForwardDirection);
    }

    //Handles conveyor behaviour
    private BlockTickStrategy handleConveyor(ConveyorComponent conveyorComponent, World world, CommandBuffer<ChunkStore> buffer, WorldChunk chunk, Vector3i pos) {
        boolean structureFailed = false;

        //Handle output to target
        ArrayList<ConveyorInstance> readyItems = conveyorComponent.decrementTimers();
        conveyorComponent.updateEntityLocations(pos, conveyorComponent.getTargetBlock(), world);

        if(readyItems.isEmpty()) return BlockTickStrategy.CONTINUE;

        //If the target is not a conveyor, eject the items!
        Ref<ChunkStore> targetConveyor = ArchLibrary.getBlockEntity(world, conveyorComponent.getTargetBlock());
        if(targetConveyor == null) {
            ejectItems(readyItems, conveyorComponent.getTargetBlock(), world);
        } else {
            ConveyorComponent targetConveyorComponent = buffer.getComponent(targetConveyor, ConveyorComponent.getComponentType());
            if(targetConveyorComponent == null) {
                ejectItems(readyItems, conveyorComponent.getTargetBlock(), world);
            } else {
                //export the items
                for(ConveyorInstance instance : readyItems) {
                    if(!targetConveyorComponent.canAddItem()) {
                        ArrayList<ConveyorInstance> temp = new ArrayList<>();
                        temp.add(instance);
                        ejectItems(temp, conveyorComponent.getTargetBlock(), world);
                        structureFailed = true;
                    } else {
                        ConveyorInstance newInstance = instance.clone();
                        newInstance.from = DirectionLibrary.getBackwardDirection(newInstance.to);
                        newInstance.to = targetConveyorComponent.getForwardDirection();

                        targetConveyorComponent.addItem(newInstance);
                        ArchTransportModule.get().getConveyorRefSystem().changeTickState(conveyorComponent.getTargetBlock(), buffer, true);
                    }
                }

                if(structureFailed) {
                    structureFailure(targetConveyorComponent, conveyorComponent.getTargetBlock(), world, chunk);
                }
            }
        }

        conveyorComponent.removeReadyItems();

        if(conveyorComponent.isEmpty()) {
            ArchLibrary.changeBlockState(ArchLibrary.getBlockEntity(world, pos), buffer, "default");
            return BlockTickStrategy.IGNORED;
        } else {
            return BlockTickStrategy.CONTINUE;
        }
    }

    private BlockTickStrategy handleImport(ConveyorComponent conveyorComponent, World world, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer, WorldChunk chunk) {
        boolean structureFailed = false;
        //Check machine block for item to import
        ConveyorImporter importer = conveyorComponent.getImportData();
        if(importer == null) {
            conveyorComponent.setImporterData(globalCoords, false);
            return BlockTickStrategy.CONTINUE;
        }

        //Get the specific component we need to extract
        OutputComponent outputComponent = importer.getOutputComponent(buffer, world);
        ItemContainerBlock containerState = importer.getContainerComponent(buffer, world);
        ProcessingBenchBlock benchState = importer.getProcessingComponent(buffer, world);

        //Do not pull an item if the importers size is already maxed out.
        if(conveyorComponent.canAddItem()) {
            ItemStack item = null;
            Direction direction = DirectionLibrary.getBackwardDirection(conveyorComponent.getForwardDirection());

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

            //If an item is found, add it to the queue
            //NOTE: Importer can never result in a situation where the queue is already full. We do not need to check earlier for sudden changes in size.
            if(item != null) {
                ConveyorInstance instance = new ConveyorInstance(item, direction);
                spawnConveyorItem(instance, world, importer.getMachinesPos());
                conveyorComponent.addItem(instance);
            }
        }

        //Handle output to target
        ArrayList<ConveyorInstance> readyItems = conveyorComponent.decrementTimers();
        conveyorComponent.updateEntityLocations(globalCoords, conveyorComponent.getTargetBlock(), world);

        if(readyItems.isEmpty()) return BlockTickStrategy.CONTINUE;

        //If the target is not a conveyor, eject the items!
        Ref<ChunkStore> targetConveyor = ArchLibrary.getBlockEntity(world, conveyorComponent.getTargetBlock());
        if(targetConveyor == null) {
            ejectItems(readyItems, conveyorComponent.getTargetBlock(), world);

        } else {
            ConveyorComponent targetConveyorComponent = buffer.getComponent(targetConveyor, ConveyorComponent.getComponentType());
            if(targetConveyorComponent == null) {
                ejectItems(readyItems, conveyorComponent.getTargetBlock(), world);
            } else {
                //export the items
                for(ConveyorInstance instance : readyItems) {
                    //This item was exported from a machine. Do not delete it!
                    if(instance.from == Direction.NOT_SET) {
                        ConveyorInstance newInstance = instance.clone();
                        newInstance.from = newInstance.to;
                        newInstance.to = conveyorComponent.getForwardDirection();

                        conveyorComponent.addItem(newInstance);
                    } else {
                        if(!targetConveyorComponent.canAddItem()) {
                            ArrayList<ConveyorInstance> temp = new ArrayList<>();
                            temp.add(instance);
                            ejectItems(temp, conveyorComponent.getTargetBlock(), world);
                            structureFailed = true;
                        } else {
                            ConveyorInstance newInstance = instance.clone();
                            newInstance.from = DirectionLibrary.getBackwardDirection(newInstance.to);
                            newInstance.to = targetConveyorComponent.getForwardDirection();

                            targetConveyorComponent.addItem(newInstance);
                            ArchTransportModule.get().getConveyorRefSystem().changeTickState(conveyorComponent.getTargetBlock(), buffer, true);
                        }
                    }
                }

                if(structureFailed) {
                    structureFailure(targetConveyorComponent, conveyorComponent.getTargetBlock(), world, chunk);
                }
            }

        }

        conveyorComponent.removeReadyItems();

        if(conveyorComponent.isEmpty()) {
            ArchLibrary.changeBlockState(ArchLibrary.getBlockEntity(world, globalCoords), buffer, "default");
        }

        return BlockTickStrategy.CONTINUE;
    }

    private boolean calculateTransaction(ItemContainer container, ConveyorInstance instance, ItemStack item) {
        for(short i = 0; i < container.getCapacity(); i++) {
            if(container.canAddItemStackToSlot(i, item, false, false)) {
                int maxStack = item.getItem().getMaxStack();
                int slotQuantity = container.getItemStack(i) != null ? container.getItemStack(i).getQuantity() : 0;
                int totalQuantity = slotQuantity + item.getQuantity();
                int remaining = totalQuantity - maxStack;

                container.addItemStackToSlot(i, item);
                if(remaining != 0) {
                    instance.item = instance.item.withQuantity(remaining);
                } else {
                    return true;
                }
            }
        }

        return false;
    }

    private BlockTickStrategy handleExport(ConveyorComponent conveyorComponent, World world, Vector3i globalCoords, CommandBuffer<ChunkStore> buffer) {
        //Handle output to target
        ArrayList<ConveyorInstance> readyItems = conveyorComponent.decrementTimers();
        conveyorComponent.updateEntityLocations(globalCoords, conveyorComponent.getTargetBlock(), world);

        if(readyItems.isEmpty()) return BlockTickStrategy.CONTINUE;

        //Check machine block for item to export
        ConveyorImporter importer = conveyorComponent.getImportData();
        if(importer == null) {
            conveyorComponent.setImporterData(globalCoords, true);
            return BlockTickStrategy.CONTINUE;
        }

        //Get the specific component we need to extract
        FuelComponent fuelComponent = importer.getFuelComponent(buffer, world);
        InputComponent inputComponent = importer.getInputComponent(buffer, world);
        ItemContainerBlock containerState = importer.getContainerComponent(buffer, world);
        ProcessingBenchBlock benchState = importer.getProcessingComponent(buffer, world);

        ArrayList<ConveyorInstance> failedToExport = new ArrayList<>();

        for(ConveyorInstance instance : readyItems) {
            ItemStack item = instance.getItem();
            boolean exported = false;

            if(fuelComponent != null) {
                if(item.getItem().getResourceTypes() != null) {
                    for(ItemResourceType type : item.getItem().getResourceTypes()) {
                        if(type.id.equals("Fuel")) {
                            exported = calculateTransaction(fuelComponent.getContainer(), instance, item);
                        }
                    }
                }

                if(exported) {
                    instance.deleteItemEntity(world);
                    continue;
                }
            }

            if(inputComponent != null) {
                exported = calculateTransaction(inputComponent.getContainer(), instance, item);
            }
            else if(containerState != null){
                exported = calculateTransaction(containerState.getItemContainer(), instance, item);
            }
            else if (benchState != null){
                boolean isFuel = false;

                if(item.getItem().getResourceTypes() != null) {
                    for(ItemResourceType type : item.getItem().getResourceTypes()) {
                        if(type.id.equals("Fuel")) {
                            exported = calculateTransaction(benchState.getFuelContainer(), instance, item);
                            isFuel = true;
                        }
                    }
                }

                if(!isFuel) exported = calculateTransaction(benchState.getInputContainer(), instance, item);
            }

            if(!exported) {
                failedToExport.add(instance);
            } else {
                instance.deleteItemEntity(world);
            }
        }

        ejectItems(failedToExport, globalCoords, world);
        conveyorComponent.removeReadyItems();

        if(conveyorComponent.isEmpty()) {
            ArchLibrary.changeBlockState(ArchLibrary.getBlockEntity(world, globalCoords), buffer, "default");
            return BlockTickStrategy.IGNORED;
        } else {
            return BlockTickStrategy.CONTINUE;
        }
    }

    private void ejectItems(ArrayList<ConveyorInstance> instances, Vector3i pos, World world) {
        ArrayList<ItemStack> items = new ArrayList<>();

        for(ConveyorInstance instance : instances) {
            items.add(instance.getItem());
            instance.deleteItemEntity(world);
        }

        ArchLibrary.spawnItems(world, pos, items);
    }

    private void structureFailure(ConveyorComponent conveyorComponent, Vector3i pos, World world, WorldChunk chunk) {
        ejectItems(conveyorComponent.getAllInstances(), pos, world);
        conveyorComponent.clearItems();

        world.execute(() -> {
            chunk.setBlock(pos.x, pos.y, pos.z, BlockType.EMPTY);
        });
    }

    private void spawnConveyorItem(ConveyorInstance instance, World world, Vector3i pos) {
        CompletableFuture<UUID> uuid = new CompletableFuture<>();

        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Vector3d centerPos = new Vector3d(pos.x + 0.5, pos.y + 0.3, pos.z + 0.5);

            Holder<EntityStore> holder = ItemComponent.generateItemDrop(store, instance.item, centerPos, new Vector3f(), 0, 0, 0);
            holder.ensureComponent(PreventItemMerging.getComponentType());

            holder.removeComponent(ItemComponent.getComponentType());
            ItemComponent itemComponent = new ItemComponent(instance.item);
            itemComponent.setPickupDelay(999);
            holder.addComponent(ItemComponent.getComponentType(), itemComponent);
            holder.removeComponent(Velocity.getComponentType());

            Ref<EntityStore> entity = store.addEntity(holder, AddReason.SPAWN);

            UUIDComponent uuidComponent = entity.getStore().getComponent(entity, UUIDComponent.getComponentType());

            uuid.complete(uuidComponent.getUuid());
            uuid.thenAccept(instance::setUUID);
        });
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }
}
