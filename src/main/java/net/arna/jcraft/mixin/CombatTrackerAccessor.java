package net.arna.jcraft.mixin;

import net.minecraft.world.damagesource.CombatTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CombatTracker.class)
public interface CombatTrackerAccessor {
    @Accessor("inCombat")
    boolean jcraft$isInCombat();

    @Accessor("takingDamage")
    boolean jcraft$isTakingDamage();
}
