package net.arna.jcraft.common.effects;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.tickable.JEnemies;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

public class DazedStatusEffect extends MobEffect {
    private static final Identifier SLOW_ID = JCraft.id("dazed_movement_slow");
    private static final Identifier ATTACK_DAMAGE_ID = JCraft.id("dazed_attack_damage");
    private static final Identifier ATTACK_SPEED_ID = JCraft.id("dazed_attack_speed");
    private static final Identifier FLYING_SPEED_ID = JCraft.id("dazed_flying_speed");

    public DazedStatusEffect() {
        super(MobEffectCategory.NEUTRAL, 0x444444);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
            .addAttributeModifier(Attributes.ATTACK_SPEED, ATTACK_SPEED_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, SLOW_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
            .addAttributeModifier(Attributes.FLYING_SPEED, FLYING_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int duration, final int amplifier) {
        return true;
    }

    public static boolean canBeComboBroken(final int amplifier) {
        return switch (amplifier) {
            case 1, 3, 4 -> true;
            default -> false;
        };
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity entity, final int amplifier) {
        Vec3 eVel = entity.getDeltaMovement();
        double yVel = eVel.y;
        double horizontalMult = 0.4;

        if (amplifier < 2) {
            yVel = Mth.clamp(yVel, -0.5, 0.5);
            horizontalMult = 0.8;
        } else if (amplifier == 3) {
            horizontalMult = 1;
        }

        entity.setDeltaMovement(eVel.x * horizontalMult, yVel, eVel.z * horizontalMult);

        if (entity instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }

        if (amplifier == 2) {
            return true;
        }
        if (entity instanceof Mob mob) {
            if (JEnemies.contains(mob)) return true;
            mob.setTarget(null);
            mob.setAggressive(false);
        }
        return true;
    }

    @Override
    public void createModifiers(final int amplifier, final BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        consumer.accept(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_ID, -1.0 * (amplifier + 1), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        consumer.accept(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        double slowAmount = switch (amplifier) {
            case 3, 1, 0 -> -1.0;
            case 4 -> -0.25;
            default -> 0.0;
        };
        consumer.accept(Attributes.MOVEMENT_SPEED, new AttributeModifier(SLOW_ID, slowAmount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        consumer.accept(Attributes.FLYING_SPEED, new AttributeModifier(FLYING_SPEED_ID, 0.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
