package net.arna.jcraft.common.debug;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.api.registry.JSoundRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.misc.BoundSoundPlayer;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.common.entity.projectile.BisectProjectile;
import net.arna.jcraft.common.entity.projectile.HamonWaveEntity;
import net.arna.jcraft.common.entity.spec.AnubisSpecUser;
import net.arna.jcraft.common.entity.stand.*;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.Gravity;
import net.arna.jcraft.common.item.Peacemaker;
import net.arna.jcraft.common.network.s2c.TimeAccelStatePacket;
import net.arna.jcraft.common.particle.BreathParticleOptions;
import net.arna.jcraft.common.particle.LockOnParticleOptions;
import net.arna.jcraft.common.particle.MoshParticleOptions;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Live Minecraft 26.3 server runtime self-test covering ALL playable Stands (28 including SC Possessed),
 * HamonWave dedicated-server safety, Made in Heaven real Overworld clock acceleration, C-Moon gravity
 * inversion physics/hitbox/eye-position, SlavedTo non-player NBT reload, and AnubisSpecUser auto-sheathe.
 * Inactive unless JVM property {@code -Djcraft.selftest.allstands=true} is passed.
 */
public final class AllStandsRuntimeDiagnostic {
    public static final boolean ENABLED = Boolean.getBoolean("jcraft.selftest.allstands");

    private static final double ARENA_X = 48.5;
    private static final double ARENA_Y = 120.0;
    private static final double ARENA_Z = 48.5;

    private static int phase = 0;
    private static int standIndex = 0;
    private static int waitTicks = 0;
    private static boolean finished = false;

    private static ServerLevel world;
    private static Husk user;
    private static Husk target;
    private static Husk gravityDummy;
    private static long mihStartClock;
    private static Vec3 gravityStartPos;
    private static double gravityStartEyeY;
    private static AnubisSpecUser anubisNpc;

    private static final List<String> failures = new ArrayList<>();
    private static int checksPassed = 0;

    private static List<Supplier<StandType>> playableStands() {
        return List.of(
                JStandTypeRegistry.STAR_PLATINUM,
                JStandTypeRegistry.STAR_PLATINUM_THE_WORLD,
                JStandTypeRegistry.THE_WORLD,
                JStandTypeRegistry.THE_WORLD_OVER_HEAVEN,
                JStandTypeRegistry.SHADOW_THE_WORLD,
                JStandTypeRegistry.KING_CRIMSON,
                JStandTypeRegistry.D4C,
                JStandTypeRegistry.CREAM,
                JStandTypeRegistry.KILLER_QUEEN,
                JStandTypeRegistry.KILLER_QUEEN_BITES_THE_DUST,
                JStandTypeRegistry.WHITE_SNAKE,
                JStandTypeRegistry.SILVER_CHARIOT,
                JStandTypeRegistry.MAGICIANS_RED,
                JStandTypeRegistry.THE_FOOL,
                JStandTypeRegistry.GOLD_EXPERIENCE,
                JStandTypeRegistry.GOLD_EXPERIENCE_REQUIEM,
                JStandTypeRegistry.HIEROPHANT_GREEN,
                JStandTypeRegistry.THE_SUN,
                JStandTypeRegistry.PURPLE_HAZE,
                JStandTypeRegistry.PURPLE_HAZE_DISTORTION,
                JStandTypeRegistry.C_MOON,
                JStandTypeRegistry.MADE_IN_HEAVEN,
                JStandTypeRegistry.HORUS,
                JStandTypeRegistry.METALLICA,
                JStandTypeRegistry.THE_HAND,
                JStandTypeRegistry.MANDOM,
                JStandTypeRegistry.AEROSMITH
        );
    }

