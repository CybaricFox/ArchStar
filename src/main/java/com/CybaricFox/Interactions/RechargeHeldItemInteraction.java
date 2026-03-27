package com.CybaricFox.Interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class RechargeHeldItemInteraction extends SimpleInteraction {
    public static final BuilderCodec<RechargeHeldItemInteraction> CODEC;

    @Override
    protected void tick0(boolean firstRun, float time, @Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
        super.tick0(firstRun, time, type, context, cooldownHandler);

        Ref<EntityStore> ref = context.getEntity();

        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        if(player == null) return;

        @SuppressWarnings("removal") CombinedItemContainer container = player.getInventory().getCombinedHotbarFirst();

        @SuppressWarnings("removal") ItemStack item = player.getInventory().getActiveHotbarItem();

        for(short i = 0; i < container.getCapacity(); i++) {
            ItemStack containedItem = container.getItemStack(i);
            if(containedItem != null) {
                if(containedItem.getItem().getData().getRawTags().isEmpty()) continue;
                for(String tag : containedItem.getItem().getData().getRawTags().get("Type")) {
                    if(tag.equals("Battery") && containedItem.getDurability() > 0) {
                        double request = item.getMaxDurability() - item.getDurability();
                        double amount = Math.min(containedItem.getDurability(), request);

                        ItemStack newContained = containedItem.withDurability(containedItem.getDurability() - amount);
                        container.setItemStackForSlot(i, newContained);

                        ItemStack newItem = item.withIncreasedDurability(amount);
                        //noinspection removal
                        player.getInventory().getHotbar().setItemStackForSlot(player.getInventory().getActiveHotbarSlot(), newItem);

                        return;
                    }
                }
            }
        }
    }

    static {
        CODEC = (BuilderCodec.builder(RechargeHeldItemInteraction.class, RechargeHeldItemInteraction::new, SimpleInteraction.CODEC)).build();
    }
}
