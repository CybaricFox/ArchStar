package com.CybaricFox.Modules.ArchLibrary.Interactions;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class SignatureInteraction extends SimpleInteraction {
    private enum ActionType {
        SET_MAX,
        ADD,
        RESET,
        NOT_SET
    }

    public static final BuilderCodec<SignatureInteraction> CODEC;
    private static int signatureIndex = -1;

    private ActionType action = ActionType.NOT_SET;
    private float value;

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        super.tick0(firstRun, time, type, context, cooldownHandler);

        Ref<EntityStore> ref = context.getEntity();

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if(player == null) return;

        CommandBuffer<EntityStore> buffer = context.getCommandBuffer();
        if(buffer == null) return;

        EntityStatMap stats = buffer.getComponent(ref, EntityStatMap.getComponentType());
        if(stats == null) return;

        if(signatureIndex == -1) {
            signatureIndex = DefaultEntityStatTypes.getSignatureEnergy();
        }

        buffer.getExternalData().getWorld().execute(() -> {
            switch (action) {
                case SET_MAX -> {
                    if(value == 0) {
                        stats.removeModifier(signatureIndex, "ArchStar_HeldItem");
                    } else {
                        stats.putModifier(signatureIndex, "ArchStar_HeldItem", new StaticModifier(Modifier.ModifierTarget.MAX, StaticModifier.CalculationType.ADDITIVE, value));
                    }
                }
                case ADD -> stats.addStatValue(signatureIndex, value);
                case RESET -> stats.resetStatValue(signatureIndex);
                case NOT_SET -> ArchLibrary.LOGGER.at(Level.WARNING).log("Signature Event Failed! Action was not set!");
            }
        });
    }

    static {
        CODEC = (BuilderCodec.builder(SignatureInteraction.class, SignatureInteraction::new, SimpleInteraction.CODEC))
                .append(new KeyedCodec<>("Action", Codec.STRING), (interaction, s) -> interaction.action = ActionType.valueOf(s.toUpperCase()), (interaction) -> interaction.action.toString()).add()
                .append(new KeyedCodec<>("Value", Codec.FLOAT), (interaction, s) -> interaction.value = s, (interaction) -> interaction.value).add()
                .build();
    }
}
