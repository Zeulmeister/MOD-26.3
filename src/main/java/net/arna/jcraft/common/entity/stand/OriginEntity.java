package net.arna.jcraft.common.entity.stand;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.NonNull;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.StateContainer;
import net.arna.jcraft.api.attack.enums.BlockableType;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.StunType;
import net.arna.jcraft.api.attack.moves.AbstractGrabAttack;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.IFlightCapable;
import net.arna.jcraft.api.stand.IIceLanceCapable;
import net.arna.jcraft.api.stand.IInhaleCapable;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.IOverwriteCapable;
import net.arna.jcraft.api.stand.IPoisonCapsuleCapable;
import net.arna.jcraft.api.stand.IRevolverCapable;
import net.arna.jcraft.api.stand.ITimeAccelCapable;
import net.arna.jcraft.api.stand.ITimeEraseCapable;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.SummonData;
import net.arna.jcraft.common.attack.actions.LungeAction;
import net.arna.jcraft.common.attack.core.MoveMapImpl;
import net.arna.jcraft.common.attack.moves.kingcrimson.TimeEraseMove;
import net.arna.jcraft.common.attack.moves.shared.MainBarrageAttack;
import net.arna.jcraft.common.attack.moves.shared.GrabAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleMultiHitAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleUppercutAttack;
import net.arna.jcraft.common.attack.moves.shared.TimeSkipMove;
import net.arna.jcraft.common.attack.moves.shared.TimeStopMove;
import net.arna.jcraft.common.attack.moves.shared.TossChargeMove;
import net.arna.jcraft.common.attack.moves.shared.TossMove;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.projectile.LargeIcicleProjectile;
import net.arna.jcraft.common.stand.OriginSkillCatalog;
import net.arna.jcraft.common.stand.OriginSynergy;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.StandAnimationState;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * The Origin Stand: obsidian-armored sovereign with violet aura that can dynamically
 * bind and wield compatible moves from any Stand in the Origin Arsenal.
 */
