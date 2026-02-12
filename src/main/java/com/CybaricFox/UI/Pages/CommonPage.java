package com.CybaricFox.UI.Pages;

import com.CybaricFox.API.FoxLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Blocks.EnergyComponent;
import com.CybaricFox.Components.Blocks.FuelComponent;
import com.CybaricFox.Components.Blocks.InputComponent;
import com.CybaricFox.Components.Blocks.OutputComponent;
import com.CybaricFox.Components.Helpers.ProcessContext;
import com.CybaricFox.UI.Pages.Common.EnergyImageState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/*
ITEM GRID SECTION ID REFERENCE
1 = Inventory
2 = Fuel
3 = Input
4 = Output
5 = Storage
 */

public class CommonPage extends InteractiveCustomUIPage<CommonPage.CommonData> {

    public ItemGridSlot[] slots; //inventory slots
    public ItemGridSlot[] fuelSlots;
    public ItemGridSlot[] inputSlots;
    public ItemGridSlot[] outputSlots;

    //Position of the block this interacted with
    protected Vector3i pos = null;

    protected boolean isDismissed = false;

    private final int inventoryID = 1;
    private final int fuelID = 2;
    private final int inputID = 3;
    private final int outputID = 4;
    private final int storageID = 5;

    protected EnergyImageState energyState = EnergyImageState.EMPTY;

