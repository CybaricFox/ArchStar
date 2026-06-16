package com.CybaricFox.Modules.ArchLibrary.Interactions;

import com.CybaricFox.Modules.ArchLibrary.ArchLibrary;
import com.CybaricFox.Modules.ArchLibrary.ArchStarDamageCalculator;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.logging.Level;


public class AdvancedDamageInteraction extends DamageEntityInteraction {
    public static final BuilderCodec<AdvancedDamageInteraction> CODEC;

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef != null && targetRef.isValid() && context.getEntity().isValid()) {
            if(damageCalculator instanceof ArchStarDamageCalculator archStarDamageCalculator) {
                archStarDamageCalculator.calculateExecution(context.getTargetEntity());
                archStarDamageCalculator.calculateMaxHealthDamage(context.getTargetEntity(), getRunTime());
            } else {
                ArchLibrary.LOGGER.at(Level.SEVERE).log("Advanced Damage Interaction is not using ArchStar's Damage Calculator!");
            }
        }

        super.tick0(firstRun, time, type, context, cooldownHandler);
    }

    static {
        CODEC = (BuilderCodec.builder(AdvancedDamageInteraction.class, AdvancedDamageInteraction::new, DamageEntityInteraction.CODEC))
                .append(new KeyedCodec<>("DamageCalculator", ArchStarDamageCalculator.CODEC), (interaction, s) -> interaction.damageCalculator = s, (interaction) -> (ArchStarDamageCalculator) interaction.damageCalculator).add()
                .build();
    }
}
