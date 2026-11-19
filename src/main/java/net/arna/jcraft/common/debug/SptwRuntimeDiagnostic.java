package net.arna.jcraft.common.debug;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.component.entity.CommonGrabComponent;
import net.arna.jcraft.api.component.entity.CommonTimeStopComponent;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler;
import net.arna.jcraft.common.component.impl.entity.CommonGrabComponentImpl;
import net.arna.jcraft.common.component.impl.entity.CommonTimeStopComponentImpl;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.projectile.ItemTossProjectile;
import net.arna.jcraft.common.entity.projectile.KnifeProjectile;
import net.arna.jcraft.common.entity.stand.SPTWEntity;
import net.arna.jcraft.common.tickable.Timestops;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.EvolutionItemHandler;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Live Minecraft 26.3 server runtime self-test for Star Platinum: The World (SPTW).
 * Inactive unless JVM property {@code -Djcraft.selftest.sptw=true} is passed.
 */
public final class SptwRuntimeDiagnostic {
    public static final boolean ENABLED = Boolean.getBoolean("jcraft.selftest.sptw");

    private static int phase = 0;
    private static int waitTicks = 0;
    private static boolean finished = false;

    private static ServerLevel world;
    private static Husk user;
    private static Husk target;
    private static Husk blockingTarget;
    private static SPTWEntity sptw;

    private static final double ARENA_X = 8.5;
    private static final double ARENA_Y = 120.0;
    private static final double ARENA_Z = 8.5;

    private static float savedHealth;
    private static double savedDouble;
    private static Vec3 savedVec;
    private static int savedInt;
    private static ItemTossProjectile trackedAnvil;
    private static Snowball trackedSnowball;
    private static ThrownSplashPotion trackedPotion;
    private static KnifeProjectile trackedKnife;
    private static Arrow enemyArrow;
    private static ItemTossProjectile ownProj;
    private static Arrow frozenArrow;
    private static final List<String> failures = new ArrayList<>();
    private static int checksPassed = 0;

    private SptwRuntimeDiagnostic() {}

