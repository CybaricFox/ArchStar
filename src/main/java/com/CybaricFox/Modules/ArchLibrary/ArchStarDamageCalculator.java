package com.CybaricFox.Modules.ArchLibrary;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.map.Object2FloatMapCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.ints.Int2FloatMap;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import javax.annotation.Nullable;
import java.util.logging.Level;

public class ArchStarDamageCalculator extends DamageCalculator {
    public enum ExecutionType {
        CURRENT,
        MAX,
        PERCENT
    }

    public static final BuilderCodec<ArchStarDamageCalculator> CODEC;
    private Object2FloatMap<String> maxHealthPercentageRaw;
    protected Int2FloatMap maxHealthPercentage;
    protected float executionThreshold;
    protected ExecutionType executionType = ExecutionType.CURRENT;

    private Object2FloatMap<DamageCause> storedDamage = new Object2FloatOpenHashMap<>();

    @Nullable
    @Override
    public Object2FloatMap<DamageCause> calculateDamage(double durationSeconds) {
        if(storedDamage == null) {
            storedDamage = super.calculateDamage(durationSeconds);
        } else {
            Object2FloatMap<DamageCause> baseDamage = super.calculateDamage(durationSeconds);

            if(baseDamage != null && !baseDamage.isEmpty()) {
                for (Object2FloatMap.Entry<DamageCause> entry : baseDamage.object2FloatEntrySet()) {
                    addDamage(entry.getKey(), entry.getFloatValue());
                }
            }
        }

        if(storedDamage != null && !storedDamage.isEmpty()) {
            Object2FloatMap<DamageCause> toReturn = new Object2FloatOpenHashMap<>(storedDamage);

            for (Object2FloatMap.Entry<DamageCause> entry : storedDamage.object2FloatEntrySet()) {
                ArchLibrary.LOGGER.at(Level.INFO).log("STORED DAMAGE");
                ArchLibrary.LOGGER.at(Level.INFO).log(entry.getKey().getId() + " Damage: " + entry.getFloatValue());
            }

            storedDamage.clear();
            return toReturn;
        }

        return null;
    }

    public void calculateMaxHealthDamage(Ref<EntityStore> victim, float duration) {
        EntityStatMap stats = victim.getStore().getComponent(victim, EntityStatMap.getComponentType());
        if(stats == null) {
            ArchLibrary.LOGGER.at(Level.WARNING).log("Failed to deal max health damage to target entity. EntityStatMap is null!");
            return;
        }

        float maxHealth = stats.get(DefaultEntityStatTypes.getHealth()).getMax();
        if(maxHealth <= 0) return;

        if (this.maxHealthPercentageRaw != null && !this.maxHealthPercentageRaw.isEmpty()) {
            ObjectIterator var5 = this.maxHealthPercentage.int2FloatEntrySet().iterator();

            while(var5.hasNext()) {
                Int2FloatMap.Entry entry = (Int2FloatMap.Entry)var5.next();
                DamageCause damageCause = DamageCause.getAssetMap().getAsset(entry.getIntKey());
                float value = entry.getFloatValue() / 100;
                float damage = this.scaleDamage(duration, maxHealth * value);
                addDamage(damageCause, damage);
            }
        }
    }

    public void calculateExecution(Ref<EntityStore> victim) {
        if(executionThreshold <= 0) return;

        EntityStatMap stats = victim.getStore().getComponent(victim, EntityStatMap.getComponentType());
        if(stats == null) {
            ArchLibrary.LOGGER.at(Level.WARNING).log("Failed to deal execution damage to target entity. EntityStatMap is null!");
            return;
        }

        float value = 0;

        switch(executionType) {
            case CURRENT -> value = stats.get(DefaultEntityStatTypes.getHealth()).get();
            case MAX -> value = stats.get(DefaultEntityStatTypes.getHealth()).getMax();
            case PERCENT -> value = stats.get(DefaultEntityStatTypes.getHealth()).asPercentage();
        }

        if(value <= executionThreshold) {
            addDamage(DamageCause.getAssetMap().getAsset(DamageCause.getAssetMap().getIndex("Physical")), 9999999);
        }
    }

    private void addDamage(DamageCause cause, float value) {
        if(storedDamage.containsKey(cause)) {
            storedDamage.put(cause, storedDamage.getFloat(cause) + value);
        } else {
            storedDamage.put(cause, value);
        }
    }

    protected float scaleDamage(double durationSeconds, float damage) {
        float var10000;
        switch (this.type.ordinal()) {
            case 0 -> var10000 = (float)durationSeconds * damage;
            case 1 -> var10000 = damage;
            default -> throw new MatchException(null, null);
        }

        return var10000;
    }

    static {
        CODEC = (BuilderCodec.builder(ArchStarDamageCalculator.class, ArchStarDamageCalculator::new, DamageCalculator.CODEC))
                .append(new KeyedCodec<>("ExecutionThreshold", Codec.FLOAT), (interaction, s) -> interaction.executionThreshold = s, (interaction) -> interaction.executionThreshold).add()
                .append(new KeyedCodec<>("ExecutionType", Codec.STRING), (interaction, s) -> interaction.executionType = ExecutionType.valueOf(s), (interaction) -> interaction.executionType.toString()).add()
                .append(new KeyedCodec<>("MaxHealthDamage", new Object2FloatMapCodec<>(Codec.STRING, Object2FloatOpenHashMap::new)), (interaction, s) -> interaction.maxHealthPercentageRaw = s, (interaction) -> interaction.maxHealthPercentageRaw).add().afterDecode((asset) -> {
                    if (asset.maxHealthPercentageRaw != null) {
                        asset.maxHealthPercentage = new Int2FloatOpenHashMap();

                        for (Object2FloatMap.Entry<String> entry : asset.maxHealthPercentageRaw.object2FloatEntrySet()) {
                            int index = DamageCause.getAssetMap().getIndex(entry.getKey());
                            asset.maxHealthPercentage.put(index, entry.getFloatValue());
                        }
                    }
                }).build();
    }
}
