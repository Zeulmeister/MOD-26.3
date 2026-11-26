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
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.IFlightCapable;
import net.arna.jcraft.api.stand.IInhaleCapable;
import net.arna.jcraft.api.stand.IOverwriteCapable;
import net.arna.jcraft.api.stand.ITimeAccelCapable;
import net.arna.jcraft.api.stand.ITimeEraseCapable;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.SummonData;
import net.arna.jcraft.common.attack.actions.LungeAction;
import net.arna.jcraft.common.attack.core.MoveMapImpl;
import net.arna.jcraft.common.attack.moves.kingcrimson.TimeEraseMove;
import net.arna.jcraft.common.attack.moves.shared.BarrageAttack;
import net.arna.jcraft.common.attack.moves.shared.GrabAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleMultiHitAttack;
import net.arna.jcraft.common.attack.moves.shared.SimpleUppercutAttack;
import net.arna.jcraft.common.attack.moves.shared.TimeSkipMove;
import net.arna.jcraft.common.attack.moves.shared.TimeStopMove;
import net.arna.jcraft.common.attack.moves.shared.TossChargeMove;
import net.arna.jcraft.common.attack.moves.shared.TossMove;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.stand.OriginSkillCatalog;
import net.arna.jcraft.common.stand.OriginSynergy;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.StandAnimationState;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Origin Stand: obsidian-armored sovereign with violet aura that can dynamically
 * bind and wield compatible moves from any Stand in the Origin Arsenal.
 */
