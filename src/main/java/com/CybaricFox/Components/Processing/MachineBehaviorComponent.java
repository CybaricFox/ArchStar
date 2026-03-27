package com.CybaricFox.Components.Processing;

import com.CybaricFox.API.EssentialsContext;
import com.CybaricFox.Components.CommonContainerComponent;
import com.CybaricFox.Components.Processing.MachineBehavior.MachineBehavior;
import com.CybaricFox.Components.Processing.MachineBehavior.MachineBehaviorRegistry;
import com.CybaricFox.UI.Pages.Common.IMachineUIComponent;
import com.CybaricFox.UI.Pages.CommonPage;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nullable;

public class MachineBehaviorComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MachineBehaviorComponent> CODEC;

    private MachineBehavior machineBehavior;

    public MachineBehaviorComponent() {

    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new MachineBehaviorComponent();
    }

    public void setMachineBehavior(String blockId) {
        machineBehavior = MachineBehaviorRegistry.create(blockId);
    }

    public CommonPage getPage(PlayerRef playerRef, BuilderCodec<CommonPage.CommonData> data, Vector3i pos) {
        return machineBehavior.getPage(playerRef, data, pos);
    }

    public boolean run(EssentialsContext context) {
        if(machineBehavior == null) {
            throw new NullPointerException("Machine Block behaviour not set! Attempted to run the behaviour before setting it!");
        }

        return machineBehavior.run(context);
    }

    static {
        CODEC = (BuilderCodec.builder(MachineBehaviorComponent.class, MachineBehaviorComponent::new)).build();
    }
}