    public static void tick(final MinecraftServer server) {
        if (!ENABLED || finished) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        try {
            runPhase(server);
        } catch (Throwable t) {
            failures.add("Phase " + phase + " threw exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            JCraft.LOGGER.error("[SPTW-DEBUG] Exception in phase {}", phase, t);
            finish(server);
        }
    }

    private static void assertTrue(final boolean cond, final String msg) {
        if (!cond) {
            failures.add("FAIL: " + msg);
            JCraft.LOGGER.error("[SPTW-DEBUG] FAIL: {}", msg);
        } else {
            checksPassed++;
            JCraft.LOGGER.info("[SPTW-DEBUG] PASS: {}", msg);
        }
    }

    private static void resetArena() {
        for (int dx = -12; dx <= 24; dx++) {
            for (int dz = -12; dz <= 12; dz++) {
                world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y - 1, ARENA_Z + dz), Blocks.STONE.defaultBlockState());
                for (int dy = 0; dy <= 10; dy++) {
                    world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y + dy, ARENA_Z + dz), Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private static void clearCooldownsAndResetPositions() {
        if (user != null) {
            CommonCooldownsComponent cds = JComponentPlatformUtils.getCooldowns(user);
            for (CooldownType type : CooldownType.values()) {
                cds.setCooldown(type, 0);
            }
            user.removeEffect(JStatusRegistry.DAZED.asHolder());
            user.setShiftKeyDown(false);
            user.setPos(ARENA_X, ARENA_Y, ARENA_Z);
            user.setYRot(-90.0f); // Facing +X
            user.setYHeadRot(-90.0f);
            user.setXRot(0.0f);
            user.setDeltaMovement(Vec3.ZERO);
        }
        if (sptw != null && sptw.isAlive()) {
            sptw.setRemote(false);
            sptw.wantToBlock = false;
            sptw.blocking = false;
            sptw.setMoveStun(0);
            sptw.setCurrentMove(null);
            sptw.setFree(false);
            sptw.setYRot(-90.0f);
            sptw.setYHeadRot(-90.0f);
            sptw.setXRot(0.0f);
            sptw.setPos(ARENA_X, ARENA_Y, ARENA_Z);
        }
        if (target != null && target.isAlive()) {
            target.setHealth(target.getMaxHealth());
            target.removeEffect(JStatusRegistry.DAZED.asHolder());
            target.removeAllEffects();
            target.setPos(ARENA_X + 2.0, ARENA_Y, ARENA_Z);
            target.setDeltaMovement(Vec3.ZERO);
            JComponentPlatformUtils.getTimeStopData(target).ifPresent(CommonTimeStopComponent::releaseNow);
            JComponentPlatformUtils.getGrab(target).endGrab();
        }
    }

    private static Husk spawnTestHusk(double x, double y, double z, boolean taggedStandless) {
        Husk h = new Husk(EntityTypes.HUSK, world);
        h.setPos(x, y, z);
        h.setNoAi(true);
        h.setPersistenceRequired();
        var maxHp = h.getAttribute(Attributes.MAX_HEALTH);
        if (maxHp != null) {
            maxHp.setBaseValue(200.0);
        }
        h.setHealth(200.0f);
        if (taggedStandless) {
            JComponentPlatformUtils.getStandComponent(h).setTagged(true);
        }
        world.addFreshEntity(h);
        return h;
    }

    private static void runPhase(final MinecraftServer server) {
        switch (phase) {
            case 0 -> {
                JCraft.LOGGER.info("[SPTW-DEBUG] === STARTING LIVE MC 26.3 SPTW BRUTAL RUNTIME DEBUG ===");
                world = server.overworld();
                // Force-load arena chunks
                world.setChunkForced(0, 0, true);
                world.setChunkForced(1, 0, true);
                resetArena();

                // 1. Evolution registry verification
                boolean hasSpToSptw = EvolutionItemHandler.getEvolutions().stream().anyMatch(e ->
                        e.item() == JItemRegistry.LIVING_ARROW.get()
                                && e.stand() == JStandTypeRegistry.STAR_PLATINUM.get()
                                && e.target() == JStandTypeRegistry.STAR_PLATINUM_THE_WORLD.get());
                assertTrue(hasSpToSptw, "1. Evolution registry contains Living Arrow: Star Platinum -> Star Platinum: The World");

                // 2. Spawn user + summon SPTW
                user = spawnTestHusk(ARENA_X, ARENA_Y, ARENA_Z, true);
                target = spawnTestHusk(ARENA_X + 2.0, ARENA_Y, ARENA_Z, true);
                CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(user);
                sc.setTypeAndSkin(JStandTypeRegistry.STAR_PLATINUM_THE_WORLD.get(), 0, false);
                StandEntity<?, ?> summoned = JCraft.summon(world, user);
                assertTrue(summoned instanceof SPTWEntity, "2a. JCraft.summon spawns live SPTWEntity");
                sptw = (SPTWEntity) summoned;
                assertTrue(sptw.getUser() == user, "2b. SPTWEntity user binding verified");
                assertTrue(sptw.getUserPose().equals(JStandTypeRegistry.STAR_PLATINUM.getId()),
                        "2c. SPTWEntity.getUserPose() falls back to jcraft:star_platinum pose (got " + sptw.getUserPose() + ")");
                MoveMap<SPTWEntity, SPTWEntity.State> mm = sptw.getMoveMap();
                for (MoveClass mc : new MoveClass[]{
                        MoveClass.LIGHT, MoveClass.HEAVY, MoveClass.BARRAGE,
                        MoveClass.SPECIAL1, MoveClass.SPECIAL2, MoveClass.SPECIAL3,
                        MoveClass.ULTIMATE, MoveClass.UTILITY, MoveClass.TOSS
                }) {
                    assertTrue(!mm.getEntries(mc).isEmpty(), "2d. SPTW moveset slot loaded: " + mc);
                }

                phase = 1;
                waitTicks = 2;
            }
            case 1 -> {
                // 3. Light Punch (M1) + Followup (M1->M1)
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                boolean started = sptw.initMove(MoveClass.LIGHT);
                assertTrue(started, "3a. LIGHT (Punch) initiated");
                phase = 2;
                waitTicks = 5; // duration 7, windup 5 -> moveStun == 2 (windupPoint) right after punch hits
            }
            case 2 -> {
                float afterPunch = target.getHealth();
                assertTrue(afterPunch < savedHealth, "3b. LIGHT (Punch) dealt damage (" + savedHealth + " -> " + afterPunch + ")");
                savedHealth = afterPunch;
                boolean followup = sptw.initMove(MoveClass.LIGHT);
                assertTrue(followup, "3c. LIGHT_FOLLOWUP (M1->M1) triggered cleanly");
                phase = 3;
                waitTicks = 8;
            }
            case 3 -> {
                assertTrue(target.getHealth() < savedHealth, "3d. LIGHT_FOLLOWUP dealt combo damage (" + savedHealth + " -> " + target.getHealth() + ")");

                // 4. Crouching Light (Ground Slam) - normal + blocking guard check
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                user.setShiftKeyDown(true);
                assertTrue(sptw.initMove(MoveClass.LIGHT), "4a. cr.LIGHT (SPTWGroundSlamAttack) initiated");
                phase = 4;
                waitTicks = 15;
            }
            case 4 -> {
                assertTrue(target.getHealth() < savedHealth, "4b. cr.LIGHT Ground Slam hit target");
                assertTrue(target.getDeltaMovement().lengthSqr() > 0.05 || target.getY() > ARENA_Y + 0.05 || target.getX() > ARENA_X + 2.1,
                        "4c. cr.LIGHT Ground Slam launched unblocked target");

                // 5. Barrage (multi-hit)
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                assertTrue(sptw.initMove(MoveClass.BARRAGE), "5a. BARRAGE initiated");
                phase = 5;
                waitTicks = 25;
            }
            case 5 -> {
                float dmg = savedHealth - target.getHealth();
                // With MC 26.3 invulnerableTime bypass fixed, 8 rapid barrage punches in 25 ticks must deal >= 15 dmg (vs ~3.89 before!).
                assertTrue(dmg >= 15.0f, "5b. BARRAGE dealt rapid multi-hit damage without MC 26.3 i-frame blocking (total dmg=" + dmg + ")");

                // 6. Heavy (Star Breaker) + cr.Heavy (Ground Breaker BlockBreakingAttack + Chest preservation)
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                assertTrue(sptw.initMove(MoveClass.HEAVY), "6a. HEAVY (Star Breaker) initiated");
                phase = 6;
                waitTicks = 24;
            }
            case 6 -> {
                assertTrue(target.getHealth() < savedHealth, "6b. HEAVY Star Breaker dealt heavy damage (" + (savedHealth - target.getHealth()) + ")");

                clearCooldownsAndResetPositions();
                BlockPos stonePos = BlockPos.containing(ARENA_X + 2, ARENA_Y, ARENA_Z);
                BlockPos chestPos = BlockPos.containing(ARENA_X + 2, ARENA_Y, ARENA_Z + 1);
                world.setBlockAndUpdate(stonePos, Blocks.STONE.defaultBlockState());
                world.setBlockAndUpdate(chestPos, Blocks.CHEST.defaultBlockState());
                user.setShiftKeyDown(true);
                assertTrue(sptw.initMove(MoveClass.HEAVY), "6c. cr.HEAVY (Ground Breaker) initiated");
                phase = 7;
                waitTicks = 23;
            }
            case 7 -> {
                BlockPos stonePos = BlockPos.containing(ARENA_X + 2, ARENA_Y, ARENA_Z);
                BlockPos chestPos = BlockPos.containing(ARENA_X + 2, ARENA_Y, ARENA_Z + 1);
                List<FallingBlockEntity> falling = world.getEntitiesOfClass(FallingBlockEntity.class,
                        new AABB(ARENA_X - 5, ARENA_Y - 5, ARENA_Z - 5, ARENA_X + 10, ARENA_Y + 10, ARENA_Z + 10));
                assertTrue(world.getBlockState(stonePos).isAir() || !falling.isEmpty(),
                        "6d. cr.HEAVY Ground Breaker broke STONE block into FallingBlockEntity");
                assertTrue(world.getBlockState(chestPos).is(Blocks.CHEST),
                        "6e. cr.HEAVY Ground Breaker preserved CHEST BlockEntity intact");
                falling.forEach(Entity::discard);
                world.setBlockAndUpdate(chestPos, Blocks.AIR.defaultBlockState());

                // 7. SP1 Time Strike (Normal + Crouching Turnaround + Pilot Mode)
                clearCooldownsAndResetPositions();
                target.setPos(ARENA_X + 4.0, ARENA_Y, ARENA_Z);
                savedDouble = user.getX();
                savedHealth = target.getHealth();
                assertTrue(sptw.initMove(MoveClass.SPECIAL1), "7a. SP1 Time Strike initiated");
                phase = 8;
                waitTicks = 10;
            }
            case 8 -> {
                assertTrue(user.getX() > savedDouble + 1.5, "7b. SP1 Time Strike teleported user forward (dx=" + (user.getX() - savedDouble) + ")");
                assertTrue(target.getHealth() < savedHealth, "7c. SP1 Time Strike hit target after teleport");

                // Crouching Time Strike turnaround
                clearCooldownsAndResetPositions();
                float startYaw = user.getYRot();
                user.setShiftKeyDown(true);
                assertTrue(sptw.initMove(MoveClass.SPECIAL1), "7d. cr.SP1 Time Strike (turnaround) initiated");
                phase = 9;
                waitTicks = 8;
            }
            case 9 -> {
                float endYaw = net.minecraft.util.Mth.wrapDegrees(user.getYRot());
                assertTrue(Math.abs(net.minecraft.util.Mth.degreesDifference(-90.0f, endYaw)) > 120.0f,
                        "7e. cr.SP1 Time Strike turned user around (yaw=" + endYaw + ")");

                // Pilot Mode Time Strike (teleports Stand, NOT parked user!)
                clearCooldownsAndResetPositions();
                sptw.setRemote(true);
                sptw.setPos(ARENA_X + 1.0, ARENA_Y, ARENA_Z);
                savedVec = user.position();
                savedDouble = sptw.getX();
                assertTrue(sptw.initMove(MoveClass.SPECIAL1), "7f. Pilot Mode SP1 Time Strike initiated");
                phase = 10;
                waitTicks = 8;
            }
            case 10 -> {
                assertTrue(user.position().distanceTo(savedVec) < 0.1, "7g. Pilot Mode SP1 kept user body parked");
                assertTrue(sptw.getX() > savedDouble + 1.5, "7h. Pilot Mode SP1 teleported piloted SPTW forward (dx=" + (sptw.getX() - savedDouble) + ")");

                // 8. SP2 Backhand
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                assertTrue(sptw.initMove(MoveClass.SPECIAL2), "8a. SP2 Backhand initiated");
                phase = 11;
                waitTicks = 11;
            }
            case 11 -> {
                JCraft.LOGGER.info("[SPTW-DEBUG] INFO 8b: userPos={} sptwPos={} sptwPass={} targetPos={} hp={}->{} curMove={}",
                        user.position(), sptw.position(), sptw.isPassenger(), target.position(), savedHealth, target.getHealth(), sptw.getCurrentMove());
                assertTrue(target.getHealth() < savedHealth, "8b. SP2 Backhand hit target");
                assertTrue(target.hasEffect(JStatusRegistry.DAZED.asHolder()), "8c. SP2 Backhand applied high stun");

                // 9. SP3 Grab (normal)
                clearCooldownsAndResetPositions();
                savedHealth = target.getHealth();
                assertTrue(sptw.initMove(MoveClass.SPECIAL3), "9a. SP3 Grab (What an Ugly Watch) initiated");
                phase = 12;
                waitTicks = 10; // windup 8 -> grabbed on tick 8
            }
            case 12 -> {
                CommonGrabComponent grab = JComponentPlatformUtils.getGrab(target);
                assertTrue(grab instanceof CommonGrabComponentImpl impl && impl.getAttacker() == sptw && impl.getDuration() > 0,
                        "9b. SP3 Grab actively holding target");
                savedHealth = target.getHealth(); // snapshot HP AFTER initial grab hit so 9c proves GRAB_HIT uppercut lands!
                phase = 13;
                waitTicks = 20; // wait for GRAB_HIT uppercut
            }
            case 13 -> {
                assertTrue(target.getHealth() < savedHealth - 4.0f,
                        "9c. SP3 GRAB_HIT uppercut landed after Grab despite MC 26.3 i-frames (hp " + savedHealth + " -> " + target.getHealth() + ")");

                // 10. Utility Time Skip (Open air + Wall safety + Ceiling safety + Pilot Mode)
                clearCooldownsAndResetPositions();
                user.fallDistance = 12.0f;
                assertTrue(sptw.initMove(MoveClass.UTILITY), "10a. UTILITY Time Skip (open air) initiated");
                phase = 14;
                waitTicks = 4;
            }
            case 14 -> {
                assertTrue(user.getX() > ARENA_X + 10.0, "10b. Time Skip traveled ~14m open air (x=" + user.getX() + ")");
                assertTrue(user.fallDistance < 1.0f, "10c. Time Skip reset fall distance");
                assertTrue(JComponentPlatformUtils.getCooldowns(user).getCooldown(CooldownType.STAND_ULTIMATE) >= 50,
                        "10d. Time Skip enforced 3s minimum Ult cooldown");

                // Wall safety
                clearCooldownsAndResetPositions();
                BlockPos wallPos = BlockPos.containing(ARENA_X + 4, ARENA_Y, ARENA_Z);
                for (int y = 0; y <= 2; y++) {
                    world.setBlockAndUpdate(wallPos.above(y), Blocks.STONE.defaultBlockState());
                }
                assertTrue(sptw.initMove(MoveClass.UTILITY), "10e. Time Skip into solid STONE wall initiated");
                phase = 15;
                waitTicks = 4;
            }
            case 15 -> {
                assertTrue(!world.getBlockState(user.blockPosition()).isSuffocating(world, user.blockPosition()),
                        "10f. Time Skip into wall backed off safely without clipping/suffocation (userPos=" + user.position() + ")");
                BlockPos wallPos = BlockPos.containing(ARENA_X + 4, ARENA_Y, ARENA_Z);
                for (int y = 0; y <= 2; y++) {
                    world.setBlockAndUpdate(wallPos.above(y), Blocks.AIR.defaultBlockState());
                }

                // Ceiling safety
                clearCooldownsAndResetPositions();
                BlockPos ceilPos = BlockPos.containing(ARENA_X, ARENA_Y + 4, ARENA_Z);
                world.setBlockAndUpdate(ceilPos, Blocks.STONE.defaultBlockState());
                user.setXRot(-90.0f); // Straight up
                assertTrue(sptw.initMove(MoveClass.UTILITY), "10g. Time Skip straight up into ceiling initiated");
                phase = 16;
                waitTicks = 4;
            }
            case 16 -> {
                assertTrue(user.getEyeY() < ARENA_Y + 4.0, "10h. Time Skip into ceiling backed off below stone (eyeY=" + user.getEyeY() + ")");
                world.setBlockAndUpdate(BlockPos.containing(ARENA_X, ARENA_Y + 4, ARENA_Z), Blocks.AIR.defaultBlockState());

                // Pilot Mode Time Skip
                clearCooldownsAndResetPositions();
                sptw.setRemote(true);
                sptw.setPos(ARENA_X + 1.0, ARENA_Y, ARENA_Z);
                savedVec = user.position();
                assertTrue(sptw.initMove(MoveClass.UTILITY), "10i. Pilot Mode Time Skip initiated");
                phase = 17;
                waitTicks = 4;
            }
            case 17 -> {
                assertTrue(user.position().distanceTo(savedVec) < 0.1, "10j. Pilot Mode Time Skip kept user parked");
                assertTrue(sptw.getX() > ARENA_X + 10.0, "10k. Pilot Mode Time Skip teleported SPTW (x=" + sptw.getX() + ")");

                // 11. Item Toss (normal charge -> release + onCancel drop & hand clear)
                clearCooldownsAndResetPositions();
                user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COBBLESTONE, 2));
                sptw.setHolding(true);
                sptw.setHoldingType(MoveInputType.TOSS);
                assertTrue(sptw.initMove(MoveClass.TOSS), "11a. TOSS_CHARGE initiated with cobblestone");
                assertTrue(sptw.getItemInHand(InteractionHand.MAIN_HAND).is(Items.COBBLESTONE),
                        "11b. SPTW took 1 cobblestone into main hand");
                assertTrue(user.getItemInHand(InteractionHand.MAIN_HAND).getCount() == 1,
                        "11c. User stack shrunk to 1");
                phase = 18;
                waitTicks = 12; // charge past minimumCharge=10
            }
            case 18 -> {
                // Release toss
                sptw.setHolding(false);
                phase = 19;
                waitTicks = 4;
            }
            case 19 -> {
                List<ItemTossProjectile> projs = world.getEntitiesOfClass(ItemTossProjectile.class,
                        new AABB(ARENA_X - 20, ARENA_Y - 10, ARENA_Z - 20, ARENA_X + 30, ARENA_Y + 15, ARENA_Z + 20));
                assertTrue(!projs.isEmpty() || sptw.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                        "11d. TOSS released ItemTossProjectile and emptied Stand hand");
                projs.forEach(Entity::discard);

                // Cancel toss check (must clear Stand hand and drop 1 item)
                clearCooldownsAndResetPositions();
                user.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE, 1));
                sptw.setHolding(true);
                sptw.setHoldingType(MoveInputType.TOSS);
                sptw.initMove(MoveClass.TOSS);
                assertTrue(sptw.getItemInHand(InteractionHand.MAIN_HAND).is(Items.APPLE), "11e. TOSS_CHARGE holds apple before cancel");
                sptw.cancelMove(true);
                assertTrue(sptw.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(), "11f. TOSS_CHARGE.onCancel cleared Stand hand (no dupe)");
                List<ItemEntity> dropped = world.getEntitiesOfClass(ItemEntity.class,
                        new AABB(ARENA_X - 4, ARENA_Y - 4, ARENA_Z - 4, ARENA_X + 4, ARENA_Y + 4, ARENA_Z + 4),
                        ie -> ie.getItem().is(Items.APPLE));
                assertTrue(dropped.size() == 1, "11g. TOSS_CHARGE.onCancel dropped exactly 1 apple (found " + dropped.size() + ")");
                dropped.forEach(Entity::discard);
                sptw.setHolding(false);

                // 12. ULTIMATE TIME STOP — Config duration + Multi-dim + Freeze + Grab in TS + Ground Slam in TS + Kinetic Toss + Early Resume!
                clearCooldownsAndResetPositions();
                savedInt = JServerConfig.SPTW_TIME_STOP_DURATION.getValue();
                JServerConfig.SPTW_TIME_STOP_DURATION.setValue(80); // 4s test window
                assertTrue(sptw.initMove(MoveClass.ULTIMATE), "12a. ULTIMATE Time Stop initiated");
                phase = 20;
                waitTicks = 7; // windup=5 -> TS active
            }
            case 20 -> {
                assertTrue(Timestops.isUserOfActiveTimestop(user), "12b. Timestops active for SPTW user");
                assertTrue(sptw.getTsTime() > 60, "12c. Time Stop respected JServerConfig.SPTW_TIME_STOP_DURATION=80 (tsTime=" + sptw.getTsTime() + ")");
                assertTrue(Timestops.isInTSRange(Level.OVERWORLD, user.blockPosition()), "12d. Overworld block pos IS in TS range");
                assertTrue(!Timestops.isInTSRange(Level.NETHER, user.blockPosition()), "12e. Nether same block pos is NOT in TS range");
                assertTrue(!world.shouldTickBlocksAt(user.blockPosition()), "12f. LevelMixin freezes block entities in Overworld TS");
                assertTrue(!sptw.initMove(MoveClass.UTILITY), "12g. Time Skip blocked while Time Stop is active");

                phase = 21;
                waitTicks = 3; // let Timestops.tick freeze target
            }
            case 21 -> {
                assertTrue(JUtils.isAffectedByTimeStop(target), "12h. Target mob is frozen in Time Stop");
                savedVec = target.position();

                // Test cr.LIGHT Ground Slam inside Time Stop -> must NOT push target mid-TS, must queue into totalVelocity!
                user.setShiftKeyDown(true);
                sptw.setMoveStun(0);
                sptw.setCurrentMove(null);
                sptw.initMove(MoveClass.LIGHT);
                phase = 22;
                waitTicks = 14;
            }
            case 22 -> {
                assertTrue(target.position().distanceTo(savedVec) < 0.05,
                        "12i. cr.LIGHT Ground Slam did NOT push frozen target mid-Time-Stop");
                Optional<CommonTimeStopComponent> tsOpt = JComponentPlatformUtils.getTimeStopData(target);
                assertTrue(tsOpt.isPresent() && tsOpt.get() instanceof CommonTimeStopComponentImpl impl && impl.getTotalVelocity().lengthSqr() > 0.1,
                        "12j. cr.LIGHT Ground Slam queued knockback into totalVelocity during Time Stop");

                // Test cr.SP3 Grab2 inside Time Stop!
                user.setShiftKeyDown(true);
                sptw.setMoveStun(0);
                sptw.setCurrentMove(null);
                JComponentPlatformUtils.getCooldowns(user).setCooldown(CooldownType.STAND_SP3, 0);
                assertTrue(sptw.initMove(MoveClass.SPECIAL3), "12k. cr.SP3 Grab2 initiated INSIDE Time Stop");
                phase = 23;
                waitTicks = 11; // windup 8 -> grab active and ticked for 3 ticks INSIDE TS
            }
            case 23 -> {
                CommonGrabComponent grab = JComponentPlatformUtils.getGrab(target);
                JCraft.LOGGER.info("[SPTW-DEBUG] INFO 12l: sptwPos={} targetPos={} sptwMove={} moveStun={} grabAttacker={} grabDur={} targetDazed={}",
                        sptw.position(), target.position(), sptw.getCurrentMove(), sptw.getMoveStun(),
                        grab instanceof CommonGrabComponentImpl g1 ? g1.getAttacker() : null,
                        grab instanceof CommonGrabComponentImpl g2 ? g2.getDuration() : -1,
                        target.getEffect(JStatusRegistry.DAZED.asHolder()));
                assertTrue(grab instanceof CommonGrabComponentImpl impl && impl.getAttacker() == sptw && impl.getDuration() > 0 && impl.getDuration() < 15,
                        "12l. Grab ticked & stayed active INSIDE Time Stop (no premature DAZED-3 cancel, remaining=" +
                                (grab instanceof CommonGrabComponentImpl g ? g.getDuration() : -1) + ")");

                // Face +Z (open air, away from target at +X) so tossed projectiles fly freely > 1.8m and suspend mid-air!
                user.setYRot(0.0f);
                user.setYHeadRot(0.0f);
                sptw.setYRot(0.0f);
                sptw.setYHeadRot(0.0f);
                // Test Kinetic Time Stop for SPTW tossed items: Anvil (ItemTossProjectile), Snowball, Splash Potion, Knife!
                trackedAnvil = (ItemTossProjectile) JUtils.tossItem(sptw, world, new ItemStack(Items.ANVIL), 1.5f, 0.0f, false);
                trackedSnowball = (Snowball) JUtils.tossItem(sptw, world, new ItemStack(Items.SNOWBALL), 1.5f, 0.0f, false);
                trackedPotion = (ThrownSplashPotion) JUtils.tossItem(sptw, world, new ItemStack(Items.SPLASH_POTION), 1.5f, 0.0f, false);
                trackedKnife = (KnifeProjectile) JUtils.tossItem(sptw, world, new ItemStack(JItemRegistry.KNIFE.get()), 1.5f, 0.0f, false);
                assertTrue(TimeStopKineticPhysicsHandler.isOwnedByTimestopUser(trackedAnvil, user),
                        "12m. Kinetic TS recognizes SPTW-owned ItemTossProjectile (Anvil)");
                assertTrue(TimeStopKineticPhysicsHandler.isOwnedByTimestopUser(trackedSnowball, user),
                        "12n. Kinetic TS recognizes SPTW-tossed Snowball");
                assertTrue(TimeStopKineticPhysicsHandler.isOwnedByTimestopUser(trackedPotion, user),
                        "12o. Kinetic TS recognizes SPTW-tossed Splash Potion");
                assertTrue(TimeStopKineticPhysicsHandler.isOwnedByTimestopUser(trackedKnife, user),
                        "12p. Kinetic TS recognizes SPTW-tossed Knife");

                phase = 24;
                waitTicks = 12; // let them travel ~1.8m and decelerate into suspension
            }
            case 24 -> {
                int suspendedCount = TimeStopKineticPhysicsHandler.getSuspendedCountForUser(user.getUUID());
                JCraft.LOGGER.info("[SPTW-DEBUG] INFO 12q: anvilAlive={} anvilPos={} anvilVel={} anvilNoGrav={} anvilInGround={} snowAlive={} potAlive={} knifeAlive={} count={}",
                        trackedAnvil.isAlive(), trackedAnvil.position(), trackedAnvil.getDeltaMovement(), trackedAnvil.isNoGravity(), trackedAnvil.isInGround(),
                        trackedSnowball.isAlive(), trackedPotion.isAlive(), trackedKnife.isAlive(), suspendedCount);
                assertTrue(suspendedCount >= 4, "12q. All 4 SPTW projectiles tracked & suspended in Kinetic Time Stop (count=" + suspendedCount + ")");
                assertTrue(trackedAnvil.isNoGravity() && trackedAnvil.getDeltaMovement().lengthSqr() < 0.01,
                        "12r. SPTW Anvil suspended mid-air with NoGravity=true");

                // Test velocity cap during Time Stop
                JComponentPlatformUtils.getTimeStopData(target).ifPresent(ts -> {
                    for (int i = 0; i < 20; i++) {
                        ts.addTotalVelocity(new Vec3(1.0, 0.5, 0.0));
                    }
                });
                Optional<CommonTimeStopComponent> tsCapOpt = JComponentPlatformUtils.getTimeStopData(target);
                assertTrue(tsCapOpt.isPresent() && tsCapOpt.get() instanceof CommonTimeStopComponentImpl impl
                                && impl.getTotalVelocity().length() <= CommonTimeStopComponentImpl.MAX_ACCUMULATED_LAUNCH_SPEED + 1.0E-4,
                        "12s. Time Stop totalVelocity properly capped at <= " + CommonTimeStopComponentImpl.MAX_ACCUMULATED_LAUNCH_SPEED);

                // Test EARLY RESUME via initMove(MoveClass.ULTIMATE)
                sptw.setMoveStun(0);
                sptw.setCurrentMove(null);
                assertTrue(sptw.initMove(MoveClass.ULTIMATE), "12t. Early Resume via ULTIMATE triggered");
                assertTrue(!Timestops.isUserOfActiveTimestop(user), "12u. Early Resume immediately stopped Timestops");
                assertTrue(sptw.getTsTime() == 0, "12v. Early Resume reset sptw.tsTime to 0");
                assertTrue(!JUtils.isAffectedByTimeStop(target), "12w. Early Resume immediately released frozen target");
                assertTrue(target.getDeltaMovement().lengthSqr() > 0.25, "12x. Released target received accumulated launch velocity");
                assertTrue(JComponentPlatformUtils.getTimeStopData(target).get() instanceof CommonTimeStopComponentImpl impl2
                                && impl2.getTotalVelocity().equals(Vec3.ZERO),
                        "12y. totalVelocity reset to Vec3.ZERO after release");
                assertTrue(TimeStopKineticPhysicsHandler.getSuspendedCountForUser(user.getUUID()) == 0,
                        "12z. Early Resume released all suspended kinetic projectiles (count=0)");
                assertTrue(!trackedAnvil.isNoGravity() && trackedAnvil.getDeltaMovement().lengthSqr() > 0.5,
                        "12aa. Released SPTW Anvil restored gravity & launched with +25% kinetic velocity");

                // Cleanup projectiles & restore config
                JServerConfig.SPTW_TIME_STOP_DURATION.setValue(savedInt);
                if (trackedAnvil != null) trackedAnvil.discard();
                if (trackedSnowball != null) trackedSnowball.discard();
                if (trackedPotion != null) trackedPotion.discard();
                if (trackedKnife != null) trackedKnife.discard();

                // 13. Guard / Block + Deflection safety + Abnormal TS cleanup on Stand removal & User death!
                clearCooldownsAndResetPositions();
                sptw.wantToBlock = true;
                sptw.tryBlock();
                enemyArrow = new Arrow(world, target, new ItemStack(Items.ARROW), null);
                enemyArrow.setPos(sptw.getX(), sptw.getY() + 0.5, sptw.getZ());
                enemyArrow.setDeltaMovement(new Vec3(-1.0, 0.0, 0.0));
                world.addFreshEntity(enemyArrow);

                ownProj = new ItemTossProjectile(sptw, world, new ItemStack(Items.IRON_INGOT));
                ownProj.setPos(sptw.getX(), sptw.getY() + 0.5, sptw.getZ());
                ownProj.setDeltaMovement(new Vec3(1.0, 0.0, 0.0));
                world.addFreshEntity(ownProj);

                sptw.standBlock();
                assertTrue(enemyArrow.getDeltaMovement().x > 0.0, "13a. SPTW Block deflected enemy Arrow");
                assertTrue(ownProj.getDeltaMovement().x > 0.5, "13b. SPTW Block did NOT deflect own Stand projectile");
                enemyArrow.discard();
                ownProj.discard();
                sptw.wantToBlock = false;
                sptw.tryUnblock();

                // Abnormal TS cleanup test: Stand removal (simulating guard crush)
                JCraft.beginTimestop(user, sptw.position(), world, 40);
                sptw.setTsTime(40);
                assertTrue(Timestops.isUserOfActiveTimestop(user), "13c. TS active before Stand removal");
                sptw.discard(); // triggers StandEntity.remove(DISCARDED)
                assertTrue(!Timestops.isUserOfActiveTimestop(user), "13d. Stand removal/guard-crush cleanly stopped active Time Stop");

                // Abnormal TS cleanup test: User death while TS active
                JCraft.beginTimestop(user, user.position(), world, 40);
                assertTrue(Timestops.isUserOfActiveTimestop(user), "13e. TS active before User death");
                user.kill(world);
                phase = 25;
                waitTicks = 2; // let Timestops.tick + death event run
            }
            case 25 -> {
                assertTrue(!Timestops.isUserOfActiveTimestop(user), "13f. User death cleanly stopped Time Stop with zero leaked entries");
                finish(server);
            }
            default -> finish(server);
        }
    }

    private static void finish(final MinecraftServer server) {
        finished = true;
        if (sptw != null && !sptw.isRemoved()) sptw.discard();
        if (target != null && !target.isRemoved()) target.discard();
        if (blockingTarget != null && !blockingTarget.isRemoved()) blockingTarget.discard();
        if (user != null && !user.isRemoved()) user.discard();
        if (world != null) {
            world.setChunkForced(0, 0, false);
            world.setChunkForced(1, 0, false);
        }
        JCraft.LOGGER.info("[SPTW-DEBUG] === SUMMARY: PASSED={} FAILED={} ===", checksPassed, failures.size());
        for (String f : failures) {
            JCraft.LOGGER.error("[SPTW-DEBUG] {}", f);
        }
        if (failures.isEmpty()) {
            JCraft.LOGGER.info("[SPTW-DEBUG] ALL_SPTW_CHECKS_PASSED");
        } else {
            JCraft.LOGGER.error("[SPTW-DEBUG] SPTW_CHECKS_HAD_FAILURES");
        }
        server.halt(false);
    }
}
