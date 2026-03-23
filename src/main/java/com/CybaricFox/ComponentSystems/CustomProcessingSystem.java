package com.CybaricFox.ComponentSystems;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.API.TickContext;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.CybaricFox.Components.Processing.ProcessContext;
import com.hypixel.hytale.builtin.crafting.CraftingPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/*
    A custom processing system.
    This system allows the expansion of work benches by effectively removing the static enum the bench class uses.
    All entities that use this system read from processingBench recipes. So just make recipes as you would for processing benches.
 */
public class CustomProcessingSystem extends EntityTickingSystem<ChunkStore> {
    @Override
    public void tick(float v, int index, @Nonnull ArchetypeChunk<ChunkStore> archetypeChunk, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        TickContext context = new TickContext(index, archetypeChunk, commandBuffer);
        if(!context.isValid) return;

        //iterate over all tickable blocks
        context.blockSection.forEachTicking(context.blockComponentChunk, commandBuffer, context.chunkSection.getY(), (blockComponentChunk1, commandBuffer1, localX, localY, localZ, blockId) ->
        {
            Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(blockComponentChunk1, new Vector3i(localX, localY, localZ));
            if (blockRef == null) {
                return BlockTickStrategy.IGNORED;
            }

            InputComponent inputComponent = commandBuffer1.getComponent(blockRef, ArchStar.get().getInputComponentType());
            OutputComponent outputComponent = commandBuffer1.getComponent(blockRef, ArchStar.get().getOutputComponentType());
            EnergyComponent energyComponent = commandBuffer1.getComponent(blockRef, ArchStar.get().getEnergyComponentType());

            if(inputComponent == null) {
                return BlockTickStrategy.IGNORED;
            }

            //First, check that the block is not already processing
            if(inputComponent.isProcessing()) {

                //Blocks that have an energy component will only process if they have energy
                if(energyComponent != null) {
                    boolean result = energyComponent.consumeEnergy();

                    //No energy for processing!
                    if(!result) {
                        ArchLibrary.changeBlockState(blockRef, commandBuffer1, "default");
                        return BlockTickStrategy.CONTINUE;
                    } else {
                        ArchLibrary.changeBlockState(blockRef, commandBuffer1, "Processing");
                    }
                } else {
                    ArchLibrary.changeBlockState(blockRef, commandBuffer1, "Processing");
                }

                //Get the current input process
                ProcessContext process = inputComponent.processInput();

                //If progress has reach the threshold, convert the input to its output
                if(process.progress == process.progressThreshold) {
                    //Consume each input item as necessary
                    for(short i = 0; i < process.targetInputIds.size(); i++) {
                        short slot = inputComponent.getFirstSlotWithItem(process.targetInputIds.get(i), process.targetInputQuantities.get(i));

                        if(slot == -1) {
                            ArchStar.LOGGER.at(Level.SEVERE).log("Attempted to finish processing " + process.targetInputIds.get(i) + " but the item could not be found in input!");
                            clearTargets(inputComponent, null);
                            return BlockTickStrategy.CONTINUE;
                        }

                        inputComponent.getContainer().removeItemStackFromSlot(slot, process.targetInputQuantities.get(i));
                    }

                    //if no output, do not continue.
                    if(outputComponent == null) {
                        ArchLibrary.changeBlockState(blockRef, commandBuffer1, "ProcessComplete");
                        return clearTargets(inputComponent, null);
                    }

                    //For every output item, add it to the output.
                    for(short i = 0; i < process.targetOutputIds.size(); i++) {
                        ItemStack item = new ItemStack(process.targetOutputIds.get(i), process.targetOutputQuantities.get(i));

                        outputComponent.getContainer().addItemStack(item);
                    }

                    ArchLibrary.changeBlockState(blockRef, commandBuffer1, "ProcessComplete");
                    return clearTargets(inputComponent, outputComponent);
                } else {
                    return BlockTickStrategy.CONTINUE;
                }
            } else {
                if(!inputComponent.isMultiInput) {
                    //Check each input slot for a potential recipe
                    for(short i = 0; i < inputComponent.getCapacity(); i++) {
                        ItemStack item = inputComponent.getItemStack(i);

                        //This slot is empty
                        if(item == null) continue;

                        //Get all recipes that match this machines id
                        List<CraftingRecipe> recipes = CraftingPlugin.getBenchRecipes(BenchType.Processing, inputComponent.getID());

                        //Check if this item has a recipe
                        for(CraftingRecipe recipe : recipes) {
                            //Since isMultiInput is false, only recipes with 1 input are counted.
                            if(recipe.getInput().length != 1) continue;

                            //Get the input item
                            MaterialQuantity material = recipe.getInput()[0];

                            //Do not continue if material is somehow null
                            if(material == null) return BlockTickStrategy.IGNORED;

                            //If the input is null, check the next recipe
                            if(material.getItemId() == null) continue;

                            //If the input matches the recipe input and has the proper quantity, process this recipe
                            if(material.getItemId().equals(item.getItemId()) && item.getQuantity() >= material.getQuantity()) {
                                //Create the process context
                                ArrayList<String> inputIds = new ArrayList<>();
                                ArrayList<Integer> inputQuantities = new ArrayList<>();
                                ArrayList<String> outputIds = new ArrayList<>();
                                ArrayList<Integer> outputQuantities = new ArrayList<>();

                                inputIds.add(material.getItemId());
                                inputQuantities.addFirst(material.getQuantity());

                                for(MaterialQuantity output : recipe.getOutputs()) {
                                    outputIds.add(output.getItemId());
                                    outputQuantities.add(output.getQuantity());
                                }

                                //If there is an output component, check that output can actually output the materials
                                if(outputComponent != null) {
                                    ArrayList<ItemStack> items = new ArrayList<>();

                                    for(int j = 0; i < outputIds.size(); i++) {
                                        items.add(new ItemStack(outputIds.get(j), outputQuantities.get(j)));
                                    }

                                    boolean result = outputComponent.getContainer().canAddItemStacks(items);

                                    if(!result) return BlockTickStrategy.CONTINUE;
                                }

                                inputComponent.setTargets(inputIds, inputQuantities, outputIds, outputQuantities, recipe.getTimeSeconds());

                                return BlockTickStrategy.CONTINUE;
                            }
                        }
                    }
                } else {

                }
            }

            ArchLibrary.changeBlockState(blockRef, commandBuffer1, "default");
            return BlockTickStrategy.CONTINUE;
        });
    }

    //Clears the process and sets the ui for update
    private BlockTickStrategy clearTargets(InputComponent input, OutputComponent output) {
        input.clearTargets();
        input.isUIUpdated = false;

        if(output != null) {
            output.isUIUpdated = false;
        }

        return BlockTickStrategy.CONTINUE;
    }

    @Nullable
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.or(BlockSection.getComponentType(), ChunkSection.getComponentType());
    }
}
