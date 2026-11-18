package net.arna.jcraft.common.attack.moves.hamon;

import com.google.common.collect.Multimap;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;
import net.arna.jcraft.common.spec.HamonSpec;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Collection;
import java.util.Set;

public final class ImproviserAttack extends AbstractSimpleAttack<ImproviserAttack, HamonSpec> {
    public static final float CHARGE_COST = 6.0F;
    public ImproviserAttack(int cooldown, int windup, int duration, float moveDistance, float damage, int stun,
                            float hitboxSize, float knockback, float offset) {
        super(cooldown, windup, duration, moveDistance, damage, stun, hitboxSize, knockback, offset);
    }

    @Override
    public @NonNull MoveType<ImproviserAttack> getMoveType() {
        return ImproviserAttack.Type.INSTANCE;
    }

    @Override
    public boolean conditionsMet(final HamonSpec attacker) {
        if (!super.conditionsMet(attacker)) {
            return false;
        }
        return (attacker.getUser().getItemInHand(InteractionHand.MAIN_HAND).has(DataComponents.TOOL) || attacker.getUser().getItemInHand(InteractionHand.MAIN_HAND).has(DataComponents.WEAPON));
    }

    @Override
    public void onInitiate(HamonSpec attacker) {
        super.onInitiate(attacker);

        attacker.drainCharge(CHARGE_COST);
        attacker.setUseHamonNext(false);
    }

    @Override
    public void tick(HamonSpec attacker) {
        super.tick(attacker);
    }

    @Override
    public @NonNull Set<LivingEntity> perform(HamonSpec attacker, LivingEntity user) {
        final ItemStack mainStack = user.getMainHandItem();
        final java.util.List<AttributeModifier> dmgMods = new java.util.ArrayList<>();
        final java.util.List<AttributeModifier> kbMods = new java.util.ArrayList<>();
        mainStack.forEachModifier(EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.equals(Attributes.ATTACK_DAMAGE)) dmgMods.add(mod);
            else if (attr.equals(Attributes.ATTACK_KNOCKBACK)) kbMods.add(mod);
        });
        final float damage = (float) applyAttributeModifiers(1.0, dmgMods);
        withDamage(damage);
        final float knockback = (float) applyAttributeModifiers(1.25, kbMods);
        withKnockback(knockback);

        final Set<LivingEntity> targets = super.perform(attacker, user);

        if (JUtils.getSpec(user) instanceof HamonSpec hamonSpec) {
            for (LivingEntity target : targets) {
                hamonSpec.processTarget(target);
                if (user.level() instanceof net.minecraft.server.level.ServerLevel sl) { EnchantmentHelper.doPostAttackEffects(sl, target, attacker.getDamageSource()); }

                final int fireLevel = user.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(Enchantments.FIRE_ASPECT).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, mainStack)).orElse(0);
                if (fireLevel > 0) target.igniteForSeconds(fireLevel * 4);
            }
        }

        return targets;
    }

    public static double applyAttributeModifiers(double baseValue, final Collection<AttributeModifier> modifiers) {
        double add = 0;
        double multBase = 0;
        double multTotal = 1;

        for (AttributeModifier mod : modifiers) {
            final double amt = mod.amount();
            switch (mod.operation()) {
                case ADD_VALUE -> add += amt;
                case ADD_MULTIPLIED_BASE -> multBase += amt;
                case ADD_MULTIPLIED_TOTAL -> multTotal *= 1 + amt;
            }
        }

        return (baseValue + add) * (1 + multBase) * multTotal;
    }

    @Override
    protected @NonNull ImproviserAttack getThis() {
        return this;
    }

    @Override
    public @NonNull ImproviserAttack copy() {
        return copyExtras(new ImproviserAttack(getCooldown(), getWindup(), getDuration(), getMoveDistance(), getDamage(),
                getStun(), getHitboxSize(), getKnockback(), getOffset()));
    }

    public static class Type extends AbstractSimpleAttack.Type<ImproviserAttack> {
        public static final ImproviserAttack.Type INSTANCE = new ImproviserAttack.Type();

        @Override
        protected @NonNull App<RecordCodecBuilder.Mu<ImproviserAttack>, ImproviserAttack> buildCodec(RecordCodecBuilder.Instance<ImproviserAttack> instance) {
            return attackDefault(instance, ImproviserAttack::new);
        }
    }
}
