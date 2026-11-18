package net.arna.jcraft.common.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.spec.SpecType;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ObtainedSpecTrigger extends SimpleCriterionTrigger<ObtainedSpecTrigger.TriggerInstance> {
    public static final Identifier ID = JCraft.id("obtained_spec");

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(final ServerPlayer player, final @NotNull SpecType type) {
        Identifier id = JRegistries.SPEC_TYPE_REGISTRY.getId(type);
        this.trigger(player, inst -> inst.matches(id));
    }

    public record TriggerInstance(Optional<Holder<LootItemCondition>> player, Optional<Identifier> spec) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                Identifier.CODEC.optionalFieldOf("spec").forGetter(TriggerInstance::spec)
        ).apply(instance, TriggerInstance::new));

        public static TriggerInstance obtainedSpec(final @Nullable SpecType type) {
            return new TriggerInstance(Optional.empty(), Optional.ofNullable(type).map(JRegistries.SPEC_TYPE_REGISTRY::getId));
        }

        public static TriggerInstance obtainedSpec() {
            return new TriggerInstance(Optional.empty(), Optional.empty());
        }

        public boolean matches(final @Nullable Identifier specId) {
            return spec.isEmpty() || spec.get().equals(specId);
        }
    }
}
