package net.arna.jcraft.common.entity.projectile;
import net.arna.jcraft.common.util.JItemData;
import net.minecraft.world.entity.EntityTypes;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonBombTrackerComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.common.entity.stand.AbstractKillerQueenEntity;
import net.arna.jcraft.common.entity.stand.WhiteSnakeEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.PotionContents;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemTossProjectile extends AbstractArrow {

    protected static final EntityDataAccessor<ItemStack> ITEM;

    protected static final EntityDataAccessor<Integer> RICOCHETS;

    static {
        ITEM = SynchedEntityData.defineId(ItemTossProjectile.class, EntityDataSerializers.ITEM_STACK);
        RICOCHETS = SynchedEntityData.defineId(ItemTossProjectile.class, EntityDataSerializers.INT);
    }

    private static final float SPIN_DEGREES_PER_VELOCITY = 40f;

    @Getter
    private float prevSpinAngle = 0f;
    @Getter
    private float spinAngle = 0f;

    public ItemTossProjectile(final Level level) {
        super(JEntityTypeRegistry.ITEM_TOSS_PROJECTILE.get(), level);
        setItem(ItemStack.EMPTY);
    }

    public ItemTossProjectile(final LivingEntity shooter, final Level level, final ItemStack item) {
        super(JEntityTypeRegistry.ITEM_TOSS_PROJECTILE.get(), shooter, level, item.copyWithCount(1), null);
        setCritArrow(true); // leverages vanilla's built-in crit particle spawning each tick for a free particle trail
        setItem(item);
        if (getItem().is(JTagRegistry.HEAVY_IMPACT)) {
            this.setBaseDamage(2d);
            // heavy impact knockback handled in calculateKnockback()
        }
        else {
            this.setBaseDamage(0d);
            // standard impact knockback
        }
    }

    @Override
    public void tick() {
        prevSpinAngle = spinAngle;
        if (!isInGround()) {
            spinAngle += (float) getDeltaMovement().length() * SPIN_DEGREES_PER_VELOCITY;
        }
        super.tick();
    }

    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    protected void setItem(@NotNull ItemStack item) {
        this.entityData.set(ITEM, item.copyWithCount(1));
    }

    public int getRicochets() {
        return this.entityData.get(RICOCHETS);
    }

    protected void incrementRicochets() {
        this.entityData.set(RICOCHETS, getRicochets() + 1);
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        return getItem();
    }

    @Override
    protected @NotNull ItemStack getDefaultPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ITEM, ItemStack.EMPTY);
        builder.define(RICOCHETS, 0);
    }

    @Override
    public boolean isOnFire() {
        // this might be better to move to doPostHurtEffects
        return super.isOnFire() || getItem().is(JTagRegistry.BURNS_ON_IMPACT);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        if (getItem().is(JTagRegistry.BLINDS_ON_IMPACT)) {
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40));
        }
        if (getItem().is(JTagRegistry.SLOWS_ON_IMPACT)) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60));
        }
        if (getItem().is(JTagRegistry.POISONS_ON_IMPACT)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 60));
        }
    }

    protected boolean maybeExplode() {
        if (!level().isClientSide() && getItem().is(JTagRegistry.EXPLODES_ON_IMPACT)) {
            final boolean grief = ((net.minecraft.server.level.ServerLevel) level()).getGameRules().get(GameRules.MOB_GRIEFING);
            final boolean standGrief = !(getOwner() instanceof StandEntity<?,?>) || ((net.minecraft.server.level.ServerLevel) level()).getGameRules().get(JCraft.STAND_GRIEFING);
            level().explode(this, getX(), getY(), getZ(), 1, grief && standGrief, Level.ExplosionInteraction.MOB);
            discard();
            return true;
        }
        return false;
    }

    @Override
    protected void onHit(HitResult result) {
        final HitResult.Type type = result.getType();
        if (type == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult)result);
        } else if (type == HitResult.Type.BLOCK) {
            BlockHitResult blockHitResult = (BlockHitResult)result;
            this.onHitBlock(blockHitResult);
        }
    }

    @Override
    protected void onHitEntity(final @NonNull EntityHitResult result) {
        if (level().isClientSide()) {
            return;
        }

        // this part has been heavily inspired by AbstractArrow
        Entity entity = result.getEntity();
        Entity entity2 = JUtils.getUserIfStand(this.getOwner());
        DamageSource damageSource;
        if (entity2 == null) {
            damageSource = this.damageSources().arrow(this, this);
        } else {
            damageSource = this.damageSources().arrow(this, entity2);
            if (entity2 instanceof LivingEntity) {
                ((LivingEntity)entity2).setLastHurtMob(entity);
            }
        }

        boolean targetIsEnderman = entity.getType() == EntityTypes.ENDERMAN;

        if (this.isOnFire() && !targetIsEnderman) {
            entity.igniteForSeconds(5);
        }

        entity.hurt(damageSource, calculateDamage(entity));

        if (targetIsEnderman) {
            return;
        }

        final int fireSeconds = level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(Enchantments.FIRE_ASPECT).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, getItem())).orElse(0) * 4;
        if (fireSeconds > 0) {
            entity.igniteForSeconds(fireSeconds);
        }

        boolean effectActivated = false;

        if (entity instanceof final LivingEntity livingEntity) {
            // handle knockback
            float knockback = calculateKnockback();
            if (knockback > 0) {
                double d = Math.max(0, 1f - livingEntity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                Vec3 vec3 = this.getDeltaMovement().multiply(1f, 0f, 1f).normalize().scale(knockback * 0.6 * d);
                if (vec3.lengthSqr() > 0) {
                    livingEntity.push(vec3.x, 0.1, vec3.z);
                }
            }
            if (getOwner() instanceof LivingEntity owner) {
                if (level() instanceof ServerLevel sl) { EnchantmentHelper.doPostAttackEffects(sl, livingEntity, damageSource); }
            }
            this.doPostHurtEffects(livingEntity);
//            if (entity2 != null && livingEntity != entity2 && livingEntity instanceof Player && entity2 instanceof ServerPlayer && !this.isSilent()) {
//                ((ServerPlayer)entity2).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
//            }
        }

        if (maybeExplode()) {
            return;
        }

        // transfer KQ bomb to hit entity
        if (getOwner() instanceof AbstractKillerQueenEntity<?, ?> kq && kq.hasUser()) {
            final CommonBombTrackerComponent.BombData bombData = JComponentPlatformUtils.getBombTracker(kq.getUserOrThrow()).getMainBomb();
            if (bombData.isEntity && bombData.bombEntityID == this.getId()) {
                bombData.setBomb(entity);
                this.discard();
                return;
            }
        }

        // force stand on target — only WhiteSnake can throw discs, and can do so into any living entity
        if (entity instanceof LivingEntity livingEntity && getOwner() instanceof WhiteSnakeEntity
                && getItem().is(JItemRegistry.STAND_DISC.get())) {
            // get NBT
            StandType itemStand = null;
            int itemSkin = 0;
            final CompoundTag data = JItemData.getOrCreateTag(getItem());
            if (data.contains("StandID")) {
                itemStand = StandTypeUtil.readFromNBT(data, "StandID");
            }
            if (data.contains("Skin")) {
                itemSkin = data.getIntOr("Skin", 0);
            }
            // apply stand
            if (itemStand != null && !JCraft.getExclusiveStandsData().isStandUsed(itemStand) && (
                    (livingEntity instanceof Player && !StandTypeUtil.isIn(itemStand, JTagRegistry.PLAYER_STAND_BLACKLIST, level().registryAccess())) ||
                    (!(livingEntity instanceof Player) && !StandTypeUtil.isIn(itemStand, JTagRegistry.MOB_STAND_BLACKLIST, level().registryAccess())))
            ) {
                final CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(livingEntity);
                if (standData.getType() == null) { // don't override current stand
                    standData.setTypeAndSkin(itemStand, itemSkin, false);
                    JCraft.summon(level(), livingEntity);
                }
                effectActivated = true;
            }
        }
        // force equip thrown stuff
        if (entity instanceof Mob mob && getItem().is(JTagRegistry.EQUIPABLES)) {
            if (level() instanceof ServerLevel sl) { mob.equipItemIfPossible(sl, getItem()); }
            effectActivated = true;
        }

        // TODO spec obtainment items

        // force feed
        if (entity instanceof LivingEntity livingEntity && getItem().has(DataComponents.FOOD)) {
            if (!JComponentPlatformUtils.getVampirism(livingEntity).isVampire()) {
                boolean canEat = true;

                if (livingEntity instanceof Player player)
                    canEat = player.getFoodData().needsFood();

                if (canEat) {
                    getItem().finishUsingItem(level(), livingEntity);

                    if (getItem().has(DataComponents.USE_REMAINDER)) {
                        setItem(Items.BOWL.getDefaultInstance());
                        dropItem(result.getLocation()); // FIXME doesn't work?
                    }

                    effectActivated = true;
                }
            }
        }

        // force drink potions
        if (entity instanceof LivingEntity living && getItem().getItem() instanceof PotionItem) {
            for (MobEffectInstance mobEffectInstance : getItem().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects()) {
                if (mobEffectInstance.getEffect().value().isInstantaneous()) {
                    if (level() instanceof ServerLevel sl) { mobEffectInstance.getEffect().value().applyInstantaneousEffect(sl, getOwner(), getOwner(), living, mobEffectInstance.getAmplifier(), 1d); }
                } else {
                    living.addEffect(new MobEffectInstance(mobEffectInstance));
                }
            }
            effectActivated = true;
        }

        // slab iron on iron golem
        if (entity instanceof IronGolem golem && getItem().is(Items.IRON_INGOT)) {
            float prevHealHealth = golem.getHealth();
            golem.heal(25.0F);
            if (golem.getHealth() != prevHealHealth) {
                float g = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
                this.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, g);
            }
            effectActivated = true;
        }

        // force equip saddles
        if (entity instanceof Mob saddleMob && getItem().is(Items.SADDLE) && saddleMob.canUseSlot(EquipmentSlot.SADDLE) && saddleMob.getItemBySlot(EquipmentSlot.SADDLE).isEmpty()) {
            saddleMob.setItemSlot(EquipmentSlot.SADDLE, getItem().copyWithCount(1));
            float g = 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F;
            this.playSound(SoundEvents.HORSE_SADDLE.value(), 1.0F, g);
            effectActivated = true;
        }

        // force equip horse armor
        if (entity instanceof AbstractHorse horse && horse.canUseSlot(EquipmentSlot.BODY) && horse.getItemBySlot(EquipmentSlot.BODY).isEmpty() && getItem().has(DataComponents.EQUIPPABLE)) {
            horse.setItemSlot(EquipmentSlot.BODY, getItem().copyWithCount(1));
            effectActivated = true;
        }

        // TODO also, what about throwing a sword, bone meal, fire charge, shears,

        // end of inspired part
        if (!this.level().isClientSide() && getItem().is(JTagRegistry.EXPLODES_ON_IMPACT)) {
            entity.hurt(this.damageSources().arrow(this, entity2), 6f);
        }
        // a little bit more inspiration
        if (this.getPierceLevel() <= 0) {
            // drop if not used
            if (!(effectActivated || getItem().is(JTagRegistry.BLINDS_ON_IMPACT) ||
                    getItem().is(JTagRegistry.BURNS_ON_IMPACT) ||
                    getItem().is(JTagRegistry.HEAVY_IMPACT) ||
                    getItem().is(JTagRegistry.EXPLODES_ON_IMPACT) ||
                    getItem().is(JTagRegistry.SLOWS_ON_IMPACT) ||
                    getItem().is(JTagRegistry.POISONS_ON_IMPACT))) {
                // drop at owner position so the item doesn't land on the target and get picked up
                final Vec3 dropTarget = entity2 != null ? entity2.position() : result.getLocation();
                dropItem(dropTarget);
            }
            this.discard();
        }
    }

    private float calculateDamage(final Entity target) {
        float damage = (float)this.baseDamage;
        if (!(target instanceof final LivingEntity livingTarget)
                || !(JUtils.getUserIfStand(getOwner()) instanceof final LivingEntity owner)
                /*|| !(getItem().getItem() instanceof final TieredItem tieredItem)*/) {
            return damage;
        }
        damage = (float)owner.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (level() instanceof ServerLevel sl) { damage = EnchantmentHelper.modifyDamage(sl, getItem(), livingTarget, this.damageSources().arrow(this, owner), damage); }
        return damage;
    }

    private float calculateKnockback() {
        int knockbackLevel = level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(Enchantments.KNOCKBACK).map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, getItem())).orElse(0);
        return (getItem().is(JTagRegistry.HEAVY_IMPACT) ? 4f : 0f) + 0.5f * knockbackLevel;
    }

    @Override
    protected void onHitBlock(final BlockHitResult result) {
        final BlockState blockstate = level().getBlockState(result.getBlockPos());
        // notify the block it has been hit with something
        blockstate.onProjectileHit(level(), blockstate, result, this);

        final BlockState hitBlock = level().getBlockState(result.getBlockPos());
        double hardness = hitBlock.getBlock().defaultDestroyTime();
        if (hardness < 0d) { // unbreakable
            hardness = Double.MAX_VALUE;
        }

        if (maybeExplode()) {
            return;
        }

        // ricochet
        incrementRicochets();
        if (getRicochets() <= maxRicochets()) {
            // calculate penetrative value and decide if it should land
            final Vec3i intNormal = result.getDirection().getUnitVec3i();
            final Vec3 normal = new Vec3(intNormal.getX(), intNormal.getY(), intNormal.getZ());
            final Vec3 impactVec = getDeltaMovement();

            // a*b = |a|*|b|*cos(φ) , a*b = a.dotProduct(b)
            final double impactAngleRad = Math.acos(normal.dot(impactVec.normalize())) - Math.PI / 2.0;
            final double impactAngleDeg = Math.toDegrees(impactAngleRad);

            // Ek = mv^2/2
            final double kineticEnergy = mass() * impactVec.lengthSqr() / 2;
            final double penAngle = penetrationAngle() + hardness * 5; // This is bs but so is minecraft physics

            final boolean lowEnergy = kineticEnergy < 0.001;
            if (impactAngleDeg <= penAngle && !lowEnergy) { // If penetrated or ran out of energy
                setDeltaMovement(impactVec.add(normal).scale(0.5 / hardness));
                // don't hit the ground
                return;
            }
        }

        // hit the ground
        final ItemStack item = getItem();
        // blocks get placed if possible
        final BlockPos pos = result.getBlockPos().relative(result.getDirection());
        if (item.getItem() instanceof BlockItem block && !item.is(Items.POINTED_DRIPSTONE)) {
            final LivingEntity placer = JUtils.getUserIfStand(getOwner()) instanceof LivingEntity living ? living : null;
            if (item.is(JTagRegistry.BRITTLE) && hardness >= Blocks.STONE.defaultDestroyTime()) {
                // brittle things get destroyed
            } else if (!JUtils.mayAlter(level(), placer, pos, null)) {
                // the stand's user may not build where the block landed, so drop it as an item instead of placing
                dropItem(result.getLocation());
            } else if ((!(getOwner() instanceof StandEntity<?,?>) || ((net.minecraft.server.level.ServerLevel) level()).getGameRules().get(JCraft.STAND_GRIEFING)) &&
                    InteractionResult.SUCCESS != block.place(new BlockPlaceContext(new UseOnContext(level(), null, InteractionHand.MAIN_HAND, item, result)))) {
                dropItem(result.getLocation());
            }
        }
        // axe get used
        else if (item.is(ItemTags.AXES)) {
            BlockState blockState = level().getBlockState(result.getBlockPos());
            Optional<BlockState> stripped = Optional.empty();
            if (stripped.isPresent()) {
                level().setBlockAndUpdate(result.getBlockPos(), stripped.get());
                if (getOwner() instanceof LivingEntity lo) { item.hurtAndBreak(1, lo, EquipmentSlot.MAINHAND); }
            }
            stickOrDrop(result.getLocation());
        }
        // hoe get used
        else if (item.is(ItemTags.HOES)) {
            final UseOnContext context = new UseOnContext(level(), null, InteractionHand.MAIN_HAND, item, result);
            net.minecraft.world.level.block.Block hitBlk = level().getBlockState(result.getBlockPos()).getBlock();
            if (result.getDirection() != net.minecraft.core.Direction.DOWN && level().getBlockState(result.getBlockPos().above()).isAir()
                    && (hitBlk == Blocks.GRASS_BLOCK || hitBlk == Blocks.DIRT_PATH || hitBlk == Blocks.DIRT)) {
                level().setBlock(result.getBlockPos(), Blocks.FARMLAND.defaultBlockState(), 11);
                // tilled
            }
            stickOrDrop(result.getLocation());
        }
        // buckets empty their content if any
        else if (item.getItem() instanceof BucketItem bucket && !(item.is(Items.BUCKET))) {
            if (bucket.emptyContents(null, level(), result.getBlockPos(), result)) {
                bucket.checkExtraContent(null, level(), item, pos);
                setItem(Items.BUCKET.getDefaultInstance());
            }
            dropItem(result.getLocation());
        }
        // music disks fling into a jukebox
        else if (item.has(DataComponents.JUKEBOX_PLAYABLE)) {
            final BlockState box = level().getBlockState(result.getBlockPos());
            if (box.is(Blocks.JUKEBOX) && !box.getValue(JukeboxBlock.HAS_RECORD)) {
                BlockEntity blockEntity = level().getBlockEntity(result.getBlockPos());
                if (blockEntity instanceof JukeboxBlockEntity boxEntity) {
                    boxEntity.setTheItem(item.copy());
                }
                else { // shouldn't happen, but who knows?
                    dropItem(result.getLocation());
                }
            }
            else {
                dropItem(result.getLocation());
            }
        }
        // arrows get flinged
        // TODO implement
        // potions get activated
        else if (item.getItem() instanceof PotionItem) {
            if (level() instanceof ServerLevel sl) {
                PotionContents contents = item.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
                boolean instant = false;
                for (MobEffectInstance eff : contents.getAllEffects()) {
                    if (eff.getEffect().value().isInstantaneous()) {
                        instant = true;
                        break;
                    }
                }
                ThrownSplashPotion thrown = new ThrownSplashPotion(sl, result.getLocation().x, result.getLocation().y, result.getLocation().z, item.copy());
                sl.addFreshEntity(thrown);
                this.level().levelEvent(instant ? 2007 : 2002, pos, contents.getColor());
            }
        }
        // XP bottle gets activated
        else if (item.getItem() instanceof ExperienceBottleItem) {
            if (level() instanceof ServerLevel sl) {
                sl.levelEvent(2002, pos, 0x385DC6);
                int amount = 3 + sl.random.nextInt(5) + sl.random.nextInt(5);
                ExperienceOrb.award(sl, result.getLocation(), amount);
            }
        }
        // spawn eggs get activated
        else if (item.getItem() instanceof SpawnEggItem egg) {
            if (level() instanceof ServerLevel sl) {
                final EntityType<?> entityType2 = SpawnEggItem.getType(item);
                if (entityType2 != null) {
                    entityType2.spawn(sl, item, null, pos, EntitySpawnReason.SPAWN_ITEM_USE, true, false);
                }
            }
        }
        // rest just get dropped (or stuck if damageable)
        else {
            stickOrDrop(result.getLocation());
        }
        if (pickup == Pickup.ALLOWED) {
            // Position flush with the block face so the item doesn't sink in
            final Vec3 loc = result.getLocation();
            final Vec3 toHit = loc.subtract(getX(), getY(), getZ()).normalize().scale(0.05);
            setPosRaw(loc.x - toHit.x, loc.y - toHit.y, loc.z - toHit.z);
            setDeltaMovement(Vec3.ZERO);
            setInGround(true);
        } else {
            setInGround(true);
            discard();
        }
    }

    /**
     * Drops the item, or sticks it in the block if it has durability (axes, swords, pickaxes, etc.).
     * Returns true if sticking, false if dropped.
     */
    private boolean stickOrDrop(final Vec3 pos) {
        if ((getItem().has(DataComponents.TOOL) || getItem().has(DataComponents.WEAPON))) {
            pickup = Pickup.ALLOWED;
            return true;
        }
        dropItem(pos);
        return false;
    }

    /**
     * Drops the item behind this projectile at the specified location. Does NOT discard this projectile.
     */
    private void dropItem(final Vec3 pos) {
        final Vec3 vec3 = pos.subtract(getX(), getY(), getZ());
        setDeltaMovement(vec3);
        final Vec3 vecN = vec3.normalize().scale(0.05d);
        Containers.dropItemStack(level(), getX() - vecN.x, getY() - vecN.y, getZ() - vecN.z, getItem());
    }

    /**
     * Maximal number of ricocheting this projectile can do.
     */
    public int maxRicochets() {
        if (getItem().is(JTagRegistry.SUPER_BOUNCY)) {
            return 15;
        }
        if (getItem().is(JTagRegistry.BOUNCY)) {
            return 10;
        }
        if (getItem().is(JTagRegistry.SOMEWHAT_BOUNCY)) {
            return 5;
        }
        return 0;
    }

    public double penetrationAngle() {
        if (getItem().is(JTagRegistry.ACUTE)) {
            return 15d;
        }
        if (getItem().is(JTagRegistry.OBTUSE)) {
            return 60d;
        }
        return 45d;
    }

    /**
     * Mass of the projectile.
     */
    public double mass() {
        if (getItem().is(JTagRegistry.VERY_HEAVY)) {
            return 50d;
        }
        if (getItem().is(JTagRegistry.HEAVY)) {
            return 10d;
        }
        if (getItem().is(JTagRegistry.LIGHT)) {
            return 0.1;
        }
        return 1d;
    }

    public boolean isFerrous() {
        final ItemStack stack = getItem();
        return stack != null && stack.is(JTagRegistry.FERROUS_ITEMS);
    }
}
