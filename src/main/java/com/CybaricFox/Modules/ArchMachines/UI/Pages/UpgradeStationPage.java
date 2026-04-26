package com.CybaricFox.Modules.ArchMachines.UI.Pages;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.FuelComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class UpgradeStationPage extends CommonPage {

    public UpgradeStationPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonData> eventDataCodec, Vector3i pos) {
        super(playerRef, eventDataCodec, pos);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        super.build(ref, uiCommandBuilder, uiEventBuilder, store);

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(ref.getStore().getExternalData().getWorld(), pos);

        ItemContainerBlock itemContainer = blockRef.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());
        addUI(itemContainer, 1, 3);
        enableItemGridEventBindings(uiEventBuilder, "ItemContainer");
        setItemUpgrades(uiEventBuilder);

        globalBuilder = null;
        isValid = true;
    }

    @Override
    public void onTick(Player player, CommandBuffer<EntityStore> commandBuffer) {
        super.onTick(player, commandBuffer);

        String dataString = "";

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(player.getWorld(), pos);
        ItemContainerBlock itemContainerBlock = blockRef.getStore().getComponent(blockRef, ItemContainerBlock.getComponentType());
        if(itemContainerBlock == null) return;

        ItemStack item = itemContainerBlock.getItemContainer().getItemStack((short) 0);
        if(item == null) {
            globalBuilder.set("#DataLabel.Text", dataString);
            return;
        }

        if(item.getMaxDurability() != 0) {
            if(!item.getItem().getData().getRawTags().isEmpty()) {
                for(String tag : item.getItem().getData().getRawTags().get("Type")) {
                    if(tag.equals("Powered_Item") || tag.equals("Battery")) {
                        dataString = dataString.concat("Power: " + item.getDurability() + "v/" + item.getMaxDurability() + "v\n");
                    } else {
                        dataString = dataString.concat("Durability: " + ((int) item.getDurability()) + "/" + ((int) item.getMaxDurability()));
                    }
                }
            } else {
                dataString = dataString.concat("Durability: " + item.getDurability() + "/" + item.getMaxDurability());
            }
        }

        globalBuilder.set("#DataLabel.Text", dataString);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);

        int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Furnace_Bench_Close");
        SoundUtil.playSoundEvent3dToPlayer(ref, soundIndex, SoundCategory.UI, pos.toVector3d(), store);
    }
}
