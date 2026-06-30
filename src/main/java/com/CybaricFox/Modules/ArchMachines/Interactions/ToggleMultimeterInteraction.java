package com.CybaricFox.Modules.ArchMachines.Interactions;

import com.CybaricFox.ArchStar;
import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchMachines.Components.MachineBehaviorComponent;
import com.CybaricFox.Modules.ArchMachines.Systems.MultimeterSystem;
import com.CybaricFox.Modules.ArchMachines.UI.Pages.CommonPage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3iUtil;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/*
    Interaction for opening powered processing blocks
 */
public class ToggleMultimeterInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<ToggleMultimeterInteraction> CODEC;

    public ToggleMultimeterInteraction() {
    }

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType, @Nonnull InteractionContext interactionContext, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> entity = interactionContext.getEntity();

        PlayerRef player = entity.getStore().getComponent(entity, PlayerRef.getComponentType());
        if(player == null) return;

        MultimeterSystem.handlePlayer(player.getUuid());
    }

    static {
        CODEC = (BuilderCodec.builder(ToggleMultimeterInteraction.class, ToggleMultimeterInteraction::new, SimpleInstantInteraction.CODEC)).build();
    }
}