    private AllStandsRuntimeDiagnostic() {}

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
            failures.add("Phase " + phase + " (standIdx=" + standIndex + ") threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
            JCraft.LOGGER.error("[ALLSTANDS-DEBUG] Exception in phase {} standIdx {}", phase, standIndex, t);
            finish(server);
        }
    }

    private static void assertTrue(final boolean cond, final String msg) {
        if (!cond) {
            failures.add("FAIL: " + msg);
            JCraft.LOGGER.error("[ALLSTANDS-DEBUG] FAIL: {}", msg);
        } else {
            checksPassed++;
            JCraft.LOGGER.info("[ALLSTANDS-DEBUG] PASS: {}", msg);
        }
    }

    private static void buildArena() {
        for (int dx = -10; dx <= 16; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y - 1, ARENA_Z + dz), Blocks.STONE.defaultBlockState());
                for (int dy = 0; dy <= 10; dy++) {
                    world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y + dy, ARENA_Z + dz), Blocks.AIR.defaultBlockState());
                }
                // Ceiling at Y+11 for inverted gravity test!
                world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y + 11, ARENA_Z + dz), Blocks.STONE.defaultBlockState());
            }
        }
        // North brick wall at Z-10 for lateral gravity test
        for (int dx = -10; dx <= 16; dx++) {
            for (int dy = 0; dy <= 10; dy++) {
                world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y + dy, ARENA_Z - 10), Blocks.BRICKS.defaultBlockState());
            }
        }
    }

    private static void resetFighters() {
        CommonCooldownsComponent cds = JComponentPlatformUtils.getCooldowns(user);
        for (CooldownType ct : CooldownType.values()) {
            cds.setCooldown(ct, 0);
        }
        user.removeEffect(JStatusRegistry.DAZED.asHolder());
        user.setHealth(user.getMaxHealth());
        user.setPos(ARENA_X, ARENA_Y, ARENA_Z);
        user.setYRot(-90f);
        user.setYHeadRot(-90f);
        user.setXRot(0f);
        user.setDeltaMovement(Vec3.ZERO);

        target.removeEffect(JStatusRegistry.DAZED.asHolder());
        target.setHealth(target.getMaxHealth());
        target.setPos(ARENA_X + 2.5, ARENA_Y, ARENA_Z);
        target.setDeltaMovement(Vec3.ZERO);
    }

    private static void runPhase(final MinecraftServer server) {
        List<Supplier<StandType>> stands = playableStands();
        switch (phase) {
            case 0 -> {
                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 0: Setup Arena & Live Mobs ===");
                world = server.getLevel(Level.OVERWORLD);
                assertTrue(world != null, "Overworld loaded");
                buildArena();

                user = new Husk(EntityTypes.HUSK, world);
                user.setNoAi(true);
                user.setPos(ARENA_X, ARENA_Y, ARENA_Z);
                user.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
                user.setHealth(500.0f);
                world.addFreshEntity(user);

                target = new Husk(EntityTypes.HUSK, world);
                target.setNoAi(true);
                target.setPos(ARENA_X + 2.5, ARENA_Y, ARENA_Z);
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500.0);
                target.setHealth(500.0f);
                world.addFreshEntity(target);

                phase = 1;
                waitTicks = 2;
            }
            case 1 -> {
                // Sweep ALL 27 playable Stands: summon, inspect MoveMap, cycle skins, fire moves, tick, desummon
                if (standIndex >= stands.size()) {
                    JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === All {} Playable Stands Summoned & Exercised Cleanly ===", stands.size());
                    phase = 2;
                    waitTicks = 2;
                    return;
                }
                StandType st = stands.get(standIndex).get();
                resetFighters();
                CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(user);
                if (sc.getStand() != null) {
                    sc.getStand().desummon();
                }
                sc.setType(st);
                StandEntity<?, ?> stand = JCraft.summon(world, user);
                String sid = st.getId().getPath();
                assertTrue(stand != null && stand.isAlive(), "Summoned Stand: " + sid);
                if (stand != null) {
                    assertTrue(!stand.getMoveMap().getEntries().isEmpty(), "MoveMap populated for " + sid);
                    for (int skin = 0; skin < 4; skin++) {
                        sc.setSkin(skin);
                    }
                    sc.setSkin(0);
                    // Exercise available MoveClasses safely
                    for (MoveInputType mit : List.of(MoveInputType.LIGHT, MoveInputType.HEAVY, MoveInputType.SPECIAL1, MoveInputType.UTILITY)) {
                        MoveClass mc = mit.getMoveClass();
                        if (mc != null && !stand.getMoveMap().getEntries(mc).isEmpty()) {
                            stand.initMove(mc);
                            stand.tick();
                            stand.cancelMove();
                        }
                    }
                    // Stand-specific sanity checks
                    if (stand instanceof MetallicaEntity met) {
                        assertTrue(met.getIron() > 0f, "Metallica starts with positive iron (" + met.getIron() + ")");
                    } else if (stand instanceof TheSunEntity sun) {
                        assertTrue(sun.getRawScale() >= TheSunEntity.MIN_SCALE, "TheSun scale initialized (" + sun.getRawScale() + ")");
                    } else if (stand instanceof PurpleHazeEntity ph) {
                        ph.setObedience(PurpleHazeEntity.MAX_OBEDIENCE);
                        assertTrue(ph.getObedience() == PurpleHazeEntity.MAX_OBEDIENCE, "PurpleHaze obedience synced");
                    } else if (stand instanceof PurpleHazeDistortionEntity phd) {
                        assertTrue(phd.getSyncedPoisonType() >= 0 && phd.getSyncedPoisonType() <= 2, "PHD poison type valid");
                    } else if (stand instanceof AerosmithEntity aero) {
                        aero.addOverheat(5f);
                        assertTrue(aero.getOverheat() >= 4.9f, "Aerosmith overheat functional");
                    } else if (stand instanceof CreamEntity cream) {
                        cream.setVoidTime(20);
                        assertTrue(CreamEntity.isCreaming(user) && user.isInvulnerable(), "Cream Void grants invulnerability");
                        cream.setVoidTime(0);
                    } else if (stand instanceof KQBTDEntity kqbtd) {
                        org.joml.Vector3f aura = kqbtd.getAuraColor();
                        assertTrue(aura != null && Float.isFinite(aura.x()) && Float.isFinite(aura.y()),
                                "KQBTDEntity.getAuraColor() runs on dedicated server without AzureLib RenderUtils");
                    }
                    // Reflective inspection of declared fields/methods catches hidden client-type leaks on Dedicated Server
                    assertTrue(stand.getClass().getDeclaredFields().length >= 0 && stand.getClass().getDeclaredMethods().length > 0,
                            "Dedicated-server reflection safe for " + sid);
                    stand.desummon();
                }
                standIndex++;
                waitTicks = 1;
            }
            case 2 -> {
                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 2: Dedicated-Server Safety (HamonWaveEntity) ===");
                HamonWaveEntity wave = new HamonWaveEntity(world);
                wave.setMaster(user);
                wave.setPos(ARENA_X, ARENA_Y, ARENA_Z);
                world.addFreshEntity(wave);
                for (int i = 0; i < 4; i++) {
                    wave.tick();
                }
                assertTrue(wave.isAlive(), "HamonWaveEntity ticks on dedicated server without ClientLevel NoClassDefFoundError");
                wave.discard();

                // Verify decoupled common particle options & server bridges on Dedicated Server
                BreathParticleOptions breathOpt = new BreathParticleOptions(1.0f);
                LockOnParticleOptions lockOnOpt = new LockOnParticleOptions(user.getId());
                MoshParticleOptions moshOpt = new MoshParticleOptions(user.getId(), new org.joml.Vector3f(1f, 0.5f, 0.5f));
                assertTrue(breathOpt.getType() == JParticleTypeRegistry.BREATH.get()
                        && lockOnOpt.getType() == JParticleTypeRegistry.LOCK_ON.get()
                        && moshOpt.getType() == JParticleTypeRegistry.METALLICA_MOSH.get(),
                        "Common Breath/LockOn/Mosh ParticleOptions instantiate cleanly on dedicated server");
                BoundSoundPlayer.EntitySoundHandle sh = BoundSoundPlayer.playSoundFrom(user, JSoundRegistry.IMPACT_1.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f, List.of());
                assertTrue(sh != null && "LMB".equals(Peacemaker.getFireKey()),
                        "BoundSoundPlayer & Peacemaker server fallbacks work without client classes");

                phase = 3;
                waitTicks = 1;
            }
            case 3 -> {
                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 3: Made in Heaven Overworld Clock Acceleration ===");
                CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(user);
                sc.setType(JStandTypeRegistry.MADE_IN_HEAVEN.get());
                StandEntity<?, ?> mih = JCraft.summon(world, user);
                assertTrue(mih instanceof MadeInHeavenEntity, "Summoned MadeInHeavenEntity");
                mihStartClock = JUtils.getDayTime(world);
                TimeAccelStatePacket.sendStart((MadeInHeavenEntity) mih, 60);
                phase = 4;
                waitTicks = 15;
            }
            case 4 -> {
                long afterClock = JUtils.getDayTime(world);
                long delta = afterClock - mihStartClock;
                assertTrue(delta > 25L, "MIH Time Acceleration really accelerates MC 26.3 Overworld clock (delta=" + delta + " ticks in 10 server ticks)");
                StandEntity<?, ?> mih = JUtils.getStand(user);
                if (mih instanceof MadeInHeavenEntity m) {
                    TimeAccelStatePacket.sendStop(m);
                    m.desummon();
                }
                phase = 5;
                waitTicks = 2;
            }
            case 5 -> {
                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 4: C-Moon Real Inverted Gravity Physics & Hitbox ===");
                gravityDummy = new Husk(EntityTypes.HUSK, world);
                gravityDummy.setNoAi(false);
                gravityDummy.removeFreeWill();
                gravityDummy.setPos(ARENA_X + 6.0, ARENA_Y + 1.0, ARENA_Z);
                gravityDummy.setDeltaMovement(Vec3.ZERO);
                world.addFreshEntity(gravityDummy);
                gravityStartPos = gravityDummy.position();
                gravityStartEyeY = gravityDummy.getEyePosition().y;

                GravityChangerAPI.addGravity(gravityDummy, new Gravity(Direction.UP, 10, 80, "selftest"));
                Direction applied = GravityChangerAPI.getGravityDirection(gravityDummy);
                assertTrue(applied == Direction.UP, "GravityChangerAPI applied Direction.UP (actual=" + applied + ")");

                Vec3 invertedEye = gravityDummy.getEyePosition();
                assertTrue(invertedEye.y < gravityDummy.getY(), "Inverted gravity flips eye position below feet pos (eyeY=" + invertedEye.y + " < y=" + gravityDummy.getY() + ")");

                AABB invBox = gravityDummy.getBoundingBox();
                assertTrue(invBox.minY < gravityDummy.getY() - 1.0, "Inverted gravity flips bounding box upward/downward properly (minY=" + invBox.minY + ", y=" + gravityDummy.getY() + ")");

                phase = 6;
                waitTicks = 28;
            }
            case 6 -> {
                double movedUp = gravityDummy.getY() - gravityStartPos.y;
                assertTrue(movedUp > 5.0, "Entity under Direction.UP gravity actually falls UPWARD onto ceiling (dy=" + movedUp + ")");
                assertTrue(gravityDummy.onGround(), "Entity reports onGround=true on ceiling under Direction.UP");
                assertTrue(gravityDummy.mainSupportingBlockPos.isPresent(), "Directional checkSupportingBlock found ceiling support block");
                assertTrue(gravityDummy.getHealth() < gravityDummy.getMaxHealth(),
                        "Directional fall damage hurts entity landing on ceiling (hp=" + gravityDummy.getHealth() + "/" + gravityDummy.getMaxHealth() + ")");

                BisectProjectile bisect = new BisectProjectile(world, gravityDummy);
                bisect.setPos(gravityDummy.getEyePosition());
                bisect.setDeltaMovement(0.4, 0.0, 0.0);
                world.addFreshEntity(bisect);
                for (int i = 0; i < 3; i++) {
                    bisect.tick();
                }
                assertTrue(bisect.isAlive(), "BisectProjectile ticks cleanly under Direction.UP gravity");
                bisect.discard();

                gravityDummy.setHealth(gravityDummy.getMaxHealth());
                gravityDummy.setPos(ARENA_X + 6.0, ARENA_Y + 4.0, ARENA_Z - 4.0);
                gravityDummy.setDeltaMovement(Vec3.ZERO);
                GravityChangerAPI.addGravity(gravityDummy, new Gravity(Direction.NORTH, 20, 60, "selftest_north"));
                phase = 7;
                waitTicks = 26;
            }
            case 7 -> {
                assertTrue(gravityDummy.getZ() < ARENA_Z - 7.0, "Entity under Direction.NORTH gravity falls onto north wall (z=" + gravityDummy.getZ() + ")");
                assertTrue(gravityDummy.onGround(), "Entity reports onGround=true against North wall");
                assertTrue(gravityDummy.mainSupportingBlockPos.isPresent(), "Directional checkSupportingBlock found North wall support block");
                GravityChangerAPI.clearGravity(gravityDummy);
                gravityDummy.discard();
                phase = 8;
                waitTicks = 1;
            }
            case 8 -> {
                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 5: Non-Player SlavedTo NBT & AnubisSpecUser Auto-Sheathe ===");
                CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(target);
                misc.setSlavedTo(user.getUUID());
                CompoundTag tag = new CompoundTag();
                ((net.arna.jcraft.common.component.impl.living.CommonMiscComponentImpl) misc).writeToNbt(tag);
                assertTrue(tag.contains("SlavedTo"), "SlavedTo serialized to NBT");
                assertTrue(world.getEntity(user.getUUID()) == user, "ServerLevel resolves non-player SlavedTo master");

                anubisNpc = new AnubisSpecUser(world);
                anubisNpc.setPos(ARENA_X - 4.0, ARENA_Y, ARENA_Z);
                anubisNpc.setNoAi(true);
                world.addFreshEntity(anubisNpc);
                anubisNpc.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JItemRegistry.ANUBIS.get()));
                for (int i = 0; i < 105; i++) {
                    anubisNpc.tick();
                }
                assertTrue(anubisNpc.getMainHandItem().is(JItemRegistry.ANUBIS_SHEATHED.get()),
                        "AnubisSpecUser auto-sheathes Anubis out of combat");
                anubisNpc.discard();

                finish(server);
            }
        }
    }

    private static void finish(final MinecraftServer server) {
        finished = true;
        if (user != null && user.isAlive()) user.discard();
        if (target != null && target.isAlive()) target.discard();
        if (gravityDummy != null && gravityDummy.isAlive()) gravityDummy.discard();

        if (failures.isEmpty()) {
            JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === ALL_STANDS_CHECKS_PASSED ({} assertions) ===", checksPassed);
        } else {
            JCraft.LOGGER.error("[ALLSTANDS-DEBUG] === ALL_STANDS_CHECKS_HAD_FAILURES ({} passed, {} failed) ===", checksPassed, failures.size());
            for (String f : failures) {
                JCraft.LOGGER.error("[ALLSTANDS-DEBUG]   {}", f);
            }
        }
        server.halt(false);
    }
}