public class OriginEntity extends StandEntity<OriginEntity, OriginEntity.State>
        implements ITimeEraseCapable, ITimeAccelCapable, IOverwriteCapable, IFlightCapable, IInhaleCapable {

    public static final MoveSet<OriginEntity, State> MOVESET = MoveSetManager.create(
            JStandTypeRegistry.ORIGIN, OriginEntity::registerMoves, OriginEntity.class, State.class);

    public static final SimpleUppercutAttack<OriginEntity> ABYSSAL_UPPERCUT = new SimpleUppercutAttack<OriginEntity>(
            0, 6, 12, 1.35f, 5.5f, 16, 1.5f, 0.45f, 0.2f, 0.65f)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.abyssal_uppercut.name"),
                    Component.translatable("move.jcraft.origin.default.abyssal_uppercut.description")
            );

    public static final SimpleAttack<OriginEntity> LIGHT = new SimpleAttack<OriginEntity>(
            10, 4, 9, 1.35f, 4.5f, 12, 1.5f, 0.25f, 0.1f)
            .withCrouchingVariant(ABYSSAL_UPPERCUT)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.light.name"),
                    Component.translatable("move.jcraft.origin.default.light.description")
            );

    public static final SimpleAttack<OriginEntity> HEAVY = new SimpleAttack<OriginEntity>(
            52, 15, 25, 1.5f, 9.5f, 24, 1.85f, 1.25f, 0.0f)
            .withSound(JSoundRegistry.ORIGIN_HEAVY)
            .withImpactSound(JSoundRegistry.IMPACT_9)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withBlockableType(BlockableType.NON_BLOCKABLE)
            .withArmor(2)
            .withLaunch()
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.heavy.name"),
                    Component.translatable("move.jcraft.origin.default.heavy.description")
            );

    public static final BarrageAttack<OriginEntity> BARRAGE = new BarrageAttack<OriginEntity>(
            80, 5, 45, 1.45f, 1.15f, 10, 1.8f, 0.12f, 0.0f, 3)
            .withSound(JSoundRegistry.ORIGIN_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.barrage.name"),
                    Component.translatable("move.jcraft.origin.default.barrage.description")
            );

    public static final SimpleAttack<OriginEntity> SPECIAL1 = new SimpleAttack<OriginEntity>(
            120, 9, 17, 1.55f, 7.0f, 20, 1.85f, 0.45f, 0.1f)
            .withInitAction(LungeAction.lunge(0.65f, 0.05f).isNotFree())
            .withSound(JSoundRegistry.ORIGIN_HEAVY)
            .withImpactSound(JSoundRegistry.IMPACT_5)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withBlockableType(BlockableType.NON_BLOCKABLE_EFFECTS_ONLY)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.special1.name"),
                    Component.translatable("move.jcraft.origin.default.special1.description")
            );

    public static final SimpleMultiHitAttack<OriginEntity> SPECIAL2 = new SimpleMultiHitAttack<OriginEntity>(
            150, 22, 1.5f, 2.8f, 14, 1.85f, 0.3f, 0.1f, IntSet.of(6, 12, 18))
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withStunType(StunType.UNBURSTABLE)
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.special2.name"),
                    Component.translatable("move.jcraft.origin.default.special2.description")
            );

    public static final SimpleAttack<OriginEntity> GRAB_HIT = new SimpleAttack<OriginEntity>(
            0, 12, 20, 1.45f, 7.5f, 22, 1.75f, 1.1f, 0.0f)
            .withImpactSound(JSoundRegistry.IMPACT_10)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withLaunch()
            .withInfo(
                    Component.translatable("move.jcraft.origin.default.grab_hit.name"),
                    Component.translatable("move.jcraft.origin.default.grab_hit.description")
            );

    public static final GrabAttack<OriginEntity, State> SPECIAL3 = new GrabAttack<OriginEntity, State>(
            200, 10, 20, 1.45f, 2.5f, 20, 1.6f, 0.0f, 0.1f, GRAB_HIT, StateContainer.of(State.GRAB_HIT))
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

    private static final Identifier SYNERGY_SPEED_MODIFIER_ID = JCraft.id("origin_synergy_speed");
    private static final AttributeModifier SYNERGY_SPEED_MODIFIER = new AttributeModifier(
            SYNERGY_SPEED_MODIFIER_ID, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final Identifier SYNERGY_KB_RES_MODIFIER_ID = JCraft.id("origin_synergy_kb_res");
    private static final AttributeModifier SYNERGY_KB_RES_MODIFIER = new AttributeModifier(
            SYNERGY_KB_RES_MODIFIER_ID, 0.25, AttributeModifier.Operation.ADD_VALUE);

    private int appliedLoadoutVersion = -1;
    private boolean appliedFreeBinds = true;
    private final EnumMap<MoveClass, OriginSkillCatalog.SkillEntry> activeSkills = new EnumMap<>(MoveClass.class);
    private OriginSynergy.SynergySummary activeSynergies = OriginSynergy.SynergySummary.EMPTY;
    private boolean uiPreview = false;

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
        moves.register(MoveClass.LIGHT, LIGHT, null, State.LIGHT).withCrouchingVariant(State.UPPERCUT);
        moves.register(MoveClass.HEAVY, HEAVY, State.HEAVY);
        moves.register(MoveClass.BARRAGE, BARRAGE, State.BARRAGE);

        moves.register(MoveClass.SPECIAL1, SPECIAL1, State.SPECIAL1);
        moves.register(MoveClass.SPECIAL2, SPECIAL2, State.SPECIAL2);
        moves.register(MoveClass.SPECIAL3, SPECIAL3, State.SPECIAL3);
        moves.register(MoveClass.ULTIMATE, ULTIMATE, State.TIME_STOP);

        moves.register(MoveClass.UTILITY, UTILITY, State.UTILITY);
        moves.register(MoveClass.TOSS, TOSS_CHARGE, State.UTILITY).withFollowup(State.UTILITY);
    }

    /**
     * Rebuilds the Stand's live {@link MoveMap} from the user's persisted Origin Arsenal loadout,
     * applying active {@link OriginSynergy} bonuses cleanly to copied moves and syncing Chroma RGB.
     */
    public void rebuildLoadoutFromUser(final CommonStandComponent userComp) {
        setCustomGlowRgb(userComp.getOriginGlowRgb());
        boolean freeBinds = JServerConfig.ORIGIN_FREE_BINDS.getValue();
        EnumMap<MoveClass, OriginSkillCatalog.SkillEntry> newActive = new EnumMap<>(MoveClass.class);
        for (MoveClass slot : MoveClass.values()) {
            String requestedId = userComp.getOriginSlot(slot);
            OriginSkillCatalog.SkillEntry resolved = OriginSkillCatalog.resolveForSlot(slot, requestedId, freeBinds);
            if (resolved != null) {
                newActive.put(slot, resolved);
            }
        }

        OriginSynergy.SynergySummary summary = OriginSynergy.evaluate(newActive.values());
        MoveMapImpl<OriginEntity, State> rebuilt = new MoveMapImpl<>();

        for (Map.Entry<MoveClass, OriginSkillCatalog.SkillEntry> e : newActive.entrySet()) {
            MoveClass slot = e.getKey();
            OriginSkillCatalog.SkillEntry resolved = e.getValue();

            AbstractMove<?, ?> copied = resolved.templateMove().copy();
            OriginSynergy.applyToMoveCopy(copied, resolved.category(), summary);
            AbstractGrabAttack.configureHitStateWithFallback(copied, State.class, State.GRAB_HIT);
            State mainState = OriginSkillCatalog.pickAnimState(slot, copied);
            CooldownType cooldownType = resolved.sourceEntry().getCooldownType() == null
                    ? null
                    : slot.getDefaultCooldownType();

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
            if (appliedLoadoutVersion != userComp.getOriginLoadoutVersion()
                    || appliedFreeBinds != freeBinds
                    || activeSkills.isEmpty()) {
                rebuildLoadoutFromUser(userComp);
            }
            if (!level().isClientSide()) {
                syncUserSynergyAttributes(getUser(), true);
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
            if (getMoveStun() > 0 && !blocking) {
                DustParticleOptions attackGlow = new DustParticleOptions(packedColor, 1.45f);
                Vec3 fwd = getLookAngle();
                for (int p = 0; p < 2; p++) {
                    double ox = (random.nextDouble() - 0.5) * 0.7;
                    double oy = 0.75 + random.nextDouble() * 0.9;
                    double oz = (random.nextDouble() - 0.5) * 0.7;
                    level().addParticle(attackGlow,
                            pos.x + fwd.x * 0.8 + ox, pos.y + oy, pos.z + fwd.z * 0.8 + oz,
                            fwd.x * 0.14, 0.02, fwd.z * 0.14);
                }
                if (tickCount % 2 == 0) {
                    var burst = (skin == 4) ? ParticleTypes.LARGE_SMOKE
                            : (skin == 5 ? ParticleTypes.END_ROD : ParticleTypes.WITCH);
                    level().addParticle(burst,
                            pos.x + fwd.x * 1.1 + (random.nextDouble() - 0.5) * 0.5,
                            pos.y + 0.9 + random.nextDouble() * 0.7,
                            pos.z + fwd.z * 1.1 + (random.nextDouble() - 0.5) * 0.5,
                            fwd.x * 0.1, 0.0, fwd.z * 0.1);
                }
            }
        }
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
        syncUserSynergyAttributes(getUser(), false);
        super.desummon();
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
        UTILITY(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "utility", AzPlayBehaviors.PLAY_ONCE)),
        TIME_STOP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "time_stop", AzPlayBehaviors.PLAY_ONCE)),
        OVERWRITE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "overwrite", AzPlayBehaviors.PLAY_ONCE));

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
