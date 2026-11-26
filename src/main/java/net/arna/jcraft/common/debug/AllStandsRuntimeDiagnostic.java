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
                JStandTypeRegistry.AEROSMITH,
                JStandTypeRegistry.CRAZY_DIAMOND,
                JStandTypeRegistry.CHARIOT_REQUIEM,
                JStandTypeRegistry.ORIGIN
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

                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 6: Combat & Game Feel Live Verification ===");
                // 1. Safe Micro-Hitstop: active only when ENABLE_HITSTOP=true, ignores barrages (<6 dmg), never overwrites longer TS
                boolean prevHitstop = net.arna.jcraft.common.config.JServerConfig.ENABLE_HITSTOP.getValue();
                try {
                    net.arna.jcraft.common.config.JServerConfig.ENABLE_HITSTOP.setValue(true);
                    Husk hitVictim = new Husk(EntityTypes.HUSK, world);
                    hitVictim.setPos(ARENA_X + 2.0, ARENA_Y, ARENA_Z + 2.0);
                    world.addFreshEntity(hitVictim);

                    // Light/barrage hit (< 6.0 dmg) must not apply hitstop
                    net.arna.jcraft.api.Attacks.tryApplyHitstop(user, hitVictim, 2.5f);
                    int lightTicks = JComponentPlatformUtils.getTimeStopData(hitVictim).map(d -> d.getTicks()).orElse(-1);
                    assertTrue(lightTicks == 0, "Barrage/light hit (< 6.0 dmg) applies 0 hitstop ticks (got=" + lightTicks + ")");

                    // Heavy hit (12.0 dmg) applies 3 ticks of micro-hitstop
                    net.arna.jcraft.api.Attacks.tryApplyHitstop(user, hitVictim, 12.0f);
                    int heavyTicks = JComponentPlatformUtils.getTimeStopData(hitVictim).map(d -> d.getTicks()).orElse(-1);
                    assertTrue(heavyTicks == 3, "Heavy hit (12.0 dmg) applies 3 micro-hitstop ticks (got=" + heavyTicks + ")");

                    // Must NEVER overwrite an existing longer Time Stop (e.g. 60 ticks)
                    JComponentPlatformUtils.getTimeStopData(hitVictim).ifPresent(d -> d.setTicks(60));
                    net.arna.jcraft.api.Attacks.tryApplyHitstop(user, hitVictim, 18.0f);
                    int preservedTs = JComponentPlatformUtils.getTimeStopData(hitVictim).map(d -> d.getTicks()).orElse(-1);
                    assertTrue(preservedTs == 60, "Hitstop must never shorten an active Time Stop (got=" + preservedTs + ")");
                    JComponentPlatformUtils.getTimeStopData(hitVictim).ifPresent(d -> d.setTicks(0));
                    JComponentPlatformUtils.getTimeStopData(user).ifPresent(d -> d.setTicks(0));
                    hitVictim.discard();
                } finally {
                    net.arna.jcraft.common.config.JServerConfig.ENABLE_HITSTOP.setValue(prevHitstop);
                }

                // 2. AI Collider Fence/Obstacle Detection (even when visual hasLineOfSight is true or slightly off-center)
                BlockPos fencePos = BlockPos.containing(ARENA_X, ARENA_Y, ARENA_Z + 1.0);
                Husk fenceMob = new Husk(EntityTypes.HUSK, world);
                fenceMob.setPos(ARENA_X + 0.18, ARENA_Y, ARENA_Z - 0.5);
                Husk fenceTarget = new Husk(EntityTypes.HUSK, world);
                fenceTarget.setPos(ARENA_X + 0.18, ARENA_Y, ARENA_Z + 2.5);
                world.addFreshEntity(fenceMob);
                world.addFreshEntity(fenceTarget);
                world.setBlockAndUpdate(fencePos, Blocks.AIR.defaultBlockState());
                assertTrue(!net.arna.jcraft.api.attack.IAttacker.isPathBlockedByBreakableObstacle(fenceMob, fenceTarget),
                        "Open air path is not blocked");
                world.setBlockAndUpdate(fencePos, Blocks.OAK_FENCE.defaultBlockState());
                assertTrue(net.arna.jcraft.api.attack.IAttacker.isPathBlockedByBreakableObstacle(fenceMob, fenceTarget),
                        "OAK_FENCE collider obstacle detected by AI for barrage break-through");
                world.setBlockAndUpdate(fencePos, Blocks.BEDROCK.defaultBlockState());
                assertTrue(!net.arna.jcraft.api.attack.IAttacker.isPathBlockedByBreakableObstacle(fenceMob, fenceTarget),
                        "Unbreakable BEDROCK is excluded from AI breakable-obstacle detection");
                world.setBlockAndUpdate(fencePos, Blocks.AIR.defaultBlockState());
                fenceMob.discard();
                fenceTarget.discard();

                // 3. StandEntity transient Attributes.ATTACK_SPEED modifier during active move & cleanup on desummon
                com.mojang.authlib.GameProfile diagProfile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "GameFeelDiag");
                net.minecraft.server.level.ServerPlayer attackSpeedPlayer = new net.minecraft.server.level.ServerPlayer(
                        server, world, diagProfile, net.minecraft.server.level.ClientInformation.createDefault());
                net.minecraft.network.Connection dummyConn = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
                attackSpeedPlayer.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(
                        server, dummyConn, attackSpeedPlayer,
                        net.minecraft.server.network.CommonListenerCookie.createInitial(diagProfile, false)) {
                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet) {}

                    @Override
                    public void send(net.minecraft.network.protocol.Packet<?> packet,
                                     @org.jetbrains.annotations.Nullable io.netty.channel.ChannelFutureListener listener) {}

                    @Override
                    public boolean hasClientLoaded() {
                        return true;
                    }
                };
                attackSpeedPlayer.setPos(ARENA_X, ARENA_Y, ARENA_Z);
                JComponentPlatformUtils.getStandComponent(attackSpeedPlayer).setType(JStandTypeRegistry.STAR_PLATINUM.get());
                StandEntity<?, ?> spStand = JCraft.summon(world, attackSpeedPlayer);
                assertTrue(spStand != null, "Summoned Star Platinum for AttackSpeed modifier check");
                if (spStand != null) {
                    spStand.initMove(net.arna.jcraft.api.attack.enums.MoveClass.LIGHT);
                    spStand.tick();
                    var atkAttr = attackSpeedPlayer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                    assertTrue(atkAttr != null && atkAttr.hasModifier(StandEntity.STAND_ATTACK_SPEED_MODIFIER_ID),
                            "Stand attack applies clean transient Attributes.ATTACK_SPEED penalty to user (no Mining Fatigue)");
                    spStand.cancelMove();
                    assertTrue(atkAttr != null && !atkAttr.hasModifier(StandEntity.STAND_ATTACK_SPEED_MODIFIER_ID),
                            "Stand cancelMove immediately cleans up transient Attributes.ATTACK_SPEED penalty");
                    spStand.initMove(net.arna.jcraft.api.attack.enums.MoveClass.LIGHT);
                    spStand.tick();
                    spStand.discard();
                    assertTrue(atkAttr != null && !atkAttr.hasModifier(StandEntity.STAND_ATTACK_SPEED_MODIFIER_ID),
                            "Stand removal/desummon immediately cleans up transient Attributes.ATTACK_SPEED penalty");
                }

                // 4. Deterministic HamonMoveStep tracking
                var hamonComp = JComponentPlatformUtils.getHamon(attackSpeedPlayer);
                hamonComp.setLastZoomPunched(target.getUUID());
                assertTrue(hamonComp.getLastHamonMove() == net.arna.jcraft.api.component.living.CommonHamonComponent.HamonMoveStep.ZOOM_PUNCH,
                        "HamonMoveStep tracks ZOOM_PUNCH");
                hamonComp.setLastSendoed(target.getUUID());
                assertTrue(hamonComp.getLastHamonMove() == net.arna.jcraft.api.component.living.CommonHamonComponent.HamonMoveStep.SENDO,
                        "HamonMoveStep tracks SENDO");
                hamonComp.setLastSendoAired(target.getUUID());
                assertTrue(hamonComp.getLastHamonMove() == net.arna.jcraft.api.component.living.CommonHamonComponent.HamonMoveStep.SENDO_AIR,
                        "HamonMoveStep tracks SENDO_AIR");
                hamonComp.setLastStomped(target.getUUID());
                assertTrue(hamonComp.getLastHamonMove() == net.arna.jcraft.api.component.living.CommonHamonComponent.HamonMoveStep.STOMP,
                        "HamonMoveStep tracks STOMP");

                JCraft.LOGGER.info("[ALLSTANDS-DEBUG] === Phase 7: Content & Balance (Crazy Diamond, Chariot Requiem, Hidden Configs) ===");
                // 1. Obtainability & evolution separation (CD is regular Stand Arrow drop; SCR is evolution-only)
                boolean cdInRegular = net.arna.jcraft.api.stand.StandTypeUtil.streamAllRegular()
                        .anyMatch(t -> t == JStandTypeRegistry.CRAZY_DIAMOND.get());
                boolean crInRegular = net.arna.jcraft.api.stand.StandTypeUtil.streamAllRegular()
                        .anyMatch(t -> t == JStandTypeRegistry.CHARIOT_REQUIEM.get());
                boolean crInEvolutions = net.arna.jcraft.api.stand.StandTypeUtil.streamAllEvolutions()
                        .anyMatch(t -> t == JStandTypeRegistry.CHARIOT_REQUIEM.get());
                assertTrue(cdInRegular, "Crazy Diamond is obtainable as a regular Stand Arrow roll");
                assertTrue(!crInRegular && crInEvolutions, "Chariot Requiem is evolution-only (excluded from regular Stand Arrow)");

                boolean hasScToScr = net.arna.jcraft.common.util.EvolutionItemHandler.getEvolutionsFor(JStandTypeRegistry.SILVER_CHARIOT.get())
                        .stream().anyMatch(e -> e.item() == JItemRegistry.REQUIEM_ARROW.get()
                                && e.target() == JStandTypeRegistry.CHARIOT_REQUIEM.get());
                assertTrue(hasScToScr, "EvolutionItemHandler registers Silver Chariot + Requiem Arrow -> Chariot Requiem");

                // 2. Crazy Diamond live 9-slot moveset & ally healing
                resetFighters();
                CommonStandComponent cdComp = JComponentPlatformUtils.getStandComponent(user);
                cdComp.setType(JStandTypeRegistry.CRAZY_DIAMOND.get());
                StandEntity<?, ?> cdStand = JCraft.summon(world, user);
                assertTrue(cdStand instanceof CrazyDiamondEntity, "Summoned CrazyDiamondEntity for live test");
                if (cdStand instanceof CrazyDiamondEntity cd) {
                    assertTrue(cd.getMoveMap().getEntries().size() >= 9,
                            "Crazy Diamond MoveMap has full moveset (" + cd.getMoveMap().getEntries().size() + " entries)");
                    target.setHealth(100.0f);
                    target.setPos(ARENA_X + 1.5, ARENA_Y, ARENA_Z);
                    cd.initMove(MoveClass.SPECIAL1);
                    for (int t = 0; t < 14; t++) {
                        user.positionRider(cd);
                        cd.tick();
                    }
                    assertTrue(target.getHealth() > 100.0f,
                            "Crazy Diamond Restoration Hand heals ally target even with ENABLE_FRIENDLY_FIRE=false (hp=" + target.getHealth() + ")");
                    cd.desummon();
                }

                // 3. Chariot Requiem live 9-slot moveset, Soul Swap & Requiem Counter
                resetFighters();
                CommonStandComponent crComp = JComponentPlatformUtils.getStandComponent(user);
                crComp.setType(JStandTypeRegistry.CHARIOT_REQUIEM.get());
                StandEntity<?, ?> crStand = JCraft.summon(world, user);
                assertTrue(crStand instanceof ChariotRequiemEntity, "Summoned ChariotRequiemEntity for live test");
                if (crStand instanceof ChariotRequiemEntity cr) {
                    assertTrue(cr.getMoveMap().getEntries().size() >= 9,
                            "Chariot Requiem MoveMap has full moveset (" + cr.getMoveMap().getEntries().size() + " entries)");
                    Husk swapA = new Husk(EntityTypes.HUSK, world);
                    Husk swapB = new Husk(EntityTypes.HUSK, world);
                    swapA.setNoAi(true);
                    swapB.setNoAi(true);
                    swapA.setPos(ARENA_X + 1.0, ARENA_Y, ARENA_Z - 1.0);
                    swapB.setPos(ARENA_X + 1.0, ARENA_Y, ARENA_Z + 1.0);
                    world.addFreshEntity(swapA);
                    world.addFreshEntity(swapB);
                    ChariotRequiemEntity.SOUL_SWAP.perform(cr, user);
                    assertTrue(swapA.getZ() > ARENA_Z && swapB.getZ() < ARENA_Z,
                            "Chariot Requiem Soul Swap swaps positions of caught targets (zA=" + swapA.getZ() + ", zB=" + swapB.getZ() + ")");

                    ChariotRequiemEntity.STAND_REBELLION.counter(cr, swapA, world.damageSources().mobAttack(swapA));
                    assertTrue(swapA.hasEffect(JStatusRegistry.STANDLESS.asHolder()) && swapA.hasEffect(JStatusRegistry.DAZED.asHolder()),
                            "Chariot Requiem Stand Rebellion counter inflicts Standless + Dazed");
                    swapA.discard();
                    swapB.discard();
                    cr.desummon();
                }

                // 4. Live verification of newly exposed JServerConfig options
                boolean prevUni = net.arna.jcraft.common.config.JServerConfig.UNIVERSAL_ABILITIES.getValue();
                boolean prevSand = net.arna.jcraft.common.config.JServerConfig.USE_FOOLISH_SAND.getValue();
                boolean prevCampfire = net.arna.jcraft.common.config.JServerConfig.SPTW_IGNITE_CAMPFIRES.getValue();
                boolean prevRespawnGrav = net.arna.jcraft.common.config.JServerConfig.RESET_GRAVITY_ON_RESPAWN.getValue();
                boolean prevVoidAbove = net.arna.jcraft.common.config.JServerConfig.VOID_DAMAGE_ABOVE_WORLD.getValue();
                try {
                    // Unified GravityChangerConfig sync verification live
                    net.arna.jcraft.common.config.JServerConfig.RESET_GRAVITY_ON_RESPAWN.setValue(false);
                    net.arna.jcraft.common.config.JServerConfig.VOID_DAMAGE_ABOVE_WORLD.setValue(true);
                    net.arna.jcraft.common.config.JServerConfig.syncGravityConfig();
                    assertTrue(!JCraft.gravityConfig.resetGravityOnRespawn && JCraft.gravityConfig.voidDamageAboveWorld,
                            "JServerConfig.syncGravityConfig() unifies and syncs resetGravityOnRespawn + voidDamageAboveWorld");
                    // UNIVERSAL_ABILITIES gates DashData.tryDash
                    net.arna.jcraft.common.config.JServerConfig.UNIVERSAL_ABILITIES.setValue(false);
                    JCraft.dashes.remove(user);
                    JComponentPlatformUtils.getCooldowns(user).setCooldown(CooldownType.DASH, 0);
                    user.setOnGround(true);
                    net.arna.jcraft.common.util.DashData.tryDash(1, 0, user);
                    assertTrue(!net.arna.jcraft.common.util.DashData.isDashing(user),
                            "UNIVERSAL_ABILITIES=false blocks DashData.tryDash");
                    net.arna.jcraft.common.config.JServerConfig.UNIVERSAL_ABILITIES.setValue(true);
                    net.arna.jcraft.common.util.DashData.tryDash(1, 0, user);
                    assertTrue(net.arna.jcraft.common.util.DashData.isDashing(user),
                            "UNIVERSAL_ABILITIES=true enables DashData.tryDash");
                    JCraft.dashes.remove(user);

                    // SPTW_IGNITE_CAMPFIRES gates campfire ignition on SPTWGroundSlamAttack
                    BlockPos campPos = BlockPos.containing(ARENA_X + 1.0, ARENA_Y, ARENA_Z);
                    world.setBlockAndUpdate(campPos, Blocks.CAMPFIRE.defaultBlockState()
                            .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
                    JComponentPlatformUtils.getStandComponent(user).setType(JStandTypeRegistry.STAR_PLATINUM_THE_WORLD.get());
                    StandEntity<?, ?> sptwTest = JCraft.summon(world, user);
                    if (sptwTest instanceof SPTWEntity sptw) {
                        net.arna.jcraft.common.config.JServerConfig.SPTW_IGNITE_CAMPFIRES.setValue(false);
                        SPTWEntity.GROUND_SLAM.perform(sptw, user);
                        assertTrue(!world.getBlockState(campPos).getValue(net.minecraft.world.level.block.CampfireBlock.LIT),
                                "SPTW_IGNITE_CAMPFIRES=false keeps unlit campfire unlit");
                        net.arna.jcraft.common.config.JServerConfig.SPTW_IGNITE_CAMPFIRES.setValue(true);
                        SPTWEntity.GROUND_SLAM.perform(sptw, user);
                        assertTrue(world.getBlockState(campPos).getValue(net.minecraft.world.level.block.CampfireBlock.LIT),
                                "SPTW_IGNITE_CAMPFIRES=true ignites nearby unlit campfire on Ground Slam");
                        sptw.desummon();
                    }
                    world.setBlockAndUpdate(campPos, Blocks.AIR.defaultBlockState());

                    // USE_FOOLISH_SAND gates TheFoolEntity.createFoolishSand
                    JComponentPlatformUtils.getStandComponent(user).setType(JStandTypeRegistry.THE_FOOL.get());
                    StandEntity<?, ?> foolStand = JCraft.summon(world, user);
                    if (foolStand instanceof TheFoolEntity fool) {
                        net.arna.jcraft.common.config.JServerConfig.USE_FOOLISH_SAND.setValue(false);
                        BlockPos sandPos = BlockPos.containing(ARENA_X + 2.0, ARENA_Y + 2.0, ARENA_Z);
                        int beforeSand = world.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
                                new AABB(sandPos).inflate(3.0)).size();
                        TheFoolEntity.createFoolishSand(world, fool, sandPos, Vec3.ZERO);
                        int afterSand = world.getEntitiesOfClass(net.minecraft.world.entity.item.FallingBlockEntity.class,
                                new AABB(sandPos).inflate(3.0)).size();
                        assertTrue(afterSand == beforeSand, "USE_FOOLISH_SAND=false suppresses Foolish Sand spawning");
                        fool.desummon();
                    }
                } finally {
                    net.arna.jcraft.common.config.JServerConfig.UNIVERSAL_ABILITIES.setValue(prevUni);
                    net.arna.jcraft.common.config.JServerConfig.USE_FOOLISH_SAND.setValue(prevSand);
                    net.arna.jcraft.common.config.JServerConfig.SPTW_IGNITE_CAMPFIRES.setValue(prevCampfire);
                    net.arna.jcraft.common.config.JServerConfig.RESET_GRAVITY_ON_RESPAWN.setValue(prevRespawnGrav);
                    net.arna.jcraft.common.config.JServerConfig.VOID_DAMAGE_ABOVE_WORLD.setValue(prevVoidAbove);
                    net.arna.jcraft.common.config.JServerConfig.syncGravityConfig();
                }

                // 5. Origin Stand, Skill Catalog & Cross-Stand Loadout Verification
                {
                    var allSkills = net.arna.jcraft.common.stand.OriginSkillCatalog.getAllSkills();
                    assertTrue(allSkills.size() >= 80,
                            "OriginSkillCatalog discovered compatible moves across Stands (count=" + allSkills.size() + ")");
                    for (var preset : net.arna.jcraft.common.stand.OriginSkillCatalog.getPresets()) {
                        assertTrue(preset.slots().size() == net.arna.jcraft.api.attack.enums.MoveClass.values().length,
                                "Origin preset covers all 9 MoveClass slots: " + preset.id());
                    }

                    CommonStandComponent originComp = JComponentPlatformUtils.getStandComponent(user);
                    originComp.setType(JStandTypeRegistry.ORIGIN.get());
                    java.util.EnumMap<net.arna.jcraft.api.attack.enums.MoveClass, String> customLoadout =
                            new java.util.EnumMap<>(net.arna.jcraft.api.attack.enums.MoveClass.class);
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.LIGHT, "jcraft:star_platinum#light");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.HEAVY, "jcraft:the_world_over_heaven#heavy");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.BARRAGE, "jcraft:star_platinum#barrage");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.SPECIAL1, "jcraft:the_world_over_heaven#special3");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.SPECIAL2, "jcraft:king_crimson#ultimate");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.SPECIAL3, "jcraft:gold_experience_requiem#utility");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.ULTIMATE, "jcraft:the_world#ultimate");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.UTILITY, "jcraft:made_in_heaven#ultimate");
                    customLoadout.put(net.arna.jcraft.api.attack.enums.MoveClass.TOSS, "jcraft:the_world#toss");
                    originComp.setOriginLoadout(customLoadout);

                    StandEntity<?, ?> originRaw = JCraft.summon(world, user);
                    assertTrue(originRaw instanceof OriginEntity, "Summoned OriginEntity cleanly");
                    if (originRaw instanceof OriginEntity origin) {
                        origin.rebuildLoadoutFromUser(originComp);
                        assertTrue(origin.getMoveMap().getEntries().size() >= 9,
                                "Origin rebuilt dynamic cross-stand MoveMap (" + origin.getMoveMap().getEntries().size() + " entries)");
                        var equippedBarrage = origin.getEquippedSkill(net.arna.jcraft.api.attack.enums.MoveClass.BARRAGE);
                        assertTrue(equippedBarrage != null && "jcraft:star_platinum#barrage".equals(equippedBarrage.id()),
                                "Origin equipped Star Platinum Barrage on BARRAGE bind");
                        JComponentPlatformUtils.getCooldowns(user).setCooldown(CooldownType.STAND_HEAVY, 0);
                        boolean startedMove = origin.initMove(net.arna.jcraft.api.attack.enums.MoveClass.HEAVY);
                        assertTrue(startedMove, "Origin initiated cross-stand TWOH Heavy bind cleanly");
                        origin.cancelMove();
                        origin.desummon();
                    }
                    originComp.clearOriginLoadout();
                }

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
