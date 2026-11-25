package net.arna.jcraft.common.entity.projectile;

import lombok.NonNull;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static net.arna.jcraft.api.Attacks.damageLogic;

public class RedBindEntity extends JAttackEntity {
    private LivingEntity boundEntity;
    private float boundHealth;
    private boolean hasExploded = false;
    public static final int LIFE_TIME = 60;
    private int timeLeft = LIFE_TIME;
    private static final EntityDataAccessor<Float> WIDTH;

    static {
        WIDTH = SynchedEntityData.defineId(RedBindEntity.class, EntityDataSerializers.FLOAT);
    }

    public float getBoundWidth() {
        return entityData.get(WIDTH);
    }

    public void setBoundEntity(@NonNull LivingEntity boundEntity) {
        this.boundEntity = boundEntity;
        this.boundHealth = boundEntity.getHealth();
        this.entityData.set(WIDTH, (float) boundEntity.getBoundingBox().getSize());
        this.startRiding(boundEntity, true, false);
        boundEntity.addEffect(new MobEffectInstance(JStatusRegistry.STANDLESS.asHolder(), timeLeft, 0, true, false));
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WIDTH, 1f);
    }

    public RedBindEntity(Level world) {
        super(JEntityTypeRegistry.RED_BIND.get(), world);
    }

    public double getPassengersRidingOffset() {
        return -1.0;
    }

    @Override
    public void tick() {
        if (level().isClientSide()) {
            if (isAlive()) IDLE.sendForEntity(this);
        } else {
            if (boundEntity == null) { // If boundEntity data was wiped, attempt to recover
                if (getVehicle() instanceof LivingEntity living) {
                    setBoundEntity(living);
                }
            } else if (!isPassenger() && !hasExploded) { // If detached
                detonate();
            }

            if (boundEntity == null) {
                discard();
            } else if (!hasExploded && (--timeLeft <= 0 || boundEntity.getHealth() < boundHealth)) {
                detonate();
            }

            // In practice, redbind lasts slightly longer than the duration, so to account for this,
            // we add two ticks of standless until we're actually done.
            if (boundEntity != null && boundEntity.getEffect(JStatusRegistry.STANDLESS.asHolder()) == null) {
                boundEntity.addEffect(new MobEffectInstance(JStatusRegistry.STANDLESS.asHolder(), 2, 0, true, false));
            }
        }

        super.tick();
    }

    private void detonate() {
        if (master != null) {
            final Vec3 vel = boundEntity.position().add(0, 0.5, 0).subtract(master.position());
            final Vec3 launch = vel.normalize().scale(1.25);
            damageLogic(boundEntity.level(), boundEntity, new net.arna.jcraft.api.AttackData(
                    launch, 20, 3, true, 6, false, 4,
                    level().damageSources().mobAttack(master), master,
                    CommonHitPropertyComponent.HitAnimation.MID, null, false, true));
        }

        EXPLODE.sendForEntity(this);
        hasExploded = true;
        if (level() instanceof net.minecraft.server.level.ServerLevel _sl) kill(_sl); else discard();
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(@NonNull DamageSource source) {
        return SoundEvents.LAVA_EXTINGUISH;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.LAVA_EXTINGUISH;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

        public void addAdditionalSaveData(@NonNull CompoundTag tag) {
        writeMasterNbt(tag);
    }

        public void readAdditionalSaveData(@NonNull CompoundTag tag) {
        readMasterNbt(tag);
    }

    private static final AzCommand IDLE = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.red_bind.idle", AzPlayBehaviors.LOOP);
    private static final AzCommand EXPLODE = AzCommand.create(JCraft.BASE_CONTROLLER, "animation.red_bind.explode");

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        readAdditionalSaveData(net.arna.jcraft.common.util.JValueIO.read(input, "JCraftEntityData"));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        net.minecraft.nbt.CompoundTag jcraftTag = new net.minecraft.nbt.CompoundTag();
        addAdditionalSaveData(jcraftTag);
        net.arna.jcraft.common.util.JValueIO.write(output, "JCraftEntityData", jcraftTag);
    }
}
