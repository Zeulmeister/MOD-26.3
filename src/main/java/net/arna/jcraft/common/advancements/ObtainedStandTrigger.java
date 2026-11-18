package net.arna.jcraft.common.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.stand.StandType;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ObtainedStandTrigger extends SimpleCriterionTrigger<ObtainedStandTrigger.TriggerInstance> {
    public static final Identifier ID = JCraft.id("obtained_stand");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(final ServerPlayer player, final @NotNull StandType type) {
        Identifier id = JRegistries.STAND_TYPE_REGISTRY.getId(type);
        this.trigger(player, inst -> inst.matches(id));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Identifier> stand) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Identifier.CODEC.optionalFieldOf("stand").forGetter(TriggerInstance::stand)
        ).apply(instance, TriggerInstance::new));

        public static TriggerInstance obtainedStand(final @Nullable StandType type) {
            return new TriggerInstance(Optional.empty(), Optional.ofNullable(type).map(JRegistries.STAND_TYPE_REGISTRY::getId));
        }

        public static TriggerInstance obtainedStand() {
            return new TriggerInstance(Optional.empty(), Optional.empty());
        }

        public boolean matches(final @Nullable Identifier standId) {
            return stand.isEmpty() || stand.get().equals(standId);
        }
    }
}
