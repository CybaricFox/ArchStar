package com.CybaricFox.UI.Pages;

import com.CybaricFox.API.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Components.Energy.EnergyComponent;
import com.CybaricFox.Components.Processing.InputComponent;
import com.CybaricFox.Components.Processing.ProcessContext;
import com.CybaricFox.UI.Pages.Common.EnergyImageState;
import com.CybaricFox.UI.Pages.Common.IMachineUIComponent;
import com.CybaricFox.UI.Pages.Common.UIComponentContext;
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
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
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

    //Position of the block this interacted with
    protected Vector3i pos;

    protected HashMap<String, UIComponentContext> componentMapping = new HashMap<>();

    protected boolean isDismissed = false;

    protected EnergyImageState energyState = EnergyImageState.EMPTY;

    protected UICommandBuilder globalBuilder;
    public boolean isValid = false;

    public CommonPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonPage.CommonData> eventDataCodec, Vector3i pos) {
        super(playerRef, CustomPageLifetime.CanDismiss, eventDataCodec);

        this.pos = pos;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        globalBuilder = uiCommandBuilder;
        uiCommandBuilder.append("Pages/CommonPage.ui");

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        World world = player.getWorld();

        //Convert global coords to local coords
        Vector3i localCoords = ArchLibrary.convertToLocalCoords(pos);

        //Get the worldChunk by indexing the target chunk from the blocks location
        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));

        BlockType type = chunk.getBlockType(localCoords);

        uiCommandBuilder.append("#InventoryRow", "Pages/Common/InventoryUI.ui");

        CombinedItemContainer container = new CombinedItemContainer(player.getInventory().getStorage(), player.getInventory().getHotbar());
        componentMapping.put("Inventory", new UIComponentContext("Inventory", container, true, 1));

        setItemGridSlots("Inventory", container);
        enableItemGridEventBindings(uiEventBuilder, "Inventory");
        translateBlockName(type);
    }

    public UICommandBuilder beginBuildingCycle() {
        sendBuilder(globalBuilder);
        globalBuilder = new UICommandBuilder();
        return globalBuilder;
    }

    public Vector3i getPos() {
        return pos;
    }

    private int getInventoryID(String name) {
        for(UIComponentContext context : componentMapping.values()) {
            if(context.name.equals(name)) {
                return context.sectionID;
            }
        }

        return -1;
    }

    private String getNameFromID(int id) {
        for(UIComponentContext context : componentMapping.values()) {
            if(context.sectionID == id) {
                return context.name;
            }
        }

        return null;
    }

    private void onDrop(String sender, String receiver, short senderSlot, short receiverSlot, int quantity) {
        componentMapping.get(sender).onDrop(sender, receiver, senderSlot, receiverSlot, quantity);

        ItemContainer senderContainer = componentMapping.get(sender).container;
        ItemContainer receiverContainer = componentMapping.get(receiver).container;

        if(!componentMapping.get(receiver).canInsert) return;

        playSlotClick(senderContainer, senderSlot);
        senderContainer.moveItemStackFromSlotToSlot(senderSlot, quantity, receiverContainer, receiverSlot);
    }



    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, CommonData data) {
        super.handleDataEvent(ref, store, data);

        if(data.type.equals("Release")) {
            onDrop(getNameFromID(data.dragFromSection), data.grid, data.dragFromSlot, data.toSlot, data.dragQuantity);

            rebuild();
            return;
        }
        if(data.type.equals("Drop")) {
            onDrop(getNameFromID(data.fromSection), data.grid, data.fromSlot, data.toSlot, data.quantity);

            setItemGridSlots(data.grid, componentMapping.get(data.grid).container);
        }
    }

    protected void translateBlockName(BlockType type) {
        String contentName = "CONTENT";

        if(type != null) {
            contentName = Message.translation(type.getItem().getTranslationKey()).getAnsiMessage().toUpperCase();
        }

        globalBuilder.set("#TitleText2.Text", contentName);
    }

    protected void addEnergyUI(Ref<EntityStore> ref) {
        globalBuilder.append("#ContentContainerGroup", "Pages/Common/EnergyUI.ui");

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

        EnergyComponent energy = block.getStore().getComponent(block, ArchStar.get().getEnergyComponentType());

        refreshEnergy(energy, true);
    }

    protected void addUI(String name, IMachineUIComponent component) {
        globalBuilder.append("#ContentContainerGroup", "Pages/Common/" + name + "UI.ui");
        globalBuilder.set("#" + name + "ItemGrid.InventorySectionId", component.getSectionID());

        UIComponentContext context = new UIComponentContext(name, component);
        componentMapping.put(name, context);

        refreshUI(name, context.container);

        if(context.container != null && context.container.getCapacity() > 1) {
            globalBuilder.set("#" + name + "ItemGrid.SlotsPerRow", 2);
        }
    }

    protected void enableItemGridEventBindings(UIEventBuilder event, String name) {
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#" + name + "ItemGrid", new EventData().append("Type", "Drop").append("Grid", name), true);
        event.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#" + name + "ItemGrid", new EventData().append("Type", "Release").append("Grid", name), false);
    }

    public void refreshAllUI() {
        for(UIComponentContext context : componentMapping.values()) {
            refreshUI(context.name, context.container);
        }
    }

    protected void refreshUI(String name, ItemContainer container) {
        setItemGridSlots(name, container);
        refreshProgressBar(name);
    }

    protected void setItemGridSlots(String name, ItemContainer container) {
        if(container == null) return;

        ItemGridSlot[] slots = new ItemGridSlot[container.getCapacity()];

        for(short i = 0; i < container.getCapacity(); i++) {
            slots[i] = new ItemGridSlot();
            slots[i].setActivatable(true);
            slots[i].setItemStack(container.getItemStack(i));
        }

        globalBuilder.set("#" + name + "ItemGrid.Slots", slots);
    }

    public void refreshProgressBar(String name) {
        if(componentMapping.get(name).progress < 0) return;

        componentMapping.get(name).updateProgress();

        globalBuilder.set("#" + name + "Progress.Value", componentMapping.get(name).progress);
    }

    public void sendBuilder(UICommandBuilder builder) {
        sendUpdate(builder, null, false);
    }

    private void setEnergyImage(String path) {
        if(path != null && !path.isEmpty()) {
            globalBuilder.set("#EnergyImage.AssetPath", path);
        } else {
            globalBuilder.setNull("#EnergyImage.AssetPath");
            ArchStar.LOGGER.at(Level.WARNING).log("Energy Image is NULL!");
        }
    }

    public void refreshEnergy(EnergyComponent energyComponent, boolean override) {
        int current = energyComponent.getCurrentEnergy();
        int max = energyComponent.getMaxEnergy();

        globalBuilder.set("#EnergyAmount.Text", current + " / " + max);

        int threshold = Math.ceilDiv(max, 5);

        if(current == 0) {
            setEnergyState(EnergyImageState.EMPTY, override);
        } else if(current < threshold) {
            setEnergyState(EnergyImageState.DEPLETED, override);
        } else if (current < threshold * 2) {
            setEnergyState(EnergyImageState.LOW, override);
        } else if (current < threshold * 3) {
            setEnergyState(EnergyImageState.UNDER_HALF, override);
        } else if (current < threshold * 4) {
            setEnergyState(EnergyImageState.ABOVE_HALF, override);
        } else if (current < max) {
            setEnergyState(EnergyImageState.HIGH, override);
        } else if (current == max) {
            setEnergyState(EnergyImageState.FULL, override);
        }
    }

    private void setEnergyState(EnergyImageState state, boolean override) {
        if(!override && energyState == state) {
            return;
        }

        energyState = state;

        switch (energyState) {
            case EMPTY -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Empty.png");
            case DEPLETED -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Dying.png");
            case LOW -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Low.png");
            case UNDER_HALF -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Average.png");
            case ABOVE_HALF -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Medium.png");
            case HIGH -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_High.png");
            case FULL -> setEnergyImage("UI/Custom/Pages/Textures/EnergyUI/EnergyUI_Full.png");
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);

        isDismissed = true;
    }

    private void playSlotClick(ItemContainer container, short slot) {
        World world = Universe.get().getWorld(playerRef.getWorldUuid());

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

            SoundUtil.playSoundEvent3dToPlayer(playerRef.getReference(), SoundEvent.getAssetMap().getIndex(dropSound), SoundCategory.UI, pos.toVector3d(), playerRef.getReference().getStore());
        });
    }

    public static class CommonData {
        public short toSlot;

        //Custom
        public String type;
        public String grid;

        public short fromSlot;
        public int quantity;
        public int fromSection;

        public short dragFromSlot;
        public int dragQuantity;
        public int dragFromSection;

        public static final BuilderCodec<CommonData> CODEC =
                BuilderCodec.builder(CommonData.class, CommonData::new)
                        .append(new KeyedCodec<>("Type", Codec.STRING), (entry, s) -> entry.type = s, (entry) -> entry.type).add()
                        .append(new KeyedCodec<>("Grid", Codec.STRING), (entry, s) -> entry.grid = s, (entry) -> entry.grid).add()
                        .append(new KeyedCodec<>("SlotIndex", Codec.SHORT), (entry, s) -> entry.toSlot = s, (entry) -> entry.toSlot).add()
                        .append(new KeyedCodec<>("SourceItemGridIndex", Codec.SHORT), (entry, s) -> entry.fromSlot = s, (entry) -> entry.fromSlot).add()
                        .append(new KeyedCodec<>("ItemStackQuantity", Codec.INTEGER), (entry, s) -> entry.quantity = s, (entry) -> entry.quantity).add()
                        .append(new KeyedCodec<>("SourceInventorySectionId", Codec.INTEGER), (entry, s) -> entry.fromSection = s, (entry) -> entry.fromSection).add()
                        .append(new KeyedCodec<>("DragSourceItemGridIndex", Codec.SHORT), (entry, s) -> entry.dragFromSlot = s, (entry) -> entry.dragFromSlot).add()
                        .append(new KeyedCodec<>("DragItemStackQuantity", Codec.INTEGER), (entry, s) -> entry.dragQuantity = s, (entry) -> entry.dragQuantity).add()
                        .append(new KeyedCodec<>("DragSourceInventorySectionId", Codec.INTEGER), (entry, s) -> entry.dragFromSection = s, (entry) -> entry.dragFromSection).add()
                        .build();
    }
}