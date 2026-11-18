package net.arna.jcraft.common.effects;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.function.BiConsumer;

public class HypoxiaEffect extends MobEffect {
    private static final Identifier DMG_ID = JCraft.id("hypoxia_attack_damage");
    private static final Identifier SPD_ID = JCraft.id("hypoxia_movement_speed");
    private static final Identifier ASPD_ID = JCraft.id("hypoxia_attack_speed");

    public HypoxiaEffect() {
        super(MobEffectCategory.HARMFUL, 0x858c30);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(final @NonNull ServerLevel level, final @NonNull LivingEntity livingEntity, final int amplifier) {
        if (amplifier < 2) return true;
        final int airSupply = livingEntity.getAirSupply();
        if (airSupply <= 0) {
            livingEntity.hurtServer(level, livingEntity.damageSources().drown(), 2.0F);
        }
        livingEntity.setAirSupply(airSupply - 20);
        return true;
    }

    @Override
    public void createModifiers(final int amplifier, final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        if (amplifier >= 0) {
            consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(DMG_ID, -0.2 * (amplifier + 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (amplifier >= 1) {
            int mult = Math.min(amplifier, 2);
            consumer.accept(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPD_ID, -0.2 * (mult + 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if (amplifier >= 2) {
            consumer.accept(Attributes.ATTACK_SPEED, new AttributeModifier(ASPD_ID, -0.2 * (amplifier + 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
