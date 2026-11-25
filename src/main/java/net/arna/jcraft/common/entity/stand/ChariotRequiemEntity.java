package net.arna.jcraft.common.entity.stand;

import lombok.NonNull;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.enums.BlockableType;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.stand.StandData;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandInfo;
import net.arna.jcraft.api.stand.SummonData;
import net.arna.jcraft.common.attack.actions.EffectAction;
import net.arna.jcraft.common.attack.moves.chariotrequiem.RequiemCounterAttack;
import net.arna.jcraft.common.attack.moves.chariotrequiem.SoulSwapAttack;
import net.arna.jcraft.common.attack.moves.shared.*;
import net.arna.jcraft.common.util.JParticleType;
import net.arna.jcraft.common.util.StandAnimationState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/**
 * The {@link StandEntity} for <a href="https://jojowiki.com/Chariot_Requiem">Chariot Requiem</a>.
 * Evolved from Silver Chariot using the Requiem Arrow. Specializes in soul manipulation,
 * somnolence crowd control, Stand-disabling counters, and spectral mobility.
 *
 * @see JStandTypeRegistry#CHARIOT_REQUIEM
 * @see net.arna.jcraft.client.renderer.entity.stands.ChariotRequiemRenderer ChariotRequiemRenderer
 */
public class ChariotRequiemEntity extends StandEntity<ChariotRequiemEntity, ChariotRequiemEntity.State> {
    public static final MoveSet<ChariotRequiemEntity, State> MOVE_SET = MoveSetManager.create(
            JStandTypeRegistry.CHARIOT_REQUIEM,
            ChariotRequiemEntity::registerMoves,
            ChariotRequiemEntity.class,
            State.class
    );

