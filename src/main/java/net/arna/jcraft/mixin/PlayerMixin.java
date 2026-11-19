package net.arna.jcraft.mixin;

import com.mojang.authlib.GameProfile;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.common.attack.moves.hamon.ImproviserAttack;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.food.IFoodData;
import net.arna.jcraft.common.network.s2c.ComboCounterPacket;
import net.arna.jcraft.common.spec.HamonSpec;
import net.arna.jcraft.common.util.IComboCounter;
import net.arna.jcraft.common.util.IOwnable;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.mixin_logic.AbilitiesAddon;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.ref.WeakReference;

@Mixin(Player.class)
public abstract class PlayerMixin implements IComboCounter, IFoodData {

    @Shadow
    protected FoodData foodData;

    @Shadow
    @Final
    private Abilities abilities;

    @Unique
    private int jcraft$comboCount = 1;

    @Unique
    private WeakReference<LivingEntity> jcraft$lastAttacked = new WeakReference<>(null);

    @Override
    public LivingEntity jcraft$getLastAttacked() {
        LivingEntity target = jcraft$lastAttacked.get();
        if (target != null && (!target.isAlive() || target.isRemoved())) {
            jcraft$lastAttacked.clear();
            return null;
        }
        return target;
    }

    @Override
    public void jcraft$setLastAttacked(LivingEntity target) {
        this.jcraft$lastAttacked = new WeakReference<>(target);
    }

    @Override
    public int jcraft$getComboCount() {
        return jcraft$comboCount;
    }

    @Override
    public void jcraft$setComboCount(int count) {
        this.jcraft$comboCount = count;
    }

    @Override
    public void jcraft$incrementComboCount() {
        this.jcraft$comboCount++;
    }

    @Override
    public @Nullable FoodData getFoodData() {
        return this.foodData;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void jcraft$setAbilitiesPlayer(Level level, GameProfile profile, CallbackInfo ci) {
        ((AbilitiesAddon) this.abilities).jcraft$setPlayer((Player) (Object) this);
    }

    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void jcraft$preventEatingWhenDazed(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (self.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void jcraft$playerTickTail(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (JUtils.isAffectedByTimeStop(self)) {
            return;
        }

        JSpec<?, ?> spec = JComponentPlatformUtils.getSpecData(self).getSpec();
        if (spec != null) {
            spec.tickSpec();
        }

        LivingEntity lastAttacked = jcraft$lastAttacked.get();
        if (lastAttacked == null) {
            return;
        }
        if (!lastAttacked.isAlive() || lastAttacked.isRemoved() || lastAttacked.level() != self.level()) {
            jcraft$lastAttacked.clear();
            if (jcraft$comboCount != 0) {
                jcraft$comboCount = 0;
                if (self instanceof ServerPlayer serverPlayer) {
                    ComboCounterPacket.send(serverPlayer, 0, 1.0f);
                }
            }
            return;
        }

        LivingEntity lastHurtBy = lastAttacked.getLastHurtByMob();
        if (lastHurtBy != null) {
            if (lastHurtBy == self) {
                if (lastAttacked.hasEffect(JStatusRegistry.DAZED.asHolder())) {
                    return;
                }
            } else if (lastHurtBy instanceof IOwnable ownable && ownable.getMaster() == self) {
                if (lastAttacked.hasEffect(JStatusRegistry.DAZED.asHolder())) {
                    return;
                }
            }
        }

        if (!lastAttacked.hasEffect(JStatusRegistry.DAZED.asHolder())) {
            jcraft$lastAttacked.clear();
            jcraft$comboCount = 0;
            if (self instanceof ServerPlayer serverPlayer) {
                ComboCounterPacket.send(serverPlayer, 0, 1.0f);
            }
        }
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void jcraft$guardVanillaAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (JUtils.isAffectedByTimeStop(self)) {
            ci.cancel();
            return;
        }
        if (self instanceof ServerPlayer) {
            JSpec<?, ?> spec = JUtils.getSpec(self);
            if (spec instanceof HamonSpec hamon && hamon.getCurrentMove() instanceof ImproviserAttack) {
                ci.cancel();
                return;
            }
        }
        if (JUtils.getStand(self) != null) {
            ItemStack mainHand = self.getMainHandItem();
            var modifiers = mainHand.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (mainHand.isEmpty() || modifiers == null || modifiers.modifiers().isEmpty()) {
                ci.cancel();
                return;
            }
        }
        JSpec<?, ?> spec = JComponentPlatformUtils.getSpecData(self).getSpec();
        if (spec != null && spec.moveStun > 0) {
            ci.cancel();
        }
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void jcraft$handlePlayerCounter(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (AbstractCounterAttack.handleCounter(self, source, amount)) {
            ci.cancel();
        }
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    private void jcraft$disableCombatElytra(CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player) (Object) this;
        if (JServerConfig.DISABLE_COMBAT_ELYTRA.getValue()
                && JComponentPlatformUtils.getMiscData(self).isOnDamageTimer()) {
            cir.setReturnValue(false);
        }
    }
}