    public CommonPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonPage.CommonData> eventDataCodec) {
        super(playerRef, CustomPageLifetime.CanDismiss, eventDataCodec);

    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        uiCommandBuilder.append("Pages/CommonPage.ui");

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        World world = player.getWorld();

        //Convert global coords to local coords
        Vector3i localCoords = FoxLibrary.convertToLocalCoords(pos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));

        BlockType type = chunk.getBlockType(localCoords);

        setSlots(ref, uiCommandBuilder);
        translateBlockName(type, uiCommandBuilder);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, "#ItemGrid", new EventData().append("Type", "Drop").append("Grid", "Inventory"), true);
    }

    public Vector3i getPos() {
        return pos;
    }

    private void setSlots(Ref<EntityStore> ref, UICommandBuilder builder) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());

        int inventoryCapacity = player.getInventory().getStorage().getCapacity();
        int hotbarCapacity = player.getInventory().getHotbar().getCapacity();

        int fullCapacity = inventoryCapacity + hotbarCapacity;

        slots = new ItemGridSlot[fullCapacity];

        for(short i = 0; i < fullCapacity; i++) {
            slots[i] = new ItemGridSlot();
            slots[i].setActivatable(true);

            if(i < inventoryCapacity) {
                slots[i].setItemStack(player.getInventory().getStorage().getItemStack(i));
            } else {
                slots[i].setItemStack(player.getInventory().getHotbar().getItemStack((short) (i - inventoryCapacity)));
            }
        }

        builder.set("#ItemGrid.Slots", slots);
    }

    private void onDrop(Ref<EntityStore> ref, int firstSlot, int secondSlot, int quantity, int sourceID, int receiverID) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), pos);

        int inventoryCapacity = player.getInventory().getStorage().getCapacity();
        int hotbarCapacity = player.getInventory().getHotbar().getCapacity();

        int fullCapacity = inventoryCapacity + hotbarCapacity;

        ItemContainer from = null;
        ItemContainer to = null;

        short realFromIndex = (short) firstSlot;
        short realToIndex = (short) secondSlot;

        if(sourceID == inventoryID) {
            if(firstSlot < inventoryCapacity ) {
                from = player.getInventory().getStorage();
            } else {
                from = player.getInventory().getHotbar();
                realFromIndex = (short)(firstSlot - inventoryCapacity);
            }
        }
        if(sourceID == fuelID) {
            FuelComponent fuel = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());
            fuel.isUIUpdated = false;
            from = fuel.getContainer();
        }
        if(sourceID == inputID) {
            InputComponent input = block.getStore().getComponent(block, ArchStar.get().getInputComponentType());
            input.isUIUpdated = false;
            //If we move an item out of input, check that the current recipe is not negated
            if(receiverID != inputID) {
                checkForRecipeCancel(input, firstSlot, quantity);
            }

            from = input.getContainer();
        }
        if(sourceID == outputID) {
            OutputComponent output = block.getStore().getComponent(block, ArchStar.get().getOutputComponentType());
            output.isUIUpdated = false;
            from = output.getContainer();
        }

        if(receiverID == inventoryID) {
            if(secondSlot < inventoryCapacity) {
                to = player.getInventory().getStorage();
            } else {
                to = player.getInventory().getHotbar();
                realToIndex = (short) (secondSlot - inventoryCapacity);
            }
        }
        if(receiverID == fuelID) {
            FuelComponent fuel = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());
            fuel.isUIUpdated = false;
            to = fuel.getContainer();
        }
        if(receiverID == inputID) {
            InputComponent input = block.getStore().getComponent(block, ArchStar.get().getInputComponentType());
            input.isUIUpdated = false;
            to = input.getContainer();
        }
        //OUTPUT CANNOT BE INSERTED INTO!!!

        if(from == null || to == null) return;

        from.moveItemStackFromSlotToSlot(realFromIndex, quantity, to, realToIndex);
    }

    private void checkForRecipeCancel(InputComponent input, int slot, int quantity) {
        ProcessContext context = input.getProcess();
        ItemStack item = input.getItemStack((short) slot);

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
                    for(int j = 0; j < input.getContainer().getCapacity(); j++) {
                        if(j == slot) continue; //We already checked this

                        item = input.getItemStack((short) j);

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
                    input.clearTargets();
                }
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, CommonData data) {
        if(data.type.equals("Drop")) {
            int id = 0;
            if(data.grid.equals("Inventory")) {
                id = inventoryID;
            } else if(data.grid.equals("Fuel")) {
                id = fuelID;
            } else if(data.grid.equals("Input")) {
                id = inputID;
            }

            if(id != 0) {
                onDrop(ref, data.fromSlot, data.toSlot, data.quantity, data.fromSection, id);
            }
        }

        UICommandBuilder builder = new UICommandBuilder();

        setSlots(ref, builder);

        sendUpdate(builder, null, false);
    }

    protected void translateBlockName(BlockType type, UICommandBuilder builder) {
        String contentName = "CONTENT";

        if(type != null) {
            contentName = Message.translation(type.getItem().getTranslationKey()).getAnsiMessage().toUpperCase();
        }

        builder.set("#TitleText2.Text", contentName);
    }

    protected void addEnergyUI(Ref<EntityStore> ref, UICommandBuilder builder) {
        builder.append("#ContentContainerGroup", "Pages/Common/EnergyUI.ui");

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), pos);

        EnergyComponent energy = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());

        refreshEnergy(energy, builder);
    }

    protected void addFuelUI(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder event) {
        builder.append("#ContentContainerGroup", "Pages/Common/FuelUI.ui");
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#FuelItemGrid", new EventData().append("Type", "Drop").append("Grid", "Fuel"));

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), pos);

        FuelComponent fuel = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());

        refreshFuelUI(fuel, builder);

        if(fuel.getCapacity() > 1) {
            builder.set("#FuelItemGrid.SlotsPerRow", 2);
        }
    }

    public void refreshFuelUI(FuelComponent fuelComponent, UICommandBuilder builder) {
        fuelSlots = new ItemGridSlot[fuelComponent.getCapacity()];

        for(short i = 0; i < fuelComponent.getCapacity(); i++) {
            fuelSlots[i] = new ItemGridSlot();
            fuelSlots[i].setActivatable(true);
            fuelSlots[i].setItemStack(fuelComponent.getItemStack(i));
        }

        if(builder != null) {
            builder.set("#FuelItemGrid.Slots", fuelSlots);
        } else {
            builder = new UICommandBuilder();
            builder.set("#FuelItemGrid.Slots", fuelSlots);
            sendUpdate(builder, null, false);
        }
    }

    protected void addInputUI(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder event) {
        builder.append("#ContentContainerGroup", "Pages/Common/InputUI.ui");
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#InputItemGrid", new EventData().append("Type", "Drop").append("Grid", "Input"));

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), pos);

        InputComponent input = block.getStore().getComponent(block, ArchStar.get().getInputComponentType());

        refreshInputUI(input, builder);
        refreshProgressBar(input, builder);

        if(input.getCapacity() > 1) {
            builder.set("#InputItemGrid.SlotsPerRow", 2);
        }
    }

    public void refreshInputUI(InputComponent inputComponent, UICommandBuilder builder) {
        inputSlots = new ItemGridSlot[inputComponent.getCapacity()];

        for(short i = 0; i < inputComponent.getCapacity(); i++) {
            inputSlots[i] = new ItemGridSlot();
            inputSlots[i].setActivatable(true);
            inputSlots[i].setItemStack(inputComponent.getItemStack(i));
        }

        if(builder != null) {
            builder.set("#InputItemGrid.Slots", inputSlots);
        } else {
            builder = new UICommandBuilder();
            builder.set("#InputItemGrid.Slots", inputSlots);
            sendUpdate(builder, null, false);
        }
    }

    public void refreshProgressBar(InputComponent inputComponent, UICommandBuilder builder) {
        builder.set("#InputProgress.Value", inputComponent.getProgressAsPercentage());
    }

    public void sendBuilder(UICommandBuilder builder) {
        sendUpdate(builder, null, false);
    }

    protected void addOutputUI(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder event) {
        builder.append("#ContentContainerGroup", "Pages/Common/OutputUI.ui");
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#OutputItemGrid", new EventData().append("Type", "Drop").append("Grid", "Output"));

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = FoxLibrary.getBlockEntity(player.getWorld(), pos);

        OutputComponent output = block.getStore().getComponent(block, ArchStar.get().getOutputComponentType());

        refreshOutputUI(output, builder);

        if(output.getCapacity() > 1) {
            builder.set("#OutputItemGrid.SlotsPerRow", 2);
        }
    }

    public void refreshOutputUI(OutputComponent outputComponent, UICommandBuilder builder) {
        outputSlots = new ItemGridSlot[outputComponent.getCapacity()];

        for(short i = 0; i < outputComponent.getCapacity(); i++) {
            outputSlots[i] = new ItemGridSlot();
            outputSlots[i].setActivatable(true);
            outputSlots[i].setItemStack(outputComponent.getItemStack(i));
        }

        if(builder != null) {
            builder.set("#OutputItemGrid.Slots", outputSlots);
        } else {
            builder = new UICommandBuilder();
            builder.set("#OutputItemGrid.Slots", outputSlots);
            sendUpdate(builder, null, false);
        }
    }

    private void setEnergyImage(UICommandBuilder builder, String path) {
        if(path != null && !path.isEmpty()) {
            builder.set("#EnergyImage.AssetPath", path);
        } else {
            builder.setNull("#EnergyImage.AssetPath");
            ArchStar.LOGGER.at(Level.WARNING).log("Energy Image is NULL!");
        }
    }

    public void refreshEnergy(EnergyComponent energyComponent, UICommandBuilder builder) {
        int current = energyComponent.getCurrentEnergy();
        int max = energyComponent.getMaxEnergy();

        builder.set("#EnergyAmount.Text", current + " / " + max);

        int threshold = Math.ceilDiv(max, 5);

        if(current == 0) {
            setEnergyState(EnergyImageState.EMPTY, builder);
        } else if(current < threshold) {
            setEnergyState(EnergyImageState.DEPLETED, builder);
        } else if (current < threshold * 2) {
            setEnergyState(EnergyImageState.LOW, builder);
        } else if (current < threshold * 3) {
            setEnergyState(EnergyImageState.UNDER_HALF, builder);
        } else if (current < threshold * 4) {
            setEnergyState(EnergyImageState.ABOVE_HALF, builder);
        } else if (current < max) {
            setEnergyState(EnergyImageState.HIGH, builder);
        } else if (current == max) {
            setEnergyState(EnergyImageState.FULL, builder);
        }
    }

    private void setEnergyState(EnergyImageState state, UICommandBuilder builder) {
        if(energyState == state) {
            return;
        }

        energyState = state;

        switch (energyState) {
            case EMPTY -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Empty.png");
            case DEPLETED -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Dying.png");
            case LOW -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Low.png");
            case UNDER_HALF -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Average.png");
            case ABOVE_HALF -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Medium.png");
            case HIGH -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_High.png");
            case FULL -> setEnergyImage(builder, "UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Full.png");
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);

        isDismissed = true;
    }

    public static class CommonData {
        private int toSlot;
        private int fromSlot;
        private int quantity;
        private String type;
        private String grid;
        private int fromSection;

        public static final BuilderCodec<CommonData> CODEC =
                BuilderCodec.builder(CommonData.class, CommonData::new)
                        .append(new KeyedCodec<String>("Type", Codec.STRING), (entry, s) -> entry.type = s, (entry) -> entry.type).add()
                        .append(new KeyedCodec<String>("Grid", Codec.STRING), (entry, s) -> entry.grid = s, (entry) -> entry.grid).add()
                        .append(new KeyedCodec<Integer>("SlotIndex", Codec.INTEGER), (entry, s) -> entry.toSlot = s, (entry) -> entry.toSlot).add()
                        .append(new KeyedCodec<Integer>("SourceItemGridIndex", Codec.INTEGER), (entry, s) -> entry.fromSlot = s, (entry) -> entry.fromSlot).add()
                        .append(new KeyedCodec<Integer>("ItemStackQuantity", Codec.INTEGER), (entry, s) -> entry.quantity = s, (entry) -> entry.quantity).add()
                        .append(new KeyedCodec<Integer>("SourceInventorySectionId", Codec.INTEGER), (entry, s) -> entry.fromSection = s, (entry) -> entry.fromSection).add()
                        .build();
    }
}
