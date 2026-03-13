package com.CybaricFox.UI.Pages;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.FuelComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.OutputComponent;
import com.CybaricFox.Components.Processing.ProcessContext;
import com.CybaricFox.UI.Pages.Common.EnergyImageState;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.itemsound.config.ItemSoundSet;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
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
        Vector3i localCoords = ArchLibrary.convertToLocalCoords(pos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));

        BlockType type = chunk.getBlockType(localCoords);

        uiCommandBuilder.append("#InventoryRow", "Pages/Common/InventoryUI.ui");

        setSlots(ref, uiCommandBuilder);
        translateBlockName(type, uiCommandBuilder);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Dropped, "#ItemGrid", new EventData().append("Type", "Drop").append("Grid", "Inventory"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#ItemGrid", new EventData().append("Type", "Release").append("Grid", "Inventory"), false);
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

    private int getInventoryID(String name) {
        if(name.equals("Inventory")) {
            return inventoryID;
        } else if(name.equals("Fuel")) {
            return fuelID;
        } else if(name.equals("Input")) {
            return inputID;
        }

        return -1;
    }

    private void onDrop(Ref<EntityStore> ref, int firstSlot, int secondSlot, int quantity, int sourceID, int receiverID) {
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

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
            //If we move an item, check that the current recipe is not negated
            checkForRecipeCancel(input, firstSlot, quantity);

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

        if(from.getItemStack(realFromIndex) != null) {
            playSlotClick(ref, from, realFromIndex);
        }

        from.moveItemStackFromSlotToSlot(realFromIndex, quantity, to, realToIndex);
    }

    private void checkForRecipeCancel(InputComponent input, int slot, int quantity) {
        ProcessContext context = input.getProcess();

        if(context == null) return;

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
        super.handleDataEvent(ref, store, data);

        UICommandBuilder builder = new UICommandBuilder();

        if(data.type.equals("Release")) {
            onDrop(ref, data.dragFromSlot, data.toSlot, data.dragQuantity, data.dragFromSection, getInventoryID(data.grid));

            rebuild();
            return;
        }
        if(data.type.equals("Drop")) {
            onDrop(ref, data.fromSlot, data.toSlot, data.quantity, data.fromSection, getInventoryID(data.grid));

            setSlots(ref, builder);
            sendBuilder(builder);
            return;
        }
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
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

        EnergyComponent energy = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());

        refreshEnergy(energy, builder, true);
    }

    protected void addFuelUI(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder event) {
        builder.append("#ContentContainerGroup", "Pages/Common/FuelUI.ui");
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#FuelItemGrid", new EventData().append("Type", "Drop").append("Grid", "Fuel"), true);
        event.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#FuelItemGrid", new EventData().append("Type", "Release").append("Grid", "Fuel"), false);

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

        FuelComponent fuel = block.getStore().getComponent(block, ArchStar.get().getFuelComponentType());

        refreshFuelUI(fuel, builder);
        refreshProgressBar(fuel, builder);

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
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#InputItemGrid", new EventData().append("Type", "Drop").append("Grid", "Input"), true);
        event.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#InputItemGrid", new EventData().append("Type", "Release").append("Grid", "Input"), false);

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

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
    public void refreshProgressBar(FuelComponent fuelComponent, UICommandBuilder builder) {
        builder.set("#FuelProgress.Value", fuelComponent.getProgressAsPercentage());
    }

    public void sendBuilder(UICommandBuilder builder) {
        sendUpdate(builder, null, false);
    }

    protected void addOutputUI(Ref<EntityStore> ref, UICommandBuilder builder, UIEventBuilder event) {
        builder.append("#ContentContainerGroup", "Pages/Common/OutputUI.ui");
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#OutputItemGrid", new EventData().append("Type", "Drop").append("Grid", "Output"), true);
        event.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#OutputItemGrid", new EventData().append("Type", "Release").append("Grid", "Output"), false);

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

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

    public void refreshEnergy(EnergyComponent energyComponent, UICommandBuilder builder, boolean override) {
        int current = energyComponent.getCurrentEnergy();
        int max = energyComponent.getMaxEnergy();

        builder.set("#EnergyAmount.Text", current + " / " + max);

        int threshold = Math.ceilDiv(max, 5);

        if(current == 0) {
            setEnergyState(EnergyImageState.EMPTY, builder, override);
        } else if(current < threshold) {
            setEnergyState(EnergyImageState.DEPLETED, builder, override);
        } else if (current < threshold * 2) {
            setEnergyState(EnergyImageState.LOW, builder, override);
        } else if (current < threshold * 3) {
            setEnergyState(EnergyImageState.UNDER_HALF, builder, override);
        } else if (current < threshold * 4) {
            setEnergyState(EnergyImageState.ABOVE_HALF, builder, override);
        } else if (current < max) {
            setEnergyState(EnergyImageState.HIGH, builder, override);
        } else if (current == max) {
            setEnergyState(EnergyImageState.FULL, builder, override);
        }
    }

    private void setEnergyState(EnergyImageState state, UICommandBuilder builder, boolean override) {
        if(!override && energyState == state) {
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

    private void playSlotClick(Ref<EntityStore> ref, ItemContainer container, short slot) {
        World world = ref.getStore().getExternalData().getWorld();

        ItemStack stack = container.getItemStack(slot);
        if(stack == null) return;
        Item item = stack.getItem();

        int setIndex = item.getItemSoundSetIndex();
        ItemSoundSet iss = ItemSoundSet.getAssetMap().getAsset(setIndex);

        world.execute(() -> {
            String dropSound = "NULL";

            if(iss == null) {
                return;
            } else {
                boolean hasSound = false;
                for(String value : iss.getSoundEventIds().values()) {
                    if(value.contains("Drop")) {
                        dropSound = value;
                        hasSound = true;
                        break;
                    }
                }

                if(!hasSound || dropSound.equals("NULL")) {
                    return;
                }
            }

            SoundUtil.playSoundEvent3dToPlayer(ref, SoundEvent.getAssetMap().getIndex(dropSound), SoundCategory.UI, pos.toVector3d(), ref.getStore());
        });
    }

    public static class CommonData {
        private int toSlot;

        //Custom
        private String type;
        private String grid;

        private int fromSlot;
        private int quantity;
        private int fromSection;

        private int dragFromSlot;
        private int dragQuantity;
        private int dragFromSection;

        public static final BuilderCodec<CommonData> CODEC =
                BuilderCodec.builder(CommonData.class, CommonData::new)
                        .append(new KeyedCodec<String>("Type", Codec.STRING), (entry, s) -> entry.type = s, (entry) -> entry.type).add()
                        .append(new KeyedCodec<String>("Grid", Codec.STRING), (entry, s) -> entry.grid = s, (entry) -> entry.grid).add()
                        .append(new KeyedCodec<Integer>("SlotIndex", Codec.INTEGER), (entry, s) -> entry.toSlot = s, (entry) -> entry.toSlot).add()
                        .append(new KeyedCodec<Integer>("SourceItemGridIndex", Codec.INTEGER), (entry, s) -> entry.fromSlot = s, (entry) -> entry.fromSlot).add()
                        .append(new KeyedCodec<Integer>("ItemStackQuantity", Codec.INTEGER), (entry, s) -> entry.quantity = s, (entry) -> entry.quantity).add()
                        .append(new KeyedCodec<Integer>("SourceInventorySectionId", Codec.INTEGER), (entry, s) -> entry.fromSection = s, (entry) -> entry.fromSection).add()
                        .append(new KeyedCodec<Integer>("DragSourceItemGridIndex", Codec.INTEGER), (entry, s) -> entry.dragFromSlot = s, (entry) -> entry.dragFromSlot).add()
                        .append(new KeyedCodec<Integer>("DragItemStackQuantity", Codec.INTEGER), (entry, s) -> entry.dragQuantity = s, (entry) -> entry.dragQuantity).add()
                        .append(new KeyedCodec<Integer>("DragSourceInventorySectionId", Codec.INTEGER), (entry, s) -> entry.dragFromSection = s, (entry) -> entry.dragFromSection).add()
                        .build();
    }
}
