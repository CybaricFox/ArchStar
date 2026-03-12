package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.ComponentSystems.Helpers.EnergyNetwork;
import com.CybaricFox.Components.Helpers.EnergyBlockType;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.CybaricFox.Components.Blocks.FuelComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.logging.Level;

/*
    Tick system for energy blocks.
 */
public class EnergySystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float v, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());
        if(blocks == null) return;

        if (blocks.getTickingBlocksCountCopy() != 0) {
            ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());
            if(section == null) return;

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
            if(blockComponentChunk == null) return;

            blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
            {
                Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(blockComponentChunk1, new Vector3i(localX, localY, localZ));

                if (blockRef == null) {
                    return BlockTickStrategy.IGNORED;
                } else {
                    EnergyComponent energyBlock = commandBuffer1.getComponent(blockRef, ArchStar.get().getEnergyComponentType());
                    FuelComponent fuelComponent = commandBuffer1.getComponent(blockRef, ArchStar.get().getFuelComponentType());

                    //This is an energy block
                    if (energyBlock != null) {
                        //Run different functions depending on the type of energy block
                        if(energyBlock.getType() == EnergyBlockType.PRODUCER) {
                            if(runProducer(energyBlock, fuelComponent)) {
                                ArchLibrary.changeBlockState(blockRef, commandBuffer1, "Processing");
                            } else {
                                ArchLibrary.changeBlockState(blockRef, commandBuffer1, "default");
                            }
                        } else if(energyBlock.getType() == EnergyBlockType.CONSUMER) {
                            runConsumer(energyBlock);
                        }

                        return BlockTickStrategy.CONTINUE;
                    } else {
                        return BlockTickStrategy.IGNORED;
                    }
                }
            });
        }
    }

    private boolean runProducer(EnergyComponent energyComponent, FuelComponent fuelComponent) {
        if(fuelComponent != null) {
            //Is the block maxed on energy?
            if(energyComponent.getCurrentEnergy() != energyComponent.getMaxEnergy()) {
                //Is the block currently cooking?
                if(fuelComponent.isCooking) {
                    energyComponent.produceEnergy();
                    fuelComponent.decrementCookTime();
                    return true;
                } else { //Consume fuel to start cooking
                    return  fuelComponent.consumeFuel();
                }
            } else {
                fuelComponent.decrementCookTime();
            }

            return fuelComponent.isCooking;
        } else {
            //If there is no fuel component, just generate energy.
            energyComponent.produceEnergy();
            return true;
        }
    }

    private void runConsumer(EnergyComponent energyComponent) {
        //Do not run if there is no buffer to fill
        if(energyComponent.getCurrentEnergy() != energyComponent.getMaxEnergy()) {
            EnergyNetwork targetNetwork = ArchStar.get().getEnergyNetworkSystem().getNetwork(energyComponent.getNetworkID());

            if(targetNetwork != null) {
                //Map of producers in the network
                HashMap<Vector3i, EnergyComponent> producers = targetNetwork.getAllOfType(EnergyBlockType.PRODUCER);

                //Only run if there are producers
                if(producers != null) {
                    for(HashMap.Entry<Vector3i, EnergyComponent> entry : producers.entrySet()) {
                        //Returns false if producer is unable to output energy
                        int output = entry.getValue().transferEnergy(energyComponent.getInputRate(), energyComponent.getMaxEnergy() - energyComponent.getCurrentEnergy());

                        if(output <= 0) continue;

                        energyComponent.addEnergy(output);
                        break;
                    }
                }
            } else {
                ArchStar.LOGGER.at(Level.SEVERE).log("Energy Network " + energyComponent.getNetworkID() + " could not be found.");
            }
        }
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }
}


