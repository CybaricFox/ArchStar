package com.CybaricFox.Modules.ArchMachines.Components;

import com.CybaricFox.Modules.ArchMachines.ArchMachinesModule;
import com.CybaricFox.Modules.ArchMachines.ProcessContext;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.Common.IMachineUIComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;
import java.util.ArrayList;

public class InputComponent extends CommonContainerComponent implements IMachineUIComponent {
    public static final BuilderCodec<InputComponent> CODEC;

    //Id is used for crafting recipes
    //For instance, "Furnace" allows this machine to process furnace recipes in processingBench recipes.
    private String id = "NULL";
    //Multiplier that increases or decreases the time taken
    private float progressMultiplier = 1.0f;

    //The current process this machine is processing
    private ProcessContext process = null;
    //When this block is loaded, what was the progress of the last recipe at when it unloaded?
    private int lastProgress = 0;

    //if true, every input slot is important to a recipe as recipes are expected to require multiple inputs
    public boolean isMultiInput = false; //This will be setup at a later date

    public InputComponent() {

    }
    public InputComponent(int actualSize, int maxSize, String id, float progressMultiplier, SimpleItemContainer container) {
        super(actualSize, maxSize, container);

        this.id = id;
        this.progressMultiplier = progressMultiplier;
    }

    //Returns the threshold multiplied by the multiplier
    private int calculateProgressThreshold(float threshold) {
        return Math.round((threshold * 30) * progressMultiplier);
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new InputComponent(actualSize, maxSize, id, progressMultiplier, container);
    }

    //Sets the process
    public void setTargets(ArrayList<String> inputIds, ArrayList<Integer> inputQuantities, ArrayList<String> outputIds, ArrayList<Integer> outputQuantities, float recipeTime) {
        process = new ProcessContext(inputIds, inputQuantities, outputIds, outputQuantities, calculateProgressThreshold(recipeTime));

        if(lastProgress != 0) {
            process.progress = lastProgress;
            lastProgress = 0;
        }
    }

    //Clears the process
    public void clearTargets() {
        process = null;
    }

    //Returns true if this machine is processing
    public boolean isProcessing() {
        return process != null;
    }

    //Increases progress and returns the process for further calculations
    public ProcessContext processInput() {
        if(process.progress < process.progressThreshold) {
            process.progress++;
        }

        return process;
    }

    public ProcessContext getProcess() {
        return process;
    }

    public float getProgressAsPercentage() {
        if(process == null) return 0;

        int progress = process.progress;
        int threshold = process.progressThreshold;

        return (float) progress / threshold;
    }

    //Returns the recipe id this machine uses
    public String getID() {return id;}

    public static ComponentType<ChunkStore, InputComponent> getComponentType() {
        return ArchMachinesModule.get().getInputComponentType();
    }

    static {
        CODEC = (BuilderCodec.builder(InputComponent.class, InputComponent::new))
                //Common fields
                .append(new KeyedCodec<Integer>("Size", Codec.INTEGER), (component, s) -> component.actualSize = s, (component) -> component.actualSize).add()
                .append(new KeyedCodec<Integer>("MaxSize", Codec.INTEGER), (component, s) -> component.maxSize = s, (component) -> component.maxSize).add()
                .append(new KeyedCodec<SimpleItemContainer>("Container", SimpleItemContainer.CODEC), (component, s) -> component.container = s, (component) -> component.container).add()

                //Required fields
                .append(new KeyedCodec<Float>("ProgressMultiplier", Codec.FLOAT), (component, s) -> component.progressMultiplier = s, (component) -> component.progressMultiplier).add()
                .append(new KeyedCodec<String>("ID", Codec.STRING), (component, s) -> component.id = s, (component) -> component.id).add()

                //Save fields
                .append(new KeyedCodec<Integer>("Progress", Codec.INTEGER), (component, s) -> component.lastProgress = s, (component) -> component.lastProgress).add()
                .build();
    }

    @Override
    public float getProgress() {
        return getProgressAsPercentage();
    }

    private static int sectionID = 0;
    @Override
    public int getSectionID() {
        if(sectionID == 0) sectionID = setSectionID();
        return sectionID;
    }

    @Override
    public boolean canInsert() {
        return true;
    }

    private void checkForRecipeCancel(int slot, int quantity) {
        ProcessContext context = getProcess();

        if(context == null) return;

        ItemStack item = getItemStack((short) slot);
        if(item == null) return;

        //Check every input in the recipe to see if the item moving out is part of the recipe
        for(int i = 0; i < context.targetInputIds.size(); i++) {
            String targetID = context.targetInputIds.get(i);
            //If the item is part of the recipe
            if(item.getItemId().equals(targetID)) {
                //Required quantity of this item for the recipe
                int requiredQuantity = context.targetInputQuantities.get(i);
                int currentQuantity = item.getQuantity();

                //Cancel the recipe if there isnt eneough of the item left over
                if(currentQuantity - quantity < requiredQuantity) {
                    //WAIT! Check if another slot has the item too
                    for(int j = 0; j < getContainer().getCapacity(); j++) {
                        if(j == slot) continue; //We already checked this

                        item = getItemStack((short) j);

                        if(item == null) continue;

                        //If another slot contains the item and has enough quantity, the recipe can go through.
                        if(item.getItemId().equals(targetID)) {
                            currentQuantity = item.getQuantity();
                            if(currentQuantity >= requiredQuantity) {
                                //The recipe can still be processed!
                                return;
                            }
                        }
                    }

                    //The recipe is cancelled
                    clearTargets();
                }
            }
        }
    }

    @Override
    public void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity) {
        checkForRecipeCancel(senderSlot, quantity);
    }
}
