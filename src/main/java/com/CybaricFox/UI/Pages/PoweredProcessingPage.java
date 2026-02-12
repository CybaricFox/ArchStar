package com.CybaricFox.UI.Pages;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class PoweredProcessingPage extends CommonPage {
    public PoweredProcessingPage(@Nonnull PlayerRef playerRef, @Nonnull BuilderCodec<CommonData> eventDataCodec, Vector3i pos) {
        super(playerRef, eventDataCodec);

        this.pos = pos;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiCommandBuilder, @Nonnull UIEventBuilder uiEventBuilder, @Nonnull Store<EntityStore> store) {
        super.build(ref, uiCommandBuilder, uiEventBuilder, store);

        addEnergyUI(ref, uiCommandBuilder);
        addInputUI(ref, uiCommandBuilder, uiEventBuilder);
        addOutputUI(ref, uiCommandBuilder, uiEventBuilder);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);


    }
}
