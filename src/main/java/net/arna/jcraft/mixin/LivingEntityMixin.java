package net.arna.jcraft.mixin;

import lombok.NonNull;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.MoveUsage;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.arna.jcraft.common.network.s2c.IPSTriggeredPacket;
import net.arna.jcraft.common.util.IJCraftComboTracker;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.LivingEntityMixinLogic;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IJCraftComboTracker {

    @Unique
    private float jcraft$damageScaling = 1.0f;

    @Unique
    private int jcraft$hitCount = 0;

    @Unique
    private final Map<LivingEntity, Set<MoveUsage>> jcraft$usedComboMoves = new WeakHashMap<>();

    @Override
    public float jcraft$getDamageScaling() {
        return jcraft$damageScaling;
    }

    @Override
    public int jcraft$getHitCount() {
        return jcraft$hitCount;
    }

    @Override
    public void jcraft$increaseHitCount(boolean tsHit) {
        jcraft$hitCount++;
        float penalty = JServerConfig.SCALING_PENALTY_PER_HIT.getValue();
        if (tsHit) {
            penalty *= 0.5f;
        }
        jcraft$damageScaling = Math.max(
                JServerConfig.DAMAGE_SCALING_MINIMUM.getValue(),
                jcraft$damageScaling - penalty
        );
    }

    @Override
    public boolean jcraft$addMoveToCombo(@NonNull LivingEntity attacker, MoveUsage moveUsage) {
        if (!jcraft$usedComboMoves.isEmpty()) {
            jcraft$usedComboMoves.keySet().removeIf(k -> k == null || !k.isAlive() || k.isRemoved());
        }
        AbstractMove<?, ?> move = moveUsage.move();
        Set<MoveUsage> set = jcraft$usedComboMoves.get(attacker);
        if (set != null) {
            for (MoveUsage prev : set) {
                if (moveUsage != prev && Attacks.prototypeMatch(prev.move(), move)) {
                    LivingEntity user = JUtils.getUserIfStand(attacker);
                    if (user instanceof ServerPlayer serverPlayer) {
                        IPSTriggeredPacket.send(serverPlayer);
                    }
                    return true;
                }
            }
            if (move.isLoopPrevention()) {
                set.add(moveUsage);
            }
            return false;
        }

        Set<MoveUsage> newSet = new HashSet<>(2);
        if (move.isLoopPrevention()) {
            newSet.add(moveUsage);
            jcraft$usedComboMoves.put(attacker, newSet);
        }
        return false;
    }

    @Override
    public boolean jcraft$comboFromAttackerContains(LivingEntity attacker, AbstractMove<?, ?> move) {
        if (attacker == null || !attacker.isAlive() || attacker.isRemoved()) {
            return false;
        }
        Set<MoveUsage> set = jcraft$usedComboMoves.get(attacker);
        if (set == null) {
            return false;
        }
        for (MoveUsage prev : set) {
            if (Attacks.prototypeMatch(prev.move(), move)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void jcraft$resetCombo() {
        jcraft$usedComboMoves.clear();
        jcraft$hitCount = 0;
        jcraft$damageScaling = 1.0f;
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void jcraft$handleCounterOnHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (AbstractCounterAttack.handleCounter(self, source, amount)) {
            ci.cancel();
        }
    }

    @Inject(method = "canStandOnFluid", at = @At("RETURN"), cancellable = true)
    private void jcraft$walkOnLiquid(FluidState fluidState, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            LivingEntity self = (LivingEntity) (Object) this;
            if (LivingEntityMixinLogic.canWalkOnLiquid(self.level(), self)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "canBeAffected", at = @At("RETURN"), cancellable = true)
    private void jcraft$dontApplyPotionsToTE(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && JUtils.inTimeErase((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void jcraft$preventJumpWhenStunnedOrFrozen(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!JUtils.canJump(self) || JUtils.isAffectedByTimeStop(self)) {
            ci.cancel();
        }
    }

    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void jcraft$notPushableIfCreaming(CallbackInfoReturnable<Boolean> cir) {
        if (CreamEntity.isCreaming((LivingEntity) (Object) this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void jcraft$stopBreathingIfCreamingOrVampire(int currentAir, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (CreamEntity.isCreaming(self) || JComponentPlatformUtils.getVampirism(self).isVampire()) {
            cir.setReturnValue(currentAir);
        }
    }

    @Inject(
            method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jcraft$noMinionLoot(ServerLevel level, DamageSource source, boolean playerKilled, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (JComponentPlatformUtils.getMiscData(self).getMaster() != null) {
            ci.cancel();
        }
    }
}
