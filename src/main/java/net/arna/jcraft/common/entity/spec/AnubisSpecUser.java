package net.arna.jcraft.common.entity.spec;

import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.api.spec.SpecUserMob;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class AnubisSpecUser extends SpecUserMob {
    private int outOfCombatTicks = 0;

    public AnubisSpecUser(Level level) {
        super(JEntityTypeRegistry.ANUBIS_SPEC_USER.get(), level);
        setSpecType(JSpecTypeRegistry.ANUBIS.get());

        setItemInHand(InteractionHand.MAIN_HAND, JItemRegistry.ANUBIS.get().getDefaultInstance());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (getTarget() != null && getTarget().isAlive()) {
            outOfCombatTicks = 0;
        } else if (getMainHandItem().is(JItemRegistry.ANUBIS.get())
                && (getSpec() == null || getSpec().getMoveStun() <= 0)) {
            if (++outOfCombatTicks >= 100) {
                setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JItemRegistry.ANUBIS_SHEATHED.get()));
                playSound(JSoundRegistry.ANUBIS_UNSHEATHE.get(), 0.7F, 0.85F);
                outOfCombatTicks = 0;
            }
        }
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();

        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, IronGolem.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Villager.class, true));
    }

    public static AttributeSupplier.Builder createUserAttributes() {
        return createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.4);
    }
}
