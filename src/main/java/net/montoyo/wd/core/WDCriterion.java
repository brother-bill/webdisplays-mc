/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

public class WDCriterion extends SimpleCriterionTrigger<WDCriterion.TriggerInstance> {

    private final String id;

    public WDCriterion(@Nonnull String name) {
        id = name;
    }

    public String getId() {
        return id;
    }

    @Override
    public @NotNull Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, instance -> true);
    }

    public static class TriggerInstance implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player)
                ).apply(instance, TriggerInstance::new)
        );

        private final Optional<ContextAwarePredicate> player;

        public TriggerInstance(Optional<ContextAwarePredicate> player) {
            this.player = player;
        }

        public static TriggerInstance simple() {
            return new TriggerInstance(Optional.empty());
        }

        @Override
        public Optional<ContextAwarePredicate> player() {
            return player;
        }
    }
}
