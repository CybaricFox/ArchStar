package com.CybaricFox.Modules.ArchMachines.UI.Pages;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.FuelComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class GeneratorPage extends CommonPage {

    public GeneratorPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonData> eventDataCodec, Vector3i pos) {
        super(playerRef, eventDataCodec, pos);
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        super.build(ref, uiCommandBuilder, uiEventBuilder, store);

        Ref<ChunkStore> blockRef = ArchLibrary.getBlockEntity(ref.getStore().getExternalData().getWorld(), pos);

        FuelComponent fuelComponent = blockRef.getStore().getComponent(blockRef, FuelComponent.getComponentType());
        addUI("Fuel", fuelComponent, null);
        enableItemGridEventBindings(uiEventBuilder, "Fuel");

        addEnergyUI(ref);

        globalBuilder = null;
        isValid = true;
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);

        int soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Furnace_Bench_Close");
        SoundUtil.playSoundEvent3dToPlayer(ref, soundIndex, SoundCategory.UI, Vector3iUtil.toVector3d(pos), store);
    }
}
