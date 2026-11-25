package net.arna.jcraft.common.entity.stand;

import it.unimi.dsi.fastutil.ints.IntList;
import lombok.NonNull;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.SummonData;
import net.arna.jcraft.common.attack.actions.LungeAction;
import net.arna.jcraft.common.attack.moves.shared.*;
import net.arna.jcraft.common.attack.moves.starplatinum.BlockBreakingAttack;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.StandAnimationState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * The {@link StandEntity} for <a href="https://jojowiki.com/Crazy_Diamond">Crazy Diamond</a>.
 * High-speed close-range brawler capable of repairing blocks via restoration recipes,
 * healing allies/mobs (never its own user), shattering walls, and firing precision ball bearings.
 */
public class CrazyDiamondEntity extends StandEntity<CrazyDiamondEntity, CrazyDiamondEntity.State> {
    public static final MoveSet<CrazyDiamondEntity, CrazyDiamondEntity.State> MOVE_SET = MoveSetManager.create(
            JStandTypeRegistry.CRAZY_DIAMOND,
            CrazyDiamondEntity::registerDefaultMoves,
            CrazyDiamondEntity.class,
            CrazyDiamondEntity.State.class
    );

    public static final RestorationAttack<CrazyDiamondEntity> LIGHT_FOLLOWUP = new RestorationAttack<CrazyDiamondEntity>(
            0, 6, 13, 0.75f, 6f, 10, 1.5f, 1.05f, -0.1f)
            .withAnim(State.LIGHT_FOLLOWUP)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withLaunch()
            .withBlockStun(4)
            .withExtraHitBox(0, 0.25, 1)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.punch.followup.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.punch.followup.description")
            );

    public static final RestorationAttack<CrazyDiamondEntity> RESTORE_UPPERCUT = new RestorationAttack<CrazyDiamondEntity>(
            0, 8, 16, 0.85f, 6.5f, 16, 1.6f, 0.7f, 0.1f)
            .withAnim(State.UPPERCUT)
            .withImpactSound(JSoundRegistry.IMPACT_4)
            .withLaunch()
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.HIGH)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.restore_uppercut.cr.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.restore_uppercut.cr.description")
            );

    public static final RestorationAttack<CrazyDiamondEntity> PUNCH = RestorationAttack.<CrazyDiamondEntity>lightAttack(
            5, 9, 0.75f, 5f, 10, 0.25f, -0.1f)
            .withFollowup(LIGHT_FOLLOWUP)
            .withCrouchingVariant(RESTORE_UPPERCUT)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.punch.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.punch.description")
            );

    public static final KnockdownAttack<CrazyDiamondEntity> DIAMOND_SMASH = new KnockdownAttack<CrazyDiamondEntity>(
            0, 16, 26, 1.0f, 9.5f, 12, 1.9f, 1.4f, 0f, 30)
            .withSound(JSoundRegistry.STAR_BREAKER)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withHyperArmor()
            .withLaunch()
            .withExtraHitBox(1.25)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.diamond_smash.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.diamond_smash.description")
            );

    public static final MainBarrageAttack<CrazyDiamondEntity> DORA_BARRAGE = new MainBarrageAttack<CrazyDiamondEntity>(
            280, 0, 40, 0.75f, 1.0f, 20, 2.0f, 0.25f, 0f, 3, Blocks.OBSIDIAN.defaultDestroyTime())
            .withSound(JSoundRegistry.STAR_PLATINUM_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_2)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.dora_barrage.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.dora_barrage.description")
            );

    public static final RestorationAttack<CrazyDiamondEntity> BLOCK_RESTORE_BURST = new RestorationAttack<CrazyDiamondEntity>(
            100, 8, 16, 1.1f, 4.5f, 12, 2.25f, 0.45f, 0f)
            .withSound(JSoundRegistry.GE_HEAL)
            .withImpactSound(JSoundRegistry.IMPACT_5)
            .withExtraHitBox(1.5)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.block_restore.cr.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.block_restore.cr.description")
            );

    public static final HealMove<CrazyDiamondEntity> RESTORATION_HAND = new HealMove<CrazyDiamondEntity>(
            320, 10, 18, 1.0f, 1.75f, 0f, 8.0f, HealMove.HealTarget.TARGETS, true)
            .withCrouchingVariant(BLOCK_RESTORE_BURST)
            .withSound(JSoundRegistry.GE_HEAL)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.restoration_hand.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.restoration_hand.description")
            );

    public static final BlockBreakingAttack<CrazyDiamondEntity> WALL_BREAKER = new BlockBreakingAttack<CrazyDiamondEntity>(
            180, 13, 23, 1.0f, 7.5f, 18, 1.9f, 0.9f, 0.4f)
            .withSound(JSoundRegistry.STAR_BREAKER)
            .withImpactSound(JSoundRegistry.IMPACT_8)
            .withHyperArmor()
            .withLaunch()
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.wall_breaker.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.wall_breaker.description")
            );

    public static final SimpleHitscanAttack<CrazyDiamondEntity> BEARING_SNAP = new SimpleHitscanAttack<CrazyDiamondEntity>(
            140, 9, 16, 0.75f, 6.0f, 14, 0.5f, 18.0f, 0.015f)
            .withSound(JSoundRegistry.IMPACT_6)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withShootSpark(JParticleType.FLASH)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.bearing_snap.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.bearing_snap.description")
            );

    public static final KnockdownMultiHitAttack<CrazyDiamondEntity> UNBREAKABLE_RAGE = new KnockdownMultiHitAttack<CrazyDiamondEntity>(
            600, 34, 1.1f, 4.5f, 18, 2.0f, 0.6f, 0f, IntList.of(10, 18, 26), 40)
            .withSound(JSoundRegistry.STAR_PLATINUM_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_10)
            .withHyperArmor()
            .withAction(LungeAction.lunge(0.55f, 0.1f))
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.unbreakable_rage.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.unbreakable_rage.description")
            );

    public static final JumpMove<CrazyDiamondEntity> SHARD_LEAP = new JumpMove<CrazyDiamondEntity>(
            260, 4, 12, 1.0f, 1.45f)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.shard_leap.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.shard_leap.description")
            );

    public static final TossMove<CrazyDiamondEntity> TOSS = new TossMove<CrazyDiamondEntity>(0, 1, 1, 0.75f, 0.15f)
            .withAnim(CrazyDiamondEntity.State.ITEM_TOSS)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.item_toss.followup.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.item_toss.followup.description")
            );

    public static final TossChargeMove<CrazyDiamondEntity> TOSS_CHARGE = new TossChargeMove<CrazyDiamondEntity>(70, 21, 40, 1.0f, 10)
            .withFollowup(TOSS)
            .withInfo(
                    Component.translatable("move.jcraft.crazy_diamond.default.item_toss.name"),
                    Component.translatable("move.jcraft.crazy_diamond.default.item_toss.description")
            );

    public static final StandData DATA = StandData.builder()
            .idleRotation(-45f)
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.crazydiamond"))
                    .proCount(4)
                    .conCount(2)
                    .freeSpace(Component.translatable("entity.jcraft.crazydiamond.info.freeSpace"))
                    .skinName(Component.translatable("entity.jcraft.crazydiamond.info.skin1"))
                    .build())
            .summonData(SummonData.of(JSoundRegistry.GE_SUMMON))
            .build()
            .withObtainable(true);

    public CrazyDiamondEntity(final Level world) {
        super(JStandTypeRegistry.CRAZY_DIAMOND.get(), world);

        auraColors = new Vector3f[]{
                new Vector3f(1.0f, 0.45f, 0.80f),
                new Vector3f(0.45f, 0.85f, 1.0f),
                new Vector3f(1.0f, 0.80f, 0.35f),
                new Vector3f(0.75f, 0.45f, 1.0f)
        };
    }

    @Override
    public @NonNull CrazyDiamondEntity getThis() {
        return this;
    }

    private static void registerDefaultMoves(final @NonNull MoveMap<CrazyDiamondEntity, CrazyDiamondEntity.State> moves) {
        moves.registerImmediate(MoveClass.LIGHT, PUNCH, State.LIGHT);
        moves.register(MoveClass.HEAVY, DIAMOND_SMASH, State.HEAVY);
        moves.register(MoveClass.BARRAGE, DORA_BARRAGE, State.BARRAGE);

        moves.register(MoveClass.SPECIAL1, RESTORATION_HAND, State.HEAL).withCrouchingVariant(State.HEAL);
        moves.register(MoveClass.SPECIAL2, WALL_BREAKER, State.WALL_BREAKER);
        moves.register(MoveClass.SPECIAL3, BEARING_SNAP, State.BEARING_SNAP);
        moves.register(MoveClass.ULTIMATE, UNBREAKABLE_RAGE, State.RAGE);

        moves.register(MoveClass.UTILITY, SHARD_LEAP, State.JUMP);

        moves.register(MoveClass.TOSS, TOSS_CHARGE, State.ITEM_TOSS_CHARGE).withFollowup(State.ITEM_TOSS);
    }

    @Override
    public boolean initMove(final MoveClass moveClass) {
        if (tryFollowUp(moveClass, MoveClass.LIGHT)) {
            return true;
        }
        return super.initMove(moveClass);
    }

    public enum State implements StandAnimationState<CrazyDiamondEntity> {
        IDLE(AzCommand.create(JCraft.BASE_CONTROLLER, "idle", AzPlayBehaviors.LOOP)),
        LIGHT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light", AzPlayBehaviors.PLAY_ONCE)),
        LIGHT_FOLLOWUP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light_followup", AzPlayBehaviors.PLAY_ONCE)),
        UPPERCUT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "uppercut", AzPlayBehaviors.PLAY_ONCE)),
        BLOCK(AzCommand.create(JCraft.BASE_CONTROLLER, "block", AzPlayBehaviors.LOOP)),
        HEAVY(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "heavy", AzPlayBehaviors.PLAY_ONCE)),
        BARRAGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "barrage", AzPlayBehaviors.LOOP)),
        HEAL(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "heal", AzPlayBehaviors.PLAY_ONCE)),
        WALL_BREAKER(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "wall_breaker", AzPlayBehaviors.PLAY_ONCE)),
        BEARING_SNAP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "bearing_snap", AzPlayBehaviors.PLAY_ONCE)),
        RAGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "rage", AzPlayBehaviors.PLAY_ONCE)),
        JUMP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "jump", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS_CHARGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow_charge", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow", AzPlayBehaviors.PLAY_ONCE));

        private final AzCommand animator;

        State(final @NonNull AzCommand animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(final @NonNull CrazyDiamondEntity attacker) {
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