    public static final SimpleAttack<ChariotRequiemEntity> LIGHT_FOLLOWUP = new SimpleAttack<ChariotRequiemEntity>(
            0, 6, 14, 0.8f, 6.5f, 12, 1.6f, 1.0f, -0.1f)
            .withAnim(State.LIGHT_FOLLOWUP)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withAction(EffectAction.inflict(MobEffects.SLOWNESS, 40, 0, true, false))
            .withLaunch()
            .withBlockStun(4)
            .withExtraHitBox(0, 0.25, 1)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_strike.followup.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_strike.followup.description")
            );

    public static final SimpleAttack<ChariotRequiemEntity> SHADOW_STRIKE = SimpleAttack.<ChariotRequiemEntity>lightAttack(
            6, 11, 0.75f, 5.5f, 12, 0.25f, -0.1f)
            .withFollowup(LIGHT_FOLLOWUP)
            .withImpactSound(JSoundRegistry.IMPACT_1)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_strike.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_strike.description")
            );

    public static final KnockdownAttack<ChariotRequiemEntity> REQUIEM_SLAM = new KnockdownAttack<ChariotRequiemEntity>(
            0, 16, 26, 1.0f, 9.5f, 12, 2.0f, 1.3f, 0f, 35)
            .withSound(JSoundRegistry.SC_HEAVY)
            .withImpactSound(JSoundRegistry.IMPACT_9)
            .withAction(EffectAction.inflict(MobEffects.DARKNESS, 40, 0, true, false))
            .withHyperArmor()
            .withLaunch()
            .withExtraHitBox(1.25)
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.requiem_slam.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.requiem_slam.description")
            );

    public static final MainBarrageAttack<ChariotRequiemEntity> PHANTOM_FLURRY = new MainBarrageAttack<ChariotRequiemEntity>(
            280, 0, 36, 0.75f, 1.0f, 20, 2.0f, 0.25f, 0f, 3, Blocks.DEEPSLATE.defaultDestroyTime())
            .withSound(JSoundRegistry.SC_BARRAGE)
            .withImpactSound(JSoundRegistry.IMPACT_3)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.phantom_flurry.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.phantom_flurry.description")
            );

    public static final SimpleAttack<ChariotRequiemEntity> SOMNOLENCE = new SimpleAttack<ChariotRequiemEntity>(
            240, 12, 22, 0.5f, 4.0f, 24, 4.5f, 0.1f, 0f)
            .withSound(JSoundRegistry.TIME_ERASE)
            .withBlockableType(BlockableType.NON_BLOCKABLE_EFFECTS_ONLY)
            .withAction(EffectAction.inflict(MobEffects.SLOWNESS, 100, 2, true, false))
            .withAction(EffectAction.inflict(MobEffects.DARKNESS, 80, 0, true, false))
            .withAction(EffectAction.inflict(MobEffects.WEAKNESS, 80, 0, true, false))
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.somnolence.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.somnolence.description")
            );

    public static final SoulSwapAttack<ChariotRequiemEntity> SOUL_SWAP = new SoulSwapAttack<ChariotRequiemEntity>(
            260, 12, 22, 0.5f, 5.0f, 18, 4.0f, 0.15f, 0f)
            .withSound(JSoundRegistry.TIME_SKIP)
            .withImpactSound(JSoundRegistry.IMPACT_5)
            .withBlockableType(BlockableType.NON_BLOCKABLE_EFFECTS_ONLY)
            .withHitSpark(JParticleType.HIT_SPARK_2)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.soul_swap.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.soul_swap.description")
            );

    public static final RequiemCounterAttack STAND_REBELLION = new RequiemCounterAttack(
            400, 5, 34, 1.0f)
            .withSound(JSoundRegistry.SC_ARMOROFF)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.stand_rebellion.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.stand_rebellion.description")
            );

    public static final KnockdownAttack<ChariotRequiemEntity> REQUIEM_DOMINION = new KnockdownAttack<ChariotRequiemEntity>(
            640, 20, 34, 0.5f, 12.0f, 20, 6.0f, 0.8f, 0f, 45)
            .withSound(JSoundRegistry.TIME_ERASE)
            .withImpactSound(JSoundRegistry.IMPACT_10)
            .withHyperArmor()
            .withBlockableType(BlockableType.NON_BLOCKABLE_EFFECTS_ONLY)
            .withAction(EffectAction.inflict(JStatusRegistry.STANDLESS, 120, 0, true, false))
            .withAction(EffectAction.inflict(MobEffects.SLOWNESS, 120, 2, true, false))
            .withAction(EffectAction.inflict(MobEffects.DARKNESS, 100, 0, true, false))
            .withHitAnimation(CommonHitPropertyComponent.HitAnimation.CRUSH)
            .withHitSpark(JParticleType.HIT_SPARK_3)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.requiem_dominion.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.requiem_dominion.description")
            );

    public static final TimeSkipMove<ChariotRequiemEntity> SHADOW_STEP = new TimeSkipMove<ChariotRequiemEntity>(
            260, 12.0, 2, 4, 1.0f)
            .withParticles()
            .withSound(JSoundRegistry.TIME_SKIP)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_step.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.shadow_step.description")
            );

    public static final TossMove<ChariotRequiemEntity> TOSS = new TossMove<ChariotRequiemEntity>(0, 1, 1, 0.75f, 0.13f)
            .withAnim(State.ITEM_TOSS)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.item_toss.followup.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.item_toss.followup.description")
            );

    public static final TossChargeMove<ChariotRequiemEntity> TOSS_CHARGE = new TossChargeMove<ChariotRequiemEntity>(70, 21, 40, 1.0f, 10)
            .withFollowup(TOSS)
            .withInfo(
                    Component.translatable("move.jcraft.chariot_requiem.default.item_toss.name"),
                    Component.translatable("move.jcraft.chariot_requiem.default.item_toss.description")
            );

    public static final StandData DATA = StandData.builder()
            .idleRotation(-45f)
            .evolution(true)
            .info(StandInfo.builder()
                    .name(Component.translatable("entity.jcraft.chariot_requiem"))
                    .proCount(4)
                    .conCount(2)
                    .freeSpace(Component.translatable("entity.jcraft.chariot_requiem.info.freeSpace"))
                    .build())
            .summonData(SummonData.of(JSoundRegistry.SC_SUMMON))
            .build()
            .withObtainable(true);

    public ChariotRequiemEntity(final Level world) {
        super(JStandTypeRegistry.CHARIOT_REQUIEM.get(), world);

        auraColors = new Vector3f[]{
                new Vector3f(0.28f, 0.22f, 0.42f),
                new Vector3f(0.55f, 0.48f, 0.72f),
                new Vector3f(0.18f, 0.18f, 0.25f),
                new Vector3f(0.75f, 0.65f, 0.90f)
        };
    }

    private static void registerMoves(final MoveMap<ChariotRequiemEntity, State> moves) {
        moves.registerImmediate(MoveClass.LIGHT, SHADOW_STRIKE, State.LIGHT);
        moves.register(MoveClass.HEAVY, REQUIEM_SLAM, State.HEAVY);
        moves.register(MoveClass.BARRAGE, PHANTOM_FLURRY, State.BARRAGE);

        moves.register(MoveClass.SPECIAL1, SOMNOLENCE, State.SOMNOLENCE);
        moves.register(MoveClass.SPECIAL2, SOUL_SWAP, State.SOUL_SWAP);
        moves.register(MoveClass.SPECIAL3, STAND_REBELLION, State.COUNTER);
        moves.register(MoveClass.ULTIMATE, REQUIEM_DOMINION, State.DOMINION);

        moves.register(MoveClass.UTILITY, SHADOW_STEP, State.SHADOW_STEP);

        moves.register(MoveClass.TOSS, TOSS_CHARGE, State.ITEM_TOSS_CHARGE).withFollowup(State.ITEM_TOSS);
    }

    @Override
    public boolean initMove(final MoveClass moveClass) {
        if (tryFollowUp(moveClass, MoveClass.LIGHT)) {
            return true;
        }
        return super.initMove(moveClass);
    }

    @Override
    public @NonNull ChariotRequiemEntity getThis() {
        return this;
    }

    public enum State implements StandAnimationState<ChariotRequiemEntity> {
        IDLE(AzCommand.create(JCraft.BASE_CONTROLLER, "idle", AzPlayBehaviors.LOOP)),
        BLOCK(AzCommand.create(JCraft.BASE_CONTROLLER, "block", AzPlayBehaviors.LOOP)),
        LIGHT(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light", AzPlayBehaviors.PLAY_ONCE)),
        LIGHT_FOLLOWUP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "light_followup", AzPlayBehaviors.PLAY_ONCE)),
        HEAVY(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "heavy", AzPlayBehaviors.PLAY_ONCE)),
        BARRAGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "barrage", AzPlayBehaviors.LOOP)),
        SOMNOLENCE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "somnolence", AzPlayBehaviors.PLAY_ONCE)),
        SOUL_SWAP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "soul_swap", AzPlayBehaviors.PLAY_ONCE)),
        COUNTER(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "counter", AzPlayBehaviors.PLAY_ONCE)),
        COUNTER_MISS(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "counter_miss", AzPlayBehaviors.PLAY_ONCE)),
        DOMINION(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "dominion", AzPlayBehaviors.PLAY_ONCE)),
        SHADOW_STEP(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "shadow_step", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS_CHARGE(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow_charge", AzPlayBehaviors.PLAY_ONCE)),
        ITEM_TOSS(Attacks.createAnimationCommand(JCraft.BASE_CONTROLLER, "itemthrow", AzPlayBehaviors.PLAY_ONCE));

        private final AzCommand animator;

        State(final @NonNull AzCommand animator) {
            this.animator = animator;
        }

        @Override
        public void playAnimation(final @NonNull ChariotRequiemEntity attacker) {
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
