package com.CybaricFox.Modules.ArchMachines.UI.Pages;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchEnergy.Components.EnergyComponent;
import com.CybaricFox.Modules.ArchLibrary.IDataPanel;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.Common.EnergyImageState;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.Common.IMachineUIComponent;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.Common.UIComponentContext;
import com.CybaricFox.Modules.ArchMachines.Upgrade.BaseUpgrade;
import com.CybaricFox.Modules.ArchMachines.Upgrade.UpgradeRegistry;
import com.CybaricFox.Modules.ArchMachines.UpgradeType;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import org.joml.Vector3i;
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
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class CommonPage extends InteractiveCustomUIPage<CommonPage.CommonData> {
    //The number of upgrades the ui system currently supports. Do not change this value without adding additional groups to the ui.
    private final int UPGRADE_SIZE = 10;

    //Position of the block this interacted with
    protected Vector3i pos;

    protected HashMap<String, UIComponentContext> componentMapping = new HashMap<>();

    protected boolean isDismissed = false;

    protected EnergyImageState energyState = EnergyImageState.EMPTY;

    protected UICommandBuilder globalBuilder;
    public boolean isValid = false;

    protected HashMap<String, IDataPanel> dataPanels = new HashMap<>();

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

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, pos);

        MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
        if(machineBehaviorComponent != null) {
            refreshData(machineBehaviorComponent);
        }

        uiCommandBuilder.append("#InventoryRow", "Pages/Common/InventoryUI.ui");

        @SuppressWarnings("removal") CombinedItemContainer container = new CombinedItemContainer(player.getInventory().getStorage(), player.getInventory().getHotbar());
        componentMapping.put("Inventory", new UIComponentContext("Inventory", container, true, 1));

        setItemGridSlots("Inventory", container);
        enableItemGridEventBindings(uiEventBuilder, "Inventory");
        setUpgrades(type, machineBehaviorComponent, uiEventBuilder);
        translateBlockName(type);
    }

    protected void addDataPanel(String title, IDataPanel data) {
        dataPanels.put(title, data);
    }

    public void refreshData(MachineBehaviorComponent machineBehaviorComponent) {
        if(machineBehaviorComponent.displayData()) {
            String dataString = "";

            for(Map.Entry<String, IDataPanel> panel : dataPanels.entrySet()) {
                dataString = dataString.concat(panel.getKey().toUpperCase() + ":" + "\n\n");

                ArrayList<String> data = panel.getValue().getData();

                for(String line : data) {
                    dataString = dataString.concat(line + "\n");
                }
            }

            globalBuilder.set("#DataLabel.Text", dataString);
        }
    }

    private void setUpgrades(BlockType blockType, MachineBehaviorComponent machineBehaviorComponent, UIEventBuilder eventBuilder) {
        ArrayList<String> upgrades = UpgradeRegistry.getUpgrades(blockType.getItem().getId());
        if(upgrades == null) return;
        if(upgrades.size() > UPGRADE_SIZE) throw new ArrayIndexOutOfBoundsException(blockType.getItem().getId() + " has more than " + UPGRADE_SIZE + " upgrades in the registry! Number of upgrades may not surpass this limit!");

        int upgradeGroup = 0;
        for (String string : upgrades) {
            //Get upgrade and its info
            BaseUpgrade upgrade = UpgradeRegistry.getUpgradeByType(string, UpgradeType.BLOCK);
            if(upgrade == null) continue;
            ArrayList<String> items = upgrade.getItems();
            ItemGridSlot[] slots = new ItemGridSlot[items.size()];

            //Append an upgrade ui element and set its title
            globalBuilder.append("#Upgrades" + upgradeGroup, "Pages/Common/Upgrade.ui");
            globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeTitle.Text", upgrade.getName());
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Upgrades" + upgradeGroup + " #UpgradeButton", new EventData().append("Type", "Upgrade").append("Grid", upgrade.getId()).append("Signature", "ARCH-SIG"), false);

            //Setup the item requirements
            for (short s = 0; s < items.size(); s++) {
                ItemGridSlot slot = new ItemGridSlot();
                ItemStack stack = new ItemStack(items.get(s), upgrade.getItemQuantity(items.get(s)));
                if (stack.isValid()) {
                    slot.setItemStack(stack);
                } else {
                    ArchLibrary.LOGGER.at(Level.SEVERE).log(upgrade.getName() + " contains an invalid item in its item requirements! Invalid item: " + stack.getItemId());
                    globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.Disabled", true);
                }
                slots[s] = slot;
            }
            globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeItemGrid.Slots", slots);
            globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeItemGrid.SlotsPerRow", slots.length);

            //Setup the final parts of the upgrade element
            globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeImage.AssetPath", upgrade.getIconPath());

            if (machineBehaviorComponent.containsUpgrade(upgrade.getId())) {
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.Text", "Uninstall");
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.OutlineColor", "#660000");
            }
            globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.TooltipText", upgrade.getDesc());

            upgradeGroup++;
        }
    }

    private void refreshUpgrades(BlockType blockType, MachineBehaviorComponent machineBehaviorComponent) {
        ArrayList<String> upgrades = UpgradeRegistry.getUpgrades(blockType.getItem().getId());
        if(upgrades == null) return;
        if(upgrades.size() > UPGRADE_SIZE) throw new ArrayIndexOutOfBoundsException(blockType.getItem().getId() + " has more than " + UPGRADE_SIZE + " upgrades in the registry! Number of upgrades may not surpass this limit!");

        for(int i = 0; i < upgrades.size(); i++) {
            //Get upgrade and its info
            BaseUpgrade upgrade = UpgradeRegistry.getUpgrade(upgrades.get(i));

            if(machineBehaviorComponent.containsUpgrade(upgrade.getId())) {
                globalBuilder.set("#Upgrades" + i + " #UpgradeButton.Text", "Uninstall");
                globalBuilder.set("#Upgrades" + i + " #UpgradeButton.OutlineColor", "#660000");
            } else {
                globalBuilder.set("#Upgrades" + i + " #UpgradeButton.Text", "Install");
                globalBuilder.set("#Upgrades" + i + " #UpgradeButton.OutlineColor", "#00CC00");
            }
        }
    }

    public void beginBuildingCycle() {
        sendBuilder(globalBuilder);
        globalBuilder = new UICommandBuilder();
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

    protected void setItemUpgrades(UIEventBuilder eventBuilder) {
        ItemContainer container = componentMapping.get("ItemContainer").container;
        ItemStack itemStack = container.getItemStack((short) 0);
        if(itemStack != null) {
            ArrayList<String> upgrades = UpgradeRegistry.getUpgrades(itemStack.getItem().getId());
            if(upgrades == null) return;
            if(upgrades.size() > UPGRADE_SIZE) throw new ArrayIndexOutOfBoundsException(itemStack.getItem().getId() + " has more than " + UPGRADE_SIZE + " upgrades in the registry! Number of upgrades may not surpass this limit!");

            int upgradeGroup = 0;
            for (String string : upgrades) {
                //Get upgrade and its info
                BaseUpgrade upgrade = UpgradeRegistry.getUpgradeByType(string, UpgradeType.ITEM);
                if (upgrade == null) {
                    continue;
                }
                ArrayList<String> items = upgrade.getItems();
                ItemGridSlot[] slots = new ItemGridSlot[items.size()];

                //Append an upgrade ui element and set its title
                globalBuilder.append("#Upgrades" + upgradeGroup, "Pages/Common/Upgrade.ui");
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeTitle.Text", upgrade.getName());
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Upgrades" + upgradeGroup + " #UpgradeButton", new EventData().append("Type", "Upgrade").append("Grid", upgrade.getId()).append("Signature", "ARCH-SIG"), false);

                //Setup the item requirements
                for (short s = 0; s < items.size(); s++) {
                    ItemGridSlot slot = new ItemGridSlot();
                    ItemStack stack = new ItemStack(items.get(s), upgrade.getItemQuantity(items.get(s)));
                    if (stack.isValid()) {
                        slot.setItemStack(stack);
                    } else {
                        ArchLibrary.LOGGER.at(Level.SEVERE).log(upgrade.getName() + " contains an invalid item in its item requirements! Invalid item: " + stack.getItemId());
                        globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.Disabled", true);
                    }
                    slots[s] = slot;
                }
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeItemGrid.Slots", slots);
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeItemGrid.SlotsPerRow", slots.length);

                //Setup the final parts of the upgrade element
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeImage.AssetPath", upgrade.getIconPath());
                globalBuilder.set("#Upgrades" + upgradeGroup + " #UpgradeButton.TooltipText", upgrade.getDesc());

                upgradeGroup++;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, CommonData data) {
        super.handleDataEvent(ref, store, data);

        //ARCH-PRO stands for processed. Do not run the event if the data has not been processed by the ui fixer. This prevents the event from running twice at once.
        if(!data.signature.equals("ARCH-PRO")) {
            return;
        }

        if(data.type.equals("Release")) {
            onDrop(getNameFromID(data.dragFromSection), data.grid, data.dragFromSlot, data.toSlot, data.dragQuantity);
            isValid = false;
            rebuild();
            return;
        }
        if(data.type.equals("Drop")) {
            onDrop(getNameFromID(data.fromSection), data.grid, data.fromSlot, data.toSlot, data.quantity);

            if(componentMapping.containsKey("ItemContainer") && componentMapping.get("ItemContainer").sectionID == 3) {
                if(data.grid.equals("ItemContainer") || data.fromSection == 3) {
                    isValid = false;
                    rebuild();
                } else {
                    setItemGridSlots(data.grid, componentMapping.get(data.grid).container);
                }
            } else {
                setItemGridSlots(data.grid, componentMapping.get(data.grid).container);
            }
            return;
        }
        if(data.type.equals("Upgrade")) {
            BaseUpgrade upgrade = UpgradeRegistry.getUpgrade(data.grid);

            World world = ref.getStore().getExternalData().getWorld();
            Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(world, pos);
            MachineBehaviorComponent machineBehaviorComponent = blockRef.getStore().getComponent(blockRef, MachineBehaviorComponent.getComponentType());
            Player playerComponent = ref.getStore().getComponent(ref, Player.getComponentType());

            if(machineBehaviorComponent.containsUpgrade(upgrade.getId())) {
                //noinspection removal
                upgrade.refundItems(ref, blockRef.getStore().getExternalData().getWorld(), pos);
                machineBehaviorComponent.purchaseUpgrade(upgrade.getId(), blockRef);
                //refreshUpgrades(blockRef.getStore().getExternalData().getWorld().getBlockType(pos), machineBehaviorComponent);
                isValid = false;
                rebuild();
            } else {
                @SuppressWarnings("removal") boolean result = upgrade.consumeItems(playerComponent.getInventory().getCombinedStorageFirst());
                if(result) {
                    if(upgrade.type == UpgradeType.BLOCK) {
                        machineBehaviorComponent.purchaseUpgrade(upgrade.getId(), blockRef);
                    } else {
                        UIComponentContext context = componentMapping.get("ItemContainer");
                        ItemStack resultItem = machineBehaviorComponent.purchaseUpgrade(upgrade.getId(), context.container.getItemStack((short) 0));
                        if(resultItem == null) {
                            upgrade.refundItems(ref, world, pos);
                        } else {
                            context.container.setItemStackForSlot((short) 0, resultItem);
                        }
                    }
                    //refreshUpgrades(blockRef.getStore().getExternalData().getWorld().getBlockType(pos), machineBehaviorComponent);
                    isValid = false;
                    rebuild();
                }
            }
        }
    }

    protected void translateBlockName(BlockType type) {
        String contentName = "CONTENT";

        if(type != null) {
            contentName = Message.translation(type.getItem().getTranslationKey()).getAnsiMessage().toUpperCase();
        }

        globalBuilder.set("#TitleText2.Text", contentName);
    }

    public void onTick(Player player, CommandBuffer<EntityStore> commandBuffer) {
        if(!isValid) return;
        beginBuildingCycle();

        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), getPos());
        EnergyComponent energyComponent = block.getStore().getComponent(block, EnergyComponent.getComponentType());
        if(energyComponent != null) {
            refreshEnergy(energyComponent, false);
        }

        MachineBehaviorComponent behaviorComponent = block.getStore().getComponent(block, MachineBehaviorComponent.getComponentType());
        if(behaviorComponent != null && behaviorComponent.displayData()) {
            refreshData(behaviorComponent);
        }

        refreshAllUI();
    }

    protected void addEnergyUI(Ref<EntityStore> ref) {
        globalBuilder.append("#ContentContainerGroup", "Pages/Common/EnergyUI.ui");

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        Ref<ChunkStore> block = ArchLibrary.getBlockEntity(player.getWorld(), pos);

        EnergyComponent energy = block.getStore().getComponent(block, EnergyComponent.getComponentType());
        dataPanels.put("ENERGY DATA", energy);

        refreshEnergy(energy, true);
    }

    protected void addUI(String name, IMachineUIComponent component, IDataPanel data) {
        globalBuilder.append("#ContentContainerGroup", "Pages/Common/" + name + "UI.ui");
        globalBuilder.set("#" + name + "ItemGrid.InventorySectionId", component.getSectionID());

        UIComponentContext context = new UIComponentContext(name, component, data);
        componentMapping.put(name, context);

        refreshUI(name, context.container);

        if(context.container != null && context.container.getCapacity() > 1) {
            globalBuilder.set("#" + name + "ItemGrid.SlotsPerRow", 2);
        }
    }
    protected void addUI(ItemContainerBlock itemContainerBlock, int slotsPerRow, int sectionId) {
        globalBuilder.append("#ContentContainerGroup", "Pages/Common/" + "ItemContainer" + "UI.ui");
        globalBuilder.set("#" + "ItemContainer" + "ItemGrid.InventorySectionId", sectionId);
        globalBuilder.set("#" + "ItemContainer" + "ItemGrid.SlotsPerRow", slotsPerRow);

        componentMapping.put("ItemContainer", new UIComponentContext("ItemContainer", itemContainerBlock.getItemContainer(), true, sectionId));

        refreshUI("ItemContainer", itemContainerBlock.getItemContainer());
    }

    protected void enableItemGridEventBindings(UIEventBuilder event, String name) {
        event.addEventBinding(CustomUIEventBindingType.Dropped, "#" + name + "ItemGrid", new EventData().append("Type", "Drop").append("Grid", name).append("Signature", "ARCH-SIG"), true);
        event.addEventBinding(CustomUIEventBindingType.SlotClickReleaseWhileDragging, "#" + name + "ItemGrid", new EventData().append("Type", "Release").append("Grid", name).append("Signature", "ARCH-SIG"), false);
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
            //This removes metadata from the item that is generated for the ui. Mods that add alot of metadata can overwhelm the ui system.
            ItemStack item = container.getItemStack(i);
            if(item != null) {
                ItemStack invItem = new ItemStack(item.getItem().getId(), item.getQuantity(), item.getDurability(), item.getMaxDurability(), null);
                slots[i].setItemStack(invItem);
            }
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

            SoundUtil.playSoundEvent3dToPlayer(playerRef.getReference(), SoundEvent.getAssetMap().getIndex(dropSound), SoundCategory.UI, Vector3iUtil.toVector3d(pos), playerRef.getReference().getStore());
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

        public String signature = "ARCH-SIG";

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
                        .append(new KeyedCodec<>("Signature", Codec.STRING), (entry, s) -> entry.signature = s, (entry) -> entry.signature).add()
                        .build();
    }
}