public class OriginEntity extends StandEntity<OriginEntity, OriginEntity.State>
        implements ITimeEraseCapable, ITimeAccelCapable, IOverwriteCapable, IFlightCapable, IInhaleCapable,
        IIronCapable, IPoisonCapsuleCapable, IIceLanceCapable, IRevolverCapable {

    public static final MoveSet<OriginEntity, State> MOVESET = MoveSetManager.create(
            JStandTypeRegistry.ORIGIN, OriginEntity::registerMoves, OriginEntity.class, State.class);

    public static final SimpleUppercutAttack<OriginEntity> ABYSSAL_UPPERCUT = new SimpleUppercutAttack<OriginEntity>(
            0, 6, 12, 0.85f, 5.5f, 16, 1.5f, 0.45f, 0.2f, 0.65f)
            .withAnim(State.UPPERCUT)
            .withSound(JSoundRegistry.ORIGIN_LIGHT)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.HIGH)
            .withExtraHitBox(0.0, 0.35, 1.25)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.abyssal_uppercut.name"),
                    Component.translatable("move.jcraft.origin.default.abyssal_uppercut.description")
            );

    public static final SimpleAttack<OriginEntity> LIGHT_FOLLOWUP = new SimpleAttack<OriginEntity>(
            0, 6, 11, 0.90f, 5.8f, 10, 1.6f, 0.95f, -0.15f)
            .withAnim(State.LIGHT_FOLLOWUP)
            .withSound(JSoundRegistry.ORIGIN_LIGHT)
            .withImpactSound(JSoundRegistry.IMPACT_5)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withExtraHitBox(0.35, 0.0, 1.15)
            .withLaunch()
            .withShockwaves()
            .withBlockStun(5)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.light_followup.name"),
                    Component.translatable("move.jcraft.origin.default.light_followup.description")
            );

    public static final SimpleAttack<OriginEntity> LIGHT = new SimpleAttack<OriginEntity>(
            10, 4, 9, 0.85f, 4.5f, 12, 1.5f, 0.25f, 0.1f)
            .withFollowup(LIGHT_FOLLOWUP)
            .withCrouchingVariant(ABYSSAL_UPPERCUT)
            .withSound(JSoundRegistry.ORIGIN_LIGHT)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.LOW)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.light.name"),
                    Component.translatable("move.jcraft.origin.default.light.description")
            );

    public static final SimpleAttack<OriginEntity> GRAVITY_SLAM = new SimpleAttack<OriginEntity>(
            60, 12, 20, 0.95f, 8.5f, 22, 2.65f, 1.15f, -0.35f)
            .withAnim(State.GRAB_HIT)
            .withSound(JSoundRegistry.ORIGIN_GRAB_HIT)
            .withImpactSound(JSoundRegistry.IMPACT_10)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withBlockableType(BlockableType.NON_BLOCKABLE)
            .withExtraHitBox(0.0, -0.25, 2.35)
            .withArmor(2)
            .withShockwaves()
            .withLaunch()
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.gravity_slam.name"),
                    Component.translatable("move.jcraft.origin.default.gravity_slam.description")
            );

    public static final SimpleAttack<OriginEntity> HEAVY = new SimpleAttack<OriginEntity>(
            52, 15, 25, 1.0f, 9.5f, 24, 1.85f, 1.25f, 0.0f)
            .withCrouchingVariant(GRAVITY_SLAM)
            .withSound(JSoundRegistry.ORIGIN_HEAVY)
            .withImpactSound(JSoundRegistry.IMPACT_9)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withExtraHitBox(0.0, 0.0, 1.35)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withBlockableType(BlockableType.NON_BLOCKABLE)
            .withArmor(2)
            .withShockwaves()
            .withLaunch()
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.heavy.name"),
                    Component.translatable("move.jcraft.origin.default.heavy.description")
            );

    public static final MainBarrageAttack<OriginEntity> BARRAGE = new MainBarrageAttack<OriginEntity>(
            120, 0, 42, 0.85f, 1.15f, 30, 2.0f, 0.22f, 0.0f, 3, Blocks.OBSIDIAN.defaultDestroyTime())
            .withSound(JSoundRegistry.ORIGIN_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.barrage.name"),
                    Component.translatable("move.jcraft.origin.default.barrage.description")
            );

    public static final SimpleAttack<OriginEntity> SPECIAL1 = new SimpleAttack<OriginEntity>(
            120, 9, 17, 1.0f, 7.0f, 20, 1.85f, 0.45f, 0.1f)
            .withInitAction(LungeAction.lunge(0.65f, 0.05f).isNotFree())
            .withSound(JSoundRegistry.ORIGIN_SPECIAL1)
            .withImpactSound(JSoundRegistry.IMPACT_5)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.HIGH)
            .withExtraHitBox(0.95, 0.1, 1.15)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withBlockableType(BlockableType.NON_BLOCKABLE_EFFECTS_ONLY)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.special1.name"),
                    Component.translatable("move.jcraft.origin.default.special1.description")
            );

    public static final SimpleMultiHitAttack<OriginEntity> SPECIAL2 = new SimpleMultiHitAttack<OriginEntity>(
            150, 22, 0.95f, 2.8f, 14, 1.85f, 0.3f, 0.1f, IntSet.of(6, 12, 18))
            .withSound(JSoundRegistry.ORIGIN_SPECIAL2)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.LOW)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withStunType(StunType.UNBURSTABLE)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.special2.name"),
                    Component.translatable("move.jcraft.origin.default.special2.description")
            );

    public static final SimpleAttack<OriginEntity> GRAB_HIT = new SimpleAttack<OriginEntity>(
            0, 12, 20, 0.95f, 7.5f, 22, 1.75f, 1.1f, 0.0f)
            .withSound(JSoundRegistry.ORIGIN_GRAB_HIT)
            .withImpactSound(JSoundRegistry.IMPACT_10)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withLaunch()
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.grab_hit.name"),
                    Component.translatable("move.jcraft.origin.default.grab_hit.description")
            );

    public static final GrabAttack<OriginEntity, State> SPECIAL3 = new GrabAttack<OriginEntity, State>(
            200, 10, 20, 0.95f, 2.5f, 20, 1.6f, 0.0f, 0.1f, GRAB_HIT, StateContainer.of(State.GRAB_HIT))
            .withSound(JSoundRegistry.ORIGIN_GRAB)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.special3.name"),
                    Component.translatable("move.jcraft.origin.default.special3.description")
            );

    public static final TimeStopMove<OriginEntity> ULTIMATE = new TimeStopMove<OriginEntity>(
            900, 24, 34, Either.left(100))
            .withSound(JSoundRegistry.ORIGIN_ULT)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.ultimate.name"),
                    Component.translatable("move.jcraft.origin.default.ultimate.description")
            );

    public static final TimeSkipMove<OriginEntity> UTILITY = new TimeSkipMove<OriginEntity>(
            180, 14.0, 2, 2, 1.5f)
            .withSound(JSoundRegistry.TIME_SKIP)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.utility.name"),
                    Component.translatable("move.jcraft.origin.default.utility.description")
            );

    public static final TossMove<OriginEntity> TOSS = new TossMove<OriginEntity>(0, 6, 12, 1.4f)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.toss.name"),
                    Component.translatable("move.jcraft.origin.default.toss.description")
            );

    public static final TossChargeMove<OriginEntity> TOSS_CHARGE = new TossChargeMove<OriginEntity>(
            70, 21, 40, 1.4f, 10)
            .withFollowup(TOSS)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.item_toss.name"),
                    Component.translatable("move.jcraft.origin.default.item_toss.description")
            );

    public static final StandData DATA = StandData.builder()
            .idleRotation(-45f)
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.origin"))
                    .proCount(4)
                    .conCount(1)
                    .freeSpace(Component.translatable("entity.jcraft.origin.info.freeSpace"))
                    .skinName(Component.translatable("entity.jcraft.origin.skin1"))
                    .skinName(Component.translatable("entity.jcraft.origin.skin2"))
                    .skinName(Component.translatable("entity.jcraft.origin.skin3"))
                    .skinName(Component.translatable("entity.jcraft.origin.skin4"))
                    .skinName(Component.translatable("entity.jcraft.origin.skin5"))
                    .build())
            .summonData(SummonData.of(JSoundRegistry.ORIGIN_SUMMON).withAnimDuration(24))
            .build()
            .withObtainable(true);

    private static final EntityDataAccessor<Integer> TIME_ERASE_TIME = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACCEL_TIME = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SPEEDOMETER = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AFTER_IMAGE = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> OVERWRITE_TYPE = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLIGHT_TIME = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INHALE_TIME = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CUSTOM_GLOW_RGB = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> IRON = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> METALLICA_INVISIBLE = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<BlockPos>> SIPHON_POS = SynchedEntityData.defineId(OriginEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    private static final Identifier SYNERGY_SPEED_MODIFIER_ID = JCraft.id("origin_synergy_speed");
    private static final AttributeModifier SYNERGY_SPEED_MODIFIER = new AttributeModifier(
            SYNERGY_SPEED_MODIFIER_ID, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final Identifier SYNERGY_KB_RES_MODIFIER_ID = JCraft.id("origin_synergy_kb_res");
    private static final AttributeModifier SYNERGY_KB_RES_MODIFIER = new AttributeModifier(
            SYNERGY_KB_RES_MODIFIER_ID, 0.25, AttributeModifier.Operation.ADD_VALUE);

    private int appliedLoadoutVersion = -1;
    private boolean appliedFreeBinds = true;
    private int appliedMaxUltimates = -1;
    private final EnumMap<MoveClass, OriginSkillCatalog.SkillEntry> activeSkills = new EnumMap<>(MoveClass.class);
    private OriginSynergy.SynergySummary activeSynergies = OriginSynergy.SynergySummary.EMPTY;
    private boolean uiPreview = false;
    private int bisectChargeTime = 0;
    private AbstractPurpleHazeEntity.PoisonType poisonType = AbstractPurpleHazeEntity.PoisonType.HARMING;
    private WeakReference<LargeIcicleProjectile> lastLargeIcicle = new WeakReference<>(null);

    public OriginEntity(final Level world) {
        super(JStandTypeRegistry.ORIGIN.get(), world);

        auraColors = new Vector3f[]{
                new Vector3f(0.64f, 0.16f, 1.00f),
                new Vector3f(0.96f, 0.14f, 0.24f),
                new Vector3f(0.14f, 0.86f, 1.00f),
                new Vector3f(1.00f, 0.80f, 0.16f),
                new Vector3f(0.86f, 0.90f, 0.98f),
                new Vector3f(1.00f, 0.84f, 0.20f)
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TIME_ERASE_TIME, 0);
        builder.define(ACCEL_TIME, 0);
        builder.define(SPEEDOMETER, 0);
        builder.define(AFTER_IMAGE, false);
        builder.define(OVERWRITE_TYPE, 0);
        builder.define(FLIGHT_TIME, 0);
        builder.define(INHALE_TIME, 0);
        builder.define(CUSTOM_GLOW_RGB, -1);
        builder.define(IRON, 80.0f);
        builder.define(METALLICA_INVISIBLE, false);
        builder.define(SIPHON_POS, Optional.empty());
    }

    public int getCustomGlowRgb() {
        return entityData.get(CUSTOM_GLOW_RGB);
    }

    public void setCustomGlowRgb(final int rgb) {
        entityData.set(CUSTOM_GLOW_RGB, rgb < 0 ? -1 : (rgb & 0xFFFFFF));
    }

    public boolean hasCustomGlowRgb() {
        return getCustomGlowRgb() >= 0;
    }

    public OriginSynergy.SynergySummary getActiveSynergies() {
        return activeSynergies;
    }

    public float getSynergyProjectileDamageMultiplier() {
        return OriginSynergy.computeProjectileDamageMultiplier(activeSynergies);
    }

    public boolean isUiPreview() {
        return uiPreview;
    }

    public void setUiPreview(final boolean uiPreview) {
        this.uiPreview = uiPreview;
    }

    @Override
    public Vector3f getAuraColor() {
        int rgb = getCustomGlowRgb();
        if (rgb >= 0) {
            return new Vector3f(
                    ((rgb >> 16) & 0xFF) / 255.0f,
                    ((rgb >> 8) & 0xFF) / 255.0f,
                    (rgb & 0xFF) / 255.0f
            );
        }
        return super.getAuraColor();
    }

    private static void registerMoves(final MoveMap<OriginEntity, State> moves) {
        moves.registerImmediate(MoveClass.LIGHT, LIGHT, State.LIGHT);
        moves.registerImmediate(MoveClass.HEAVY, HEAVY, State.HEAVY);
        moves.register(MoveClass.BARRAGE, BARRAGE, State.BARRAGE);

        moves.register(MoveClass.SPECIAL1, SPECIAL1, State.SPECIAL1);
        moves.register(MoveClass.SPECIAL2, SPECIAL2, State.SPECIAL2);
        moves.register(MoveClass.SPECIAL3, SPECIAL3, State.SPECIAL3);
        moves.register(MoveClass.ULTIMATE, ULTIMATE, State.TIME_STOP);

        moves.register(MoveClass.UTILITY, UTILITY, State.UTILITY);
        moves.register(MoveClass.TOSS, TOSS_CHARGE, State.ITEM_TOSS_CHARGE).withFollowup(State.ITEM_TOSS);
    }

    /**
     * Rebuilds the Stand's live {@link MoveMap} from the user's persisted Origin Arsenal loadout,
     * applying active {@link OriginSynergy} bonuses cleanly to copied moves and syncing Chroma RGB.
     */
    public void rebuildLoadoutFromUser(final CommonStandComponent userComp) {
        setCustomGlowRgb(userComp.getOriginGlowRgb());
        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        int maxUltimates = JServerConfig.ORIGIN_MAX_ULTIMATES.getValue();
        EnumMap<MoveClass, String> rawSlots = new EnumMap<>(MoveClass.class);
        for (MoveClass slot : MoveClass.values()) {
            rawSlots.put(slot, userComp.getOriginSlot(slot));
        }
        EnumMap<MoveClass, OriginSkillCatalog.SkillEntry> newActive =
                OriginSkillCatalog.sanitizeLoadout(rawSlots, freeBinds, maxUltimates);

        OriginSynergy.SynergySummary summary = OriginSynergy.evaluate(newActive.values());
        MoveMapImpl<OriginEntity, State> rebuilt = new MoveMapImpl<>();

        for (Map.Entry<MoveClass, OriginSkillCatalog.SkillEntry> e : newActive.entrySet()) {
            MoveClass slot = e.getKey();
            OriginSkillCatalog.SkillEntry resolved = e.getValue();

            AbstractMove<?, ?> copied = resolved.templateMove().copy();
            OriginSynergy.applyToMoveCopy(copied, resolved.category(), summary);
            AbstractGrabAttack.configureHitStateWithFallback(copied, State.class, State.GRAB_HIT);
            State mainState = OriginSkillCatalog.pickAnimState(slot, copied);
            CooldownType cooldownType;
            if (resolved.sourceEntry().getCooldownType() == null) {
                cooldownType = null;
            } else if (resolved.sourceSlot() == MoveClass.ULTIMATE) {
                // Any Ultimate skill bound across slots shares the dedicated Ultimate cooldown.
                cooldownType = CooldownType.STAND_ULTIMATE;
            } else {
                cooldownType = slot.getDefaultCooldownType();
            }

            MoveMap.Entry<OriginEntity, State> entry = registerCopiedMove(rebuilt, slot, copied, cooldownType, mainState);
            if (copied.getFollowup() != null) {
                entry.withFollowup(OriginSkillCatalog.pickAnimState(slot, copied.getFollowup()));
            }
            if (copied.getCrouchingVariant() != null) {
                entry.withCrouchingVariant(OriginSkillCatalog.pickAnimState(slot, copied.getCrouchingVariant()));
            }
            if (copied.getAerialVariant() != null) {
                entry.withAerialVariant(OriginSkillCatalog.pickAnimState(slot, copied.getAerialVariant()));
            }
        }

        if (!newActive.isEmpty()) {
            getMoveMap().copyFrom(rebuilt, true);
            activeSkills.clear();
            activeSkills.putAll(newActive);
        }
        activeSynergies = summary;
        if (!level().isClientSide() && hasUser()) {
            syncUserSynergyAttributes(getUser(), true);
        }
        appliedLoadoutVersion = userComp.getOriginLoadoutVersion();
        appliedFreeBinds = freeBinds;
        appliedMaxUltimates = maxUltimates;
    }

    @SuppressWarnings("unchecked")
    private static MoveMap.Entry<OriginEntity, State> registerCopiedMove(
            MoveMap<OriginEntity, State> map,
            MoveClass slot,
            AbstractMove<?, ?> move,
            @Nullable CooldownType cooldownType,
            State animState) {
        return map.register(slot, (AbstractMove<?, ? super OriginEntity>) move, cooldownType, animState);
    }

    public @Nullable OriginSkillCatalog.SkillEntry getEquippedSkill(MoveClass slot) {
        return activeSkills.get(slot);
    }

    @Override
    protected void playSummonSound() {
        if (shouldNotPlaySummonSound()) {
            return;
        }
        int skin = getSkin();
        switch (skin) {
            case 1 -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.0f, 0.86f);
                playBoundSound(SoundEvents.BLAZE_SHOOT, 0.55f, 0.65f);
            }
            case 2 -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.0f, 1.15f);
                playBoundSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.95f, 1.25f);
            }
            case 3 -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.0f, 0.94f);
                playBoundSound(SoundEvents.BEACON_ACTIVATE, 0.65f, 1.15f);
            }
            case 4 -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.1f, 0.64f);
                playBoundSound(SoundEvents.WITHER_SHOOT, 0.65f, 0.52f);
                playBoundSound(SoundEvents.SOUL_ESCAPE.value(), 1.0f, 0.58f);
            }
            case 5 -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.0f, 1.26f);
                playBoundSound(SoundEvents.BEACON_ACTIVATE, 0.85f, 1.48f);
                playBoundSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 1.55f);
                playBoundSound(SoundEvents.BELL_RESONATE, 0.55f, 1.40f);
            }
            default -> {
                playBoundSound(JSoundRegistry.ORIGIN_SUMMON.get(), 1.0f, 1.0f);
                playBoundSound(SoundEvents.ENDERMAN_TELEPORT, 0.45f, 0.72f);
            }
        }
    }

    private void syncUserSynergyAttributes(@Nullable LivingEntity target, boolean active) {
        if (target == null || level().isClientSide()) {
            return;
        }
        boolean wantSpeed = active && activeSynergies.isActive(OriginSynergy.SynergyType.PHANTOM_TEMPEST);
        AttributeInstance speedAttr = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            if (wantSpeed) {
                if (!speedAttr.hasModifier(SYNERGY_SPEED_MODIFIER_ID)) {
                    speedAttr.addTransientModifier(SYNERGY_SPEED_MODIFIER);
                }
            } else if (speedAttr.hasModifier(SYNERGY_SPEED_MODIFIER_ID)) {
                speedAttr.removeModifier(SYNERGY_SPEED_MODIFIER_ID);
            }
        }

        boolean wantKb = active && activeSynergies.isActive(OriginSynergy.SynergyType.OBSIDIAN_JUGGERNAUT);
        AttributeInstance kbAttr = target.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbAttr != null) {
            if (wantKb) {
                if (!kbAttr.hasModifier(SYNERGY_KB_RES_MODIFIER_ID)) {
                    kbAttr.addTransientModifier(SYNERGY_KB_RES_MODIFIER);
                }
            } else if (kbAttr.hasModifier(SYNERGY_KB_RES_MODIFIER_ID)) {
                kbAttr.removeModifier(SYNERGY_KB_RES_MODIFIER_ID);
            }
        }
    }

    @Override
    public boolean initMove(final MoveClass moveClass) {
        if (tryFollowUp(moveClass, moveClass)) {
            return true;
        }
        if (super.initMove(moveClass)) {
            return true;
        }
        if (canAttack() && getTETime() > 0) {
            MoveMap.Entry<OriginEntity, State> first = getFirstValidEntry(moveClass);
            if (first != null && first.getMove() instanceof TimeEraseMove<?> te) {
                te.cancelTE(this);
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        if (hasUser() && getUser() != null) {
            CommonStandComponent userComp = JComponentPlatformUtils.getStandComponent(getUser());
            boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
            int maxUltimates = JServerConfig.ORIGIN_MAX_ULTIMATES.getValue();
            if (appliedLoadoutVersion != userComp.getOriginLoadoutVersion()
                    || appliedFreeBinds != freeBinds
                    || appliedMaxUltimates != maxUltimates
                    || activeSkills.isEmpty()) {
                rebuildLoadoutFromUser(userComp);
            }
            if (!level().isClientSide()) {
                syncUserSynergyAttributes(getUser(), true);
                if (tickCount % 10 == 0 && !isMetallicaInvisible() && getIron() < getMaxIron()) {
                    addIron(1.0f);
                }
            }
        }

        super.tick();

        if (level().isClientSide() && hasUser()) {
            int skin = getSkin();
            boolean customGlow = hasCustomGlowRgb();
            Vector3f c = getAuraColor();
            int packedColor;
            if (customGlow) {
                packedColor = getCustomGlowRgb();
            } else if (skin == 4) {
                // Absolute Abyss: pitch-black void dust with faint silver glints
                packedColor = (tickCount % 6 == 0) ? 0xD8E0F4 : 0x0A0A10;
            } else if (skin == 5) {
                // Celestial Divinity: radiant solar gold alternating with pure marble white
                packedColor = (tickCount % 4 == 0) ? 0xFFFDF2 : 0xFFD94A;
            } else {
                packedColor = ((int) (MthClamp01(c.x) * 255) << 16)
                        | ((int) (MthClamp01(c.y) * 255) << 8)
                        | (int) (MthClamp01(c.z) * 255);
            }
            Vec3 pos = position();

            if (tickCount >= 1 && tickCount <= 24) {
                spawnSkinSummonParticles(skin, customGlow, packedColor, pos);
            }

            if (tickCount % 2 == 0) {
                DustParticleOptions auraDust = new DustParticleOptions(packedColor, 1.15f);
                double angle = (tickCount * 0.28) % (Math.PI * 2.0);
                double r = 0.78;
                level().addParticle(auraDust,
                        pos.x + Math.cos(angle) * r,
                        pos.y + 0.55 + (random.nextDouble() * 1.15),
                        pos.z + Math.sin(angle) * r,
                        0.0, 0.025, 0.0);
            }
            if (tickCount % 5 == 0) {
                var ambient = (skin == 4) ? ParticleTypes.SMOKE
                        : (skin == 5 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL);
                level().addParticle(ambient,
                        pos.x + (random.nextDouble() - 0.5) * 0.9,
                        pos.y + 0.3 + random.nextDouble() * 1.4,
                        pos.z + (random.nextDouble() - 0.5) * 0.9,
                        0.0, skin == 5 ? 0.015 : 0.03, 0.0);
            }
            if (blocking) {
                spawnBlockHaloParticles(skin, packedColor, pos);
            } else if (getMoveStun() > 0) {
                spawnCombatStateParticles(skin, packedColor, pos);
            }
        }
    }

    private void spawnBlockHaloParticles(int skin, int packedColor, Vec3 pos) {
        if (tickCount % 2 != 0) {
            return;
        }
        Vec3 fwd = getLookAngle();
        Vec3 rightVec = horizontalRight(fwd);
        Vec3 upVec = fwd.cross(rightVec).normalize();
        Vec3 shieldCenter = pos.add(fwd.scale(0.88)).add(0.0, 1.18, 0.0);
        DustParticleOptions haloDust = new DustParticleOptions(packedColor, 1.10f);
        double spin = tickCount * 0.14;
        for (int i = 0; i < 8; i++) {
            double ang = spin + i * (Math.PI * 2.0 / 8.0);
            Vec3 p = shieldCenter
                    .add(rightVec.scale(Math.cos(ang) * 0.82))
                    .add(upVec.scale(Math.sin(ang) * 0.82));
            level().addParticle(haloDust, p.x, p.y, p.z, fwd.x * 0.03, 0.01, fwd.z * 0.03);
        }
        if (tickCount % 4 == 0) {
            var core = (skin == 4) ? ParticleTypes.SMOKE
                    : (skin == 5 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL);
            level().addParticle(core, shieldCenter.x, shieldCenter.y, shieldCenter.z, 0.0, 0.015, 0.0);
        }
    }

    private void spawnCombatStateParticles(int skin, int packedColor, Vec3 pos) {
        Vec3 fwd = getLookAngle();
        Vec3 rightVec = horizontalRight(fwd);
        int ms = getMoveStun();
        var spark = (skin == 4) ? ParticleTypes.SOUL_FIRE_FLAME
                : (skin == 5 ? ParticleTypes.END_ROD : ParticleTypes.CRIT);
        var ambientBurst = (skin == 4) ? ParticleTypes.LARGE_SMOKE
                : (skin == 5 ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL);

        switch (getState()) {
            case BARRAGE -> {
                DustParticleOptions punchStreak = new DustParticleOptions(packedColor, 1.18f);
                for (int p = 0; p < 2; p++) {
                    double side = ((p + tickCount) % 2 == 0 ? 1.0 : -1.0) * (0.45 + random.nextDouble() * 0.50);
                    double lift = 0.80 + random.nextDouble() * 0.85;
                    double dist = 1.05 + random.nextDouble() * 0.70;
                    double px = pos.x + fwd.x * dist + rightVec.x * side;
                    double py = pos.y + lift;
                    double pz = pos.z + fwd.z * dist + rightVec.z * side;
                    level().addParticle(punchStreak, px, py, pz,
                            fwd.x * 0.48 + (random.nextDouble() - 0.5) * 0.05,
                            fwd.y * 0.38 + (random.nextDouble() - 0.5) * 0.04,
                            fwd.z * 0.48 + (random.nextDouble() - 0.5) * 0.05);
                }
                double sSide = (tickCount % 2 == 0 ? 0.5 : -0.5) * (0.5 + random.nextDouble() * 0.6);
                level().addParticle(spark,
                        pos.x + fwd.x * 1.35 + rightVec.x * sSide,
                        pos.y + 0.85 + random.nextDouble() * 0.85,
                        pos.z + fwd.z * 1.35 + rightVec.z * sSide,
                        fwd.x * 0.55, fwd.y * 0.45 + 0.03, fwd.z * 0.55);
                // Expanding air pressure rings ahead of fists
                if (tickCount % 3 == 0) {
                    Vec3 upVec = fwd.cross(rightVec).normalize();
                    Vec3 ringCenter = pos.add(fwd.scale(1.18)).add(0.0, 1.15, 0.0);
                    net.minecraft.core.particles.ParticleOptions airRing = (skin == 4) ? ParticleTypes.SMOKE
                            : (skin == 5 ? ParticleTypes.END_ROD : new DustParticleOptions(0xF5EEFF, 0.78f));
                    for (int i = 0; i < 8; i++) {
                        double ang = i * (Math.PI * 2.0 / 8.0) + tickCount * 0.3;
                        Vec3 radial = rightVec.scale(Math.cos(ang)).add(upVec.scale(Math.sin(ang)));
                        Vec3 pt = ringCenter.add(radial.scale(0.58));
                        level().addParticle(i % 2 == 0 ? punchStreak : airRing,
                                pt.x, pt.y, pt.z,
                                fwd.x * 0.34 + radial.x * 0.08,
                                fwd.y * 0.30 + radial.y * 0.08,
                                fwd.z * 0.34 + radial.z * 0.08);
                    }
                }
                if (tickCount % 2 == 0) {
                    double wSide = (random.nextBoolean() ? 1.0 : -1.0) * (0.65 + random.nextDouble() * 0.45);
                    level().addParticle(ambientBurst,
                            pos.x - fwd.x * 0.45 + rightVec.x * wSide,
                            pos.y + 0.8 + random.nextDouble() * 0.9,
                            pos.z - fwd.z * 0.45 + rightVec.z * wSide,
                            -fwd.x * 0.14, 0.03, -fwd.z * 0.14);
                }
            }
            case HEAVY, OVERWRITE -> {
                Vec3 fistPos = pos.add(rightVec.scale(0.62)).add(fwd.scale(0.45)).add(0.0, 1.18, 0.0);
                if (ms > 10) {
                    // Charging energy arc converging onto the right fist
                    DustParticleOptions chargeDust = new DustParticleOptions(packedColor, 1.40f);
                    for (int i = 0; i < 5; i++) {
                        double ang = tickCount * 0.52 + i * (Math.PI * 2.0 / 5.0);
                        Vec3 off = rightVec.scale(Math.cos(ang) * 0.55).add(0.0, Math.sin(ang) * 0.55, 0.0);
                        Vec3 p = fistPos.add(off);
                        level().addParticle(i == 0 ? spark : chargeDust, p.x, p.y, p.z,
                                -off.x * 0.18, -off.y * 0.18, -off.z * 0.18);
                    }
                } else if (ms == 10 || ms == 9) {
                    // Radial explosion at strike frame
                    Vec3 burstCenter = pos.add(fwd.scale(1.45)).add(0.0, 1.05, 0.0);
                    DustParticleOptions impactDust = new DustParticleOptions(packedColor, 1.85f);
                    for (int i = 0; i < 16; i++) {
                        double ang = i * (Math.PI * 2.0 / 16.0);
                        double rise = (i % 3 - 1) * 0.18;
                        double dx = Math.cos(ang) * 0.38;
                        double dz = Math.sin(ang) * 0.38;
                        level().addParticle(i % 2 == 0 ? impactDust : spark,
                                burstCenter.x, burstCenter.y, burstCenter.z, dx, rise, dz);
                    }
                } else {
                    DustParticleOptions trailDust = new DustParticleOptions(packedColor, 1.25f);
                    level().addParticle(trailDust, fistPos.x + fwd.x * 0.7, fistPos.y, fistPos.z + fwd.z * 0.7,
                            fwd.x * 0.18, 0.02, fwd.z * 0.18);
                }
            }
            case TIME_STOP, ULTIMATE -> {
                Vec3 coreCenter = pos.add(0.0, 1.30, 0.0);
                if (ms > 10) {
                    // Particles pulled inward toward core during windup
                    DustParticleOptions pullDust = new DustParticleOptions(packedColor, 1.45f);
                    for (int i = 0; i < 8; i++) {
                        double ang = tickCount * 0.38 + i * (Math.PI * 2.0 / 8.0);
                        double r = 1.8 + (i % 2) * 0.85;
                        double sy = (random.nextDouble() - 0.3) * 1.4;
                        Vec3 spawn = coreCenter.add(Math.cos(ang) * r, sy, Math.sin(ang) * r);
                        Vec3 vel = coreCenter.subtract(spawn).scale(0.18);
                        level().addParticle(i % 3 == 0 ? ambientBurst : pullDust,
                                spawn.x, spawn.y, spawn.z, vel.x, vel.y, vel.z);
                    }
                } else if (ms == 10 || ms == 9) {
                    // Expanding dome shockwave on Time Stop snap
                    DustParticleOptions domeDust = new DustParticleOptions(packedColor, 1.90f);
                    for (int i = 0; i < 20; i++) {
                        double ang = i * (Math.PI * 2.0 / 20.0);
                        level().addParticle(i % 2 == 0 ? domeDust : spark,
                                coreCenter.x, coreCenter.y - 0.15, coreCenter.z,
                                Math.cos(ang) * 0.45, 0.05, Math.sin(ang) * 0.45);
                        if (i % 2 == 0) {
                            level().addParticle(ambientBurst,
                                    coreCenter.x, coreCenter.y, coreCenter.z,
                                    Math.cos(ang) * 0.30, 0.28, Math.sin(ang) * 0.30);
                        }
                    }
                }
            }
            case GRAB, SPECIAL3 -> {
                // Double-helix vacuum spiral in front of Origin
                Vec3 focus = pos.add(fwd.scale(1.40)).add(0.0, 1.05, 0.0);
                Vec3 upVec = fwd.cross(rightVec).normalize();
                DustParticleOptions spiralDust = new DustParticleOptions(packedColor, 1.30f);
                for (int arm = 0; arm < 2; arm++) {
                    for (int step = 0; step < 3; step++) {
                        double ang = tickCount * 0.65 + arm * Math.PI + step * 0.7;
                        double r = 0.78 - step * 0.18;
                        Vec3 radial = rightVec.scale(Math.cos(ang) * r).add(upVec.scale(Math.sin(ang) * r));
                        Vec3 p = focus.add(radial).add(fwd.scale((step - 1) * 0.25));
                        level().addParticle(arm == 0 && step == 0 ? ambientBurst : spiralDust,
                                p.x, p.y, p.z,
                                -radial.x * 0.14, -radial.y * 0.14, -radial.z * 0.14);
                    }
                }
            }
            case GRAB_HIT -> {
                if (ms > 8) {
                    Vec3 hoist = pos.add(fwd.scale(0.85)).add(0.0, 1.95, 0.0);
                    DustParticleOptions hoistDust = new DustParticleOptions(packedColor, 1.40f);
                    for (int i = 0; i < 4; i++) {
                        double ang = tickCount * 0.6 + i * (Math.PI * 0.5);
                        level().addParticle(hoistDust,
                                hoist.x + Math.cos(ang) * 0.55, hoist.y, hoist.z + Math.sin(ang) * 0.55,
                                0.0, -0.06, 0.0);
                    }
                } else if (ms == 8 || ms == 7) {
                    DustParticleOptions slamDust = new DustParticleOptions(packedColor, 1.85f);
                    for (int i = 0; i < 18; i++) {
                        double ang = i * (Math.PI * 2.0 / 18.0);
                        level().addParticle(i % 2 == 0 ? slamDust : spark,
                                pos.x + Math.cos(ang) * 0.4, pos.y + 0.25, pos.z + Math.sin(ang) * 0.4,
                                Math.cos(ang) * 0.42, 0.12, Math.sin(ang) * 0.42);
                    }
                }
            }
            default -> {
                DustParticleOptions slashDust = new DustParticleOptions(packedColor, 1.35f);
                for (int p = 0; p < 2; p++) {
                    double side = (p == 0 ? 1.0 : -1.0) * (0.35 + random.nextDouble() * 0.45);
                    double lift = 0.75 + random.nextDouble() * 0.90;
                    level().addParticle(slashDust,
                            pos.x + fwd.x * 0.95 + rightVec.x * side,
                            pos.y + lift,
                            pos.z + fwd.z * 0.95 + rightVec.z * side,
                            fwd.x * 0.20, 0.03, fwd.z * 0.20);
                }
                if (tickCount % 2 == 0) {
                    level().addParticle(ambientBurst,
                            pos.x + fwd.x * 1.15, pos.y + 1.05, pos.z + fwd.z * 1.15,
                            fwd.x * 0.12, 0.02, fwd.z * 0.12);
                }
            }
        }
    }

    private static Vec3 horizontalRight(Vec3 fwd) {
        Vec3 r = new Vec3(-fwd.z, 0.0, fwd.x);
        if (r.lengthSqr() < 1.0E-4) {
            return new Vec3(1.0, 0.0, 0.0);
        }
        return r.normalize();
    }

    private void spawnSkinSummonParticles(int skin, boolean customGlow, int packedColor, Vec3 pos) {
        float progress = tickCount / 24.0f;
        switch (skin) {
            case 4 -> {
                // Absolute Abyss: expanding ground wave of dark smoke, ink, and crisp silver glints
                double radius = 0.55 + tickCount * 0.13;
                int ringColor = customGlow ? packedColor : 0x07070D;
                DustParticleOptions darkWave = new DustParticleOptions(ringColor, 1.85f);
                for (int i = 0; i < 10; i++) {
                    double ang = (i * (Math.PI * 2.0 / 10.0)) + tickCount * 0.18;
                    double px = pos.x + Math.cos(ang) * radius;
                    double pz = pos.z + Math.sin(ang) * radius;
                    level().addParticle(darkWave, px, pos.y + 0.18 + (i % 2) * 0.22, pz,
                            Math.cos(ang) * 0.04, 0.02, Math.sin(ang) * 0.04);
                    if (i % 2 == 0) {
                        level().addParticle(ParticleTypes.LARGE_SMOKE, px, pos.y + 0.25, pz,
                                Math.cos(ang) * 0.05, 0.03, Math.sin(ang) * 0.05);
                    }
                    if (tickCount % 3 == 0) {
                        level().addParticle(ParticleTypes.SQUID_INK, px * 0.3 + pos.x * 0.7, pos.y + 0.4 + random.nextDouble(), pz * 0.3 + pos.z * 0.7,
                                0.0, 0.03, 0.0);
                    }
                }
                if (tickCount == 2 || tickCount == 10) {
                    int glint = customGlow ? packedColor : 0xDCE6FF;
                    DustParticleOptions silverFlash = new DustParticleOptions(glint, 1.6f);
                    for (int i = 0; i < 12; i++) {
                        double ang = i * (Math.PI * 2.0 / 12.0);
                        level().addParticle(silverFlash,
                                pos.x + Math.cos(ang) * 1.15, pos.y + 1.1, pos.z + Math.sin(ang) * 1.15,
                                0.0, 0.06, 0.0);
                    }
                }
            }
            case 5 -> {
                // Celestial Divinity: rising golden & marble-white light pillar with ascending halo rings
                int goldColor = customGlow ? packedColor : 0xFFD94A;
                int whiteColor = customGlow ? packedColor : 0xFFFDF2;
                DustParticleOptions goldDust = new DustParticleOptions(goldColor, 1.65f);
                DustParticleOptions whiteDust = new DustParticleOptions(whiteColor, 1.45f);
                for (int yStep = 0; yStep < 4; yStep++) {
                    double h = (yStep * 0.9) + (progress * 1.2);
                    double ang = (tickCount * 0.45) + (yStep * 1.1);
                    level().addParticle(ParticleTypes.END_ROD,
                            pos.x + Math.cos(ang) * 0.55, pos.y + 0.2 + h, pos.z + Math.sin(ang) * 0.55,
                            0.0, 0.07, 0.0);
                    level().addParticle(yStep % 2 == 0 ? goldDust : whiteDust,
                            pos.x - Math.cos(ang) * 0.75, pos.y + 0.15 + h, pos.z - Math.sin(ang) * 0.75,
                            0.0, 0.05, 0.0);
                }
                for (int i = 0; i < 8; i++) {
                    double ang = i * (Math.PI * 2.0 / 8.0) + tickCount * 0.2;
                    double r = 0.7 + Math.sin(progress * Math.PI) * 0.9;
                    level().addParticle(goldDust, pos.x + Math.cos(ang) * r, pos.y + 0.18, pos.z + Math.sin(ang) * r, 0.0, 0.04, 0.0);
                    level().addParticle(whiteDust, pos.x + Math.cos(ang) * (r * 0.65), pos.y + 2.55, pos.z + Math.sin(ang) * (r * 0.65), 0.0, 0.02, 0.0);
                }
                if (tickCount == 2 || tickCount == 8) {
                    for (int i = 0; i < 10; i++) {
                        double ang = i * (Math.PI * 2.0 / 10.0);
                        level().addParticle(ParticleTypes.TOTEM_OF_UNDYING,
                                pos.x + Math.cos(ang) * 0.85, pos.y + 1.2, pos.z + Math.sin(ang) * 0.85,
                                Math.cos(ang) * 0.12, 0.15, Math.sin(ang) * 0.12);
                    }
                }
            }
            case 1 -> {
                DustParticleOptions crimsonDust = new DustParticleOptions(packedColor, 1.5f);
                for (int i = 0; i < 6; i++) {
                    double ang = (i * (Math.PI * 2.0 / 6.0)) + tickCount * 0.3;
                    double r = 0.6 + progress * 1.0;
                    level().addParticle(crimsonDust, pos.x + Math.cos(ang) * r, pos.y + 0.35 + (i % 3) * 0.55, pos.z + Math.sin(ang) * r,
                            0.0, 0.04, 0.0);
                    if (i % 2 == 0) {
                        level().addParticle(ParticleTypes.FLAME, pos.x + Math.cos(ang) * r, pos.y + 0.25, pos.z + Math.sin(ang) * r,
                                Math.cos(ang) * 0.03, 0.05, Math.sin(ang) * 0.03);
                    }
                }
            }
            case 2 -> {
                DustParticleOptions cyanDust = new DustParticleOptions(packedColor, 1.45f);
                for (int i = 0; i < 6; i++) {
                    double ang = (i * (Math.PI * 2.0 / 6.0)) - tickCount * 0.35;
                    double r = 0.5 + progress * 1.1;
                    level().addParticle(cyanDust, pos.x + Math.cos(ang) * r, pos.y + 0.4 + progress * 1.6, pos.z + Math.sin(ang) * r,
                            0.0, 0.03, 0.0);
                    if (i % 2 == 0) {
                        level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.x + Math.cos(ang) * 0.8, pos.y + 0.3, pos.z + Math.sin(ang) * 0.8,
                                0.0, 0.05, 0.0);
                    }
                }
            }
            case 3 -> {
                DustParticleOptions goldDust = new DustParticleOptions(packedColor, 1.55f);
                for (int i = 0; i < 4; i++) {
                    double ang = (i * (Math.PI / 2.0)) + tickCount * 0.15;
                    level().addParticle(goldDust, pos.x + Math.cos(ang) * 0.95, pos.y + progress * 2.4, pos.z + Math.sin(ang) * 0.95,
                            0.0, 0.05, 0.0);
                    level().addParticle(ParticleTypes.ENCHANT, pos.x + Math.cos(ang) * 1.0, pos.y + 0.4 + progress * 1.8, pos.z + Math.sin(ang) * 1.0,
                            0.0, 0.08, 0.0);
                }
            }
            default -> {
                DustParticleOptions violetDust = new DustParticleOptions(packedColor, 1.45f);
                for (int i = 0; i < 6; i++) {
                    double ang = (i * (Math.PI / 3.0)) + tickCount * 0.35;
                    double r = 0.65 + (1.0 - progress) * 0.7;
                    level().addParticle(violetDust, pos.x + Math.cos(ang) * r, pos.y + 0.25 + progress * 2.0, pos.z + Math.sin(ang) * r,
                            0.0, 0.04, 0.0);
                    if (i % 2 == 0) {
                        level().addParticle(ParticleTypes.REVERSE_PORTAL, pos.x + Math.cos(ang) * 1.1, pos.y + 0.2, pos.z + Math.sin(ang) * 1.1,
                                0.0, 0.06, 0.0);
                    }
                }
            }
        }
    }

    private static float MthClamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    @Override
    public void desummon() {
        if (getFlightTime() > 0) {
            setFlightTime(0);
            if (hasUser() && getUserOrThrow() instanceof Player player && !player.isCreative() && !player.isSpectator()) {
                player.getAbilities().flying = false;
            }
            return;
        }
        desummon(true);
    }

    @Override
    public void desummon(final boolean playSound) {
        if (getCurrentMove() != null || getMoveStun() > 0) {
            return;
        }
        syncUserSynergyAttributes(getUser(), false);
        if (playSound && !level().isClientSide()) {
            float pitch = switch (getSkin()) {
                case 1 -> 0.88f;
                case 2 -> 1.12f;
                case 3 -> 0.96f;
                case 4 -> 0.70f;
                case 5 -> 1.22f;
                default -> 1.0f;
            };
            playSound(JSoundRegistry.ORIGIN_DESUMMON.get(), 1.0f, pitch);
        }
        super.desummon(false);
    }

    @Override
    public void setState(final State state) {
        super.setState(state);
        if (!level().isClientSide() && state == State.COUNTER) {
            playBoundSound(JSoundRegistry.ORIGIN_COUNTER.get(), 1.0f, 1.0f);
        }
    }

    @Override
    public void remove(@NonNull RemovalReason reason) {
        syncUserSynergyAttributes(getUser(), false);
        if (getUser() instanceof Player player && player.getAbilities().flying && !player.isCreative() && !player.isSpectator()) {
            Abilities abilities = player.getAbilities();
            abilities.flying = false;
            abilities.mayfly = false;
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket(abilities));
            }
        }
        super.remove(reason);
    }

    @Override
    public int getTETime() {
        return entityData.get(TIME_ERASE_TIME);
    }

    @Override
    public void setTETime(int time) {
        entityData.set(TIME_ERASE_TIME, time);
    }

    @Override
    public int getAccelTime() {
        return entityData.get(ACCEL_TIME);
    }

    @Override
    public void setAccelTime(int time) {
        entityData.set(ACCEL_TIME, time);
    }

    @Override
    public int getSpeedometer() {
        return entityData.get(SPEEDOMETER);
    }

    @Override
    public void setSpeedometer(int speedometer) {
        entityData.set(SPEEDOMETER, speedometer);
    }

    @Override
    public boolean isAfterimage() {
        return entityData.get(AFTER_IMAGE);
    }

    @Override
    public void setAfterimage(boolean afterimage) {
        entityData.set(AFTER_IMAGE, afterimage);
    }

    @Override
    public int getOverwriteType() {
        return entityData.get(OVERWRITE_TYPE);
    }

    @Override
    public void setOverwriteType(int type) {
        entityData.set(OVERWRITE_TYPE, type);
    }

    @Override
    public int getFlightTime() {
        return entityData.get(FLIGHT_TIME);
    }

    @Override
    public void setFlightTime(int time) {
        entityData.set(FLIGHT_TIME, time);
    }

    @Override
    public int getInhaleTime() {
        return entityData.get(INHALE_TIME);
    }

    @Override
    public void setInhaleTime(int time) {
        entityData.set(INHALE_TIME, time);
    }

    @Override
    public float getIron() {
        return entityData.get(IRON);
    }

    @Override
    public void setIron(float iron) {
        entityData.set(IRON, Mth.clamp(iron, 0.0f, getMaxIron()));
    }

    @Override
    public void addIron(float add) {
        setIron(getIron() + add);
    }

    @Override
    public boolean drainIron(float cost) {
        float iron = getIron();
        if (iron < cost) {
            return false;
        }
        setIron(iron - cost);
        return true;
    }

    @Override
    public int getBisectChargeTime() {
        return bisectChargeTime;
    }

    @Override
    public void setBisectChargeTime(int time) {
        this.bisectChargeTime = time;
    }

    @Override
    public boolean isMetallicaInvisible() {
        return entityData.get(METALLICA_INVISIBLE);
    }

    @Override
    public void setMetallicaInvisible(boolean invisible) {
        entityData.set(METALLICA_INVISIBLE, invisible);
    }

    @Override
    public boolean isInvisible() {
        return super.isInvisible() || isMetallicaInvisible();
    }

    @Override
    public Optional<BlockPos> getSiphonPos() {
        return entityData.get(SIPHON_POS);
    }

    @Override
    public void setSiphonPos(Optional<BlockPos> pos) {
        entityData.set(SIPHON_POS, pos);
    }

    @Override
    public AbstractPurpleHazeEntity.PoisonType getPoisonType() {
        return poisonType;
    }

    @Override
    public void nextPoisonType() {
        AbstractPurpleHazeEntity.PoisonType[] vals = AbstractPurpleHazeEntity.PoisonType.values();
        poisonType = vals[(poisonType.ordinal() + 1) % vals.length];
    }

    @Override
    public @Nullable LargeIcicleProjectile getLastLargeIcicle() {
        return lastLargeIcicle.get();
    }

    @Override
    public void setLastLargeIcicle(@Nullable LargeIcicleProjectile icicle) {
        lastLargeIcicle = new WeakReference<>(icicle);
    }

    @Override
    public void equipRevolver() {
        setItemSlot(EquipmentSlot.MAINHAND, JItemRegistry.FV_REVOLVER.get().getDefaultInstance());
    }

    @Override
    public @NonNull OriginEntity getThis() {
        return this;
    }

    public enum State implements StandAnimationState<OriginEntity> {
        IDLE(AzCommand.create(JCraft.BASE_CONTROLLER, "idle", AzPlayBehaviors.LOOP)),
        BLOCK(AzCommand.create(JCraft.BASE_CONTROLLER, "block", AzPlayBehaviors.LOOP)),
        LIGHT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light", AzPlayBehaviors.PLAY_ONCE)),
        HEAVY(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "heavy", AzPlayBehaviors.PLAY_ONCE)),
        BARRAGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "barrage", AzPlayBehaviors.LOOP)),
        SPECIAL1(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "special1", AzPlayBehaviors.PLAY_ONCE)),
        SPECIAL2(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "special2", AzPlayBehaviors.PLAY_ONCE)),
        SPECIAL3(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "special3", AzPlayBehaviors.PLAY_ONCE)),
        UPPERCUT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "uppercut", AzPlayBehaviors.PLAY_ONCE)),
        COUNTER(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "counter", AzPlayBehaviors.PLAY_ONCE)),
        COUNTER_MISS(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "counter_miss", AzPlayBehaviors.PLAY_ONCE)),
        GRAB(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "grab", AzPlayBehaviors.PLAY_ONCE)),
        GRAB_HIT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "grab_hit", AzPlayBehaviors.PLAY_ONCE)),
        ULTIMATE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "ultimate", AzPlayBehaviors.PLAY_ONCE)),
        LIGHT_FOLLOWUP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light_followup", AzPlayBehaviors.PLAY_ONCE)),
        UTILITY(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "utility", AzPlayBehaviors.PLAY_ONCE)),
        TIME_STOP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "time_stop", AzPlayBehaviors.PLAY_ONCE)),
        OVERWRITE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "overwrite", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS_CHARGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow_charge", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow", AzPlayBehaviors.PLAY_ONCE));

        private final AzCommand animator;

        State(final @NonNull AzCommand animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(final @NonNull OriginEntity attacker) {
            animator.sendForEntity(attacker);
        }
    }

    @Override
    protected State[] getStateValues() {
        return State.values();
    }

    @Override
    public State getBlockState() {
        return State.BLOCK;
    }
}
