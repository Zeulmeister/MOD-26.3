package net.arna.jcraft.client.debug;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.api.registry.JSplatterTypeRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.splatter.JSplatterManager;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.renderer.effects.AttackHitboxEffectRenderer;
import net.arna.jcraft.client.rendering.MihAfterimageTrail;
import net.arna.jcraft.client.rendering.handler.ColorShiftShaderHandler;
import net.arna.jcraft.client.rendering.handler.CrimsonShaderHandler;
import net.arna.jcraft.client.rendering.handler.JPostEffectManager;
import net.arna.jcraft.client.rendering.handler.MandomRewindShaderHandler;
import net.arna.jcraft.client.rendering.handler.ZaWarudoShaderHandler;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.Gravity;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Automated graphical MC 26.3 client visual verification suite.
 * Captures real OpenGL/RenderPearl screenshots and asserts live visual + physics behavior:
 * combat feature layers, splatter/hitbox renderers, Cinderella mask texture, C-Moon directional
 * gravity (UP/NORTH) + camera flip + terrain invalidation, all 4 RenderPearl shaders,
 * MIH afterimage trails, and Vampire blood HUD icons.
 * Active only when {@code -Djcraft.selftest.client=true} is passed.
 */
@Environment(EnvType.CLIENT)
public final class ClientVisualDiagnostic {
    public static final boolean ENABLED = Boolean.getBoolean("jcraft.selftest.client");
    private static final String OUT_DIR_PROP = System.getProperty("jcraft.selftest.client.outdir", "jcraft_visual_shots");

    private static int phase = 0;
    private static int waitTicks = 0;
    private static int totalTicks = 0;
    private static boolean finished = false;
    private static UUID dummyId = null;

    private static final List<String> failures = new ArrayList<>();
    private static int checksPassed = 0;
    private static final List<String> expectedScreenshots = new ArrayList<>();

    private ClientVisualDiagnostic() {}

    public static void tick(final Minecraft client) {
        if (!ENABLED || finished) {
            return;
        }
        totalTicks++;
        if (totalTicks > 900) {
            failures.add("FAIL: Watchdog timeout reached at phase " + phase);
            finish(client);
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        try {
            runPhase(client);
        } catch (Throwable t) {
            failures.add("Phase " + phase + " threw " + t.getClass().getSimpleName() + ": " + t.getMessage());
            JCraft.LOGGER.error("[CLIENT-VISUAL] Exception in phase {}", phase, t);
            finish(client);
        }
    }

    private static void assertTrue(final boolean cond, final String msg) {
        if (!cond) {
            failures.add("FAIL: " + msg);
            JCraft.LOGGER.error("[CLIENT-VISUAL] FAIL: {}", msg);
        } else {
            checksPassed++;
            JCraft.LOGGER.info("[CLIENT-VISUAL] PASS: {}", msg);
        }
    }

    private static File getOutDir() {
        File dir = new File(OUT_DIR_PROP);
        new File(dir, "screenshots").mkdirs();
        return dir;
    }

    private static void capture(final Minecraft client, final String fileName) {
        expectedScreenshots.add(fileName);
        Screenshot.grab(getOutDir(), fileName, client.gameRenderer.mainRenderTarget(), 1,
                msg -> JCraft.LOGGER.info("[CLIENT-VISUAL] Captured {}: {}", fileName, msg.getString()));
    }

    private static ServerPlayer getServerPlayer(final IntegratedServer srv) {
        List<ServerPlayer> players = srv.getPlayerList().getPlayers();
        return players.isEmpty() ? null : players.get(0);
    }

    private static void deleteDirIfExists(final Path path) {
        if (!Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException e) {
            JCraft.LOGGER.warn("[CLIENT-VISUAL] Could not clean old world dir {}", path, e);
        }
    }

    private static void runPhase(final Minecraft client) {
        switch (phase) {
            case 0 -> {
                if (!client.isGameLoadFinished()) {
                    return;
                }
                if (client.level == null) {
                    JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 0: Creating singleplayer visual test world ===");
                    deleteDirIfExists(client.gameDirectory.toPath().resolve("saves/jcraft_visual_test"));
                    client.createWorldOpenFlows().createFreshLevel(
                            "jcraft_visual_test",
                            new LevelSettings("jcraft_visual_test", GameType.CREATIVE,
                                    LevelSettings.DifficultySettings.DEFAULT, true, WorldDataConfiguration.DEFAULT),
                            new WorldOptions(12345L, false, false),
                            WorldPresets::createTestWorldDimensions,
                            client.gui.screen()
                    );
                    phase = 1;
                    waitTicks = 10;
                }
            }
            case 1 -> {
                if (client.level == null || client.player == null || client.getSingleplayerServer() == null) {
                    return;
                }
                if (client.gui.screen() != null) {
                    client.gui.setScreen(null);
                }
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 1: Building Lit Test Chamber, Combat Layers & Effects ===");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) {
                        return;
                    }
                    JUtils.setDayTime(overworld, 6000L);
                    for (int x = -6; x <= 6; x++) {
                        for (int z = -5; z <= 6; z++) {
                            overworld.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                            for (int y = 100; y <= 105; y++) {
                                overworld.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                            }
                            overworld.setBlockAndUpdate(new BlockPos(x, 106, z), Blocks.QUARTZ_BLOCK.defaultBlockState());
                        }
                    }
                    // North wall (brick) for lateral C-Moon wall-gravity landing
                    for (int x = -6; x <= 6; x++) {
                        for (int y = 99; y <= 106; y++) {
                            overworld.setBlockAndUpdate(new BlockPos(x, y, -4), Blocks.BRICKS.defaultBlockState());
                        }
                    }
                    // Glowstone illumination + step-up blocks on floor & ceiling
                    overworld.setBlockAndUpdate(new BlockPos(-4, 99, 4), Blocks.GLOWSTONE.defaultBlockState());
                    overworld.setBlockAndUpdate(new BlockPos(4, 99, 4), Blocks.GLOWSTONE.defaultBlockState());
                    overworld.setBlockAndUpdate(new BlockPos(-4, 106, 0), Blocks.GLOWSTONE.defaultBlockState());
                    overworld.setBlockAndUpdate(new BlockPos(4, 106, 0), Blocks.GLOWSTONE.defaultBlockState());
                    overworld.setBlockAndUpdate(new BlockPos(2, 105, 1), Blocks.STONE_SLAB.defaultBlockState());

                    sp.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JItemRegistry.CINDERELLA_MASK.get()));
                    sp.teleportTo(overworld, 0.5, 100.0, 0.0, java.util.Set.of(), 0.0f, 8.0f, true);
                    sp.setDeltaMovement(Vec3.ZERO);

                    Husk dummy = new Husk(EntityTypes.HUSK, overworld);
                    dummy.setNoAi(true);
                    dummy.setPos(0.5, 100.0, 3.0);
                    dummy.setYRot(180.0f);
                    dummy.setYHeadRot(180.0f);
                    dummy.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JItemRegistry.CINDERELLA_MASK.get()));
                    overworld.addFreshEntity(dummy);
                    dummyId = dummy.getUUID();

                    CommonMiscComponent misc = JComponentPlatformUtils.getMiscData(dummy);
                    for (int i = 0; i < 5; i++) {
                        misc.stab();
                    }
                    misc.displayArmoredHit();

                    CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(dummy);
                    hamon.setHamonCharge(90.0f);
                    hamon.setHamonizeReady(true);

                    JSplatterManager sm = JUtils.getSplatterManager(overworld);
                    if (sm != null) {
                        sm.addSplatter(new Vec3(0.5, 100.05, 2.0), JSplatterTypeRegistry.BLOOD_SPLATTER_TYPE, 1.5f, 400, dummy);
                    }
                });

                AttackHitboxEffectRenderer.addHitbox(new AABB(-0.3, 100.2, 2.2, 1.3, 102.0, 3.8), 6000L, false);
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                phase = 2;
                waitTicks = 12;
            }
            case 2 -> {
                assertTrue(client.player != null, "LocalPlayer active in visual chamber");
                capture(client, "01_layers_splatter_mask.png");
                phase = 3;
                waitTicks = 6;
            }
            case 3 -> {
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 2: C-Moon Gravity UP (Ceiling Fall, Camera Flip, Terrain Invalidation) ===");
                final IntegratedServer srv = client.getSingleplayerServer();
                JCraftClient.gravityTerrainInvalidations = 0;
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    sp.setGameMode(GameType.SURVIVAL);
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.C_MOON.get());
                    if (JUtils.getStand(sp) == null) {
                        JCraft.summon(overworld, sp);
                    }
                    sp.teleportTo(overworld, 0.5, 100.5, 0.5, java.util.Set.of(), 0.0f, 0.0f, true);
                    GravityChangerAPI.addGravity(sp, new Gravity(Direction.UP, 50, 200, "visual_up"));
                });
                phase = 4;
                waitTicks = 32;
            }
            case 4 -> {
                LocalPlayer p = client.player;
                assertTrue(p != null && p.getY() > 103.5, "C-Moon UP pulls Survival LocalPlayer to ceiling (y=" + (p != null ? p.getY() : 0) + ")");
                assertTrue(p != null && p.onGround(), "LocalPlayer reports onGround=true standing on ceiling");
                assertTrue(p != null && p.mainSupportingBlockPos.isPresent(), "Directional checkSupportingBlock finds ceiling block");
                assertTrue(p != null && p.getEyePosition().y < p.getY(), "Inverted eye position is below feet (eyeY=" + (p != null ? p.getEyePosition().y : 0) + ")");
                float camUpY = client.gameRenderer.mainCamera().upVector().y();
                var cam = client.gameRenderer.mainCamera();
                JCraft.LOGGER.info("[CLIENT-VISUAL] DIAG-UP: pose={} yRot={} xRot={} camPos={} fwd={} blockAtCam={} visibleSections={}",
                        p != null ? p.getPose() : null,
                        p != null ? p.getYRot() : 0f,
                        p != null ? p.getXRot() : 0f,
                        cam.position(), cam.forwardVector(),
                        client.level.getBlockState(cam.blockPosition()),
                        client.levelRenderer.visibleSections().size());
                assertTrue(camUpY < -0.5f, "Camera upVector is inverted upside-down (upY=" + camUpY + ")");
                assertTrue(JCraftClient.gravityTerrainInvalidations > 0, "RotationAnimation triggered SectionOcclusionGraph invalidation (" + JCraftClient.gravityTerrainInvalidations + "x)");

                capture(client, "02_cmoon_up_3rdperson.png");
                phase = 5;
                waitTicks = 5;
            }
            case 5 -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                phase = 6;
                waitTicks = 5;
            }
            case 6 -> {
                capture(client, "03_cmoon_up_1stperson.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld != null && sp != null) {
                        sp.setDeltaMovement(Vec3.ZERO);
                        sp.teleportTo(overworld, 0.5, 102.5, 1.0, java.util.Set.of(), 0.0f, 0.0f, true);
                        GravityChangerAPI.addGravity(sp, new Gravity(Direction.NORTH, 60, 200, "visual_north"));
                    }
                });
                phase = 65;
                waitTicks = 8;
            }
            case 65 -> {
                if (client.player != null) {
                    client.player.setYRot(90.0f);
                    client.player.setXRot(0.0f);
                    client.player.yRotO = 90.0f;
                    client.player.xRotO = 0.0f;
                }
                phase = 7;
                waitTicks = 24;
            }
            case 7 -> {
                LocalPlayer p = client.player;
                assertTrue(p != null && p.getZ() < -1.5, "C-Moon NORTH pulls LocalPlayer onto North brick wall (z=" + (p != null ? p.getZ() : 0) + ")");
                assertTrue(p != null && Math.abs(p.getY() - 102.5) < 0.15, "LocalPlayer holds steady on vertical North wall without sliding (y=" + (p != null ? p.getY() : 0) + ")");
                assertTrue(p != null && p.onGround(), "LocalPlayer reports onGround=true on vertical North wall");
                assertTrue(p != null && p.mainSupportingBlockPos.isPresent(), "Directional checkSupportingBlock finds vertical wall block");
                float camUpZ = client.gameRenderer.mainCamera().upVector().z();
                var camN = client.gameRenderer.mainCamera();
                JCraft.LOGGER.info("[CLIENT-VISUAL] DIAG-NORTH: pose={} pPos={} yRot={} xRot={} camPos={} fwd={} blockAtCam={} visibleSections={}",
                        p != null ? p.getPose() : null,
                        p != null ? p.position() : null,
                        p != null ? p.getYRot() : 0f,
                        p != null ? p.getXRot() : 0f,
                        camN.position(), camN.forwardVector(),
                        client.level.getBlockState(camN.blockPosition()),
                        client.levelRenderer.visibleSections().size());
                assertTrue(camUpZ > 0.5f, "Camera upVector rotated sideways for North wall (upZ=" + camUpZ + ")");
                assertTrue(client.level.getBlockState(camN.blockPosition()).isAir(), "Camera position for North wall is in open air");
                assertTrue(client.levelRenderer.visibleSections().size() > 10, "Visible terrain sections rendered for North wall (" + client.levelRenderer.visibleSections().size() + ")");
                capture(client, "04_cmoon_north_wall.png");

                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld != null && sp != null) {
                        GravityChangerAPI.clearGravity(sp);
                        sp.teleportTo(overworld, 0.5, 100.0, 0.0, java.util.Set.of(), 0.0f, 8.0f, true);
                    }
                });
                phase = 8;
                waitTicks = 14;
            }
            case 8 -> {
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 3: RenderPearl Post-Effect Shaders ===");
                ZaWarudoShaderHandler.INSTANCE.shouldRender = true;
                ZaWarudoShaderHandler.INSTANCE.effectLength = 40;
                ZaWarudoShaderHandler.INSTANCE.renderingEffect = true;
                phase = 9;
                waitTicks = 6;
            }
            case 9 -> {
                verifyShaderActive(client, ZaWarudoShaderHandler.POST_EFFECT_ID, "za_warudo");
                capture(client, "05_shader_za_warudo.png");
                ZaWarudoShaderHandler.INSTANCE.reset();
                phase = 10;
                waitTicks = 4;
            }
            case 10 -> {
                CrimsonShaderHandler.INSTANCE.shouldRender = true;
                CrimsonShaderHandler.INSTANCE.effectLength = 40;
                CrimsonShaderHandler.INSTANCE.renderingEffect = true;
                phase = 11;
                waitTicks = 6;
            }
            case 11 -> {
                verifyShaderActive(client, CrimsonShaderHandler.POST_EFFECT_ID, "time_erase");
                capture(client, "06_shader_time_erase.png");
                CrimsonShaderHandler.INSTANCE.shouldRender = false;
                CrimsonShaderHandler.INSTANCE.renderingEffect = false;
                phase = 12;
                waitTicks = 4;
            }
            case 12 -> {
                MandomRewindShaderHandler.INSTANCE.shouldRender = true;
                MandomRewindShaderHandler.INSTANCE.duration = 40;
                MandomRewindShaderHandler.INSTANCE.renderingEffect = true;
                phase = 13;
                waitTicks = 6;
            }
            case 13 -> {
                verifyShaderActive(client, MandomRewindShaderHandler.POST_EFFECT_ID, "bites_the_dust");
                MandomRewindShaderHandler.INSTANCE.shouldRender = false;
                MandomRewindShaderHandler.INSTANCE.renderingEffect = false;
                ColorShiftShaderHandler.INSTANCE.trigger(30, 0.55f);
                phase = 14;
                waitTicks = 6;
            }
            case 14 -> {
                verifyShaderActive(client, ColorShiftShaderHandler.POST_EFFECT_ID, "color_shift");
                ColorShiftShaderHandler.INSTANCE.shouldRender = false;
                ColorShiftShaderHandler.INSTANCE.renderingEffect = false;
                phase = 15;
                waitTicks = 4;
            }
            case 15 -> {
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 4: MIH Afterimage Trail + Vampire Blood HUD Icons ===");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.MADE_IN_HEAVEN.get());
                    StandEntity<?, ?> mih = JCraft.summon(overworld, sp);
                    if (mih instanceof MadeInHeavenEntity m) {
                        m.setAccelTime(100);
                        m.setSpeedometer(5);
                    }
                    JComponentPlatformUtils.getSpecData(sp).setType(JSpecTypeRegistry.VAMPIRE.get());
                    CommonVampireComponent vamp = JComponentPlatformUtils.getVampirism(sp);
                    vamp.setVampire(true);
                    vamp.setBlood(15.0f);
                });
                phase = 16;
                waitTicks = 4;
            }
            case 16 -> {
                // Step forward across several ticks so MihAfterimageTrail captures non-overlapping trail positions
                LocalPlayer p = client.player;
                if (p != null) {
                    p.setPos(p.getX(), p.getY(), p.getZ() + 0.35);
                }
                phase = 17;
                waitTicks = 1;
            }
            case 17 -> {
                LocalPlayer p = client.player;
                if (p != null) {
                    p.setPos(p.getX(), p.getY(), p.getZ() + 0.35);
                }
                phase = 18;
                waitTicks = 1;
            }
            case 18 -> {
                LocalPlayer p = client.player;
                if (p != null) {
                    p.setPos(p.getX(), p.getY(), p.getZ() + 0.35);
                }
                phase = 19;
                waitTicks = 2;
            }
            case 19 -> {
                LocalPlayer p = client.player;
                assertTrue(p != null && JComponentPlatformUtils.getVampirism(p).isVampire(), "Vampire spec synced to LocalPlayer for Blood HUD");
                assertTrue(!JClientConfig.getInstance().isDisableMihAfterimages(), "MIH afterimages enabled by default");
                assertTrue(p != null && MihAfterimageTrail.get(p, 10).at(2) != null, "MihAfterimageTrail sampled recent player trail");
                capture(client, "07_mih_afterimage_vampire_hud.png");
                phase = 20;
                waitTicks = 8;
            }
            case 20 -> {
                File shotDir = new File(getOutDir(), "screenshots");
                for (String name : expectedScreenshots) {
                    File f = new File(shotDir, name);
                    assertTrue(f.exists() && f.length() > 1024L, "Screenshot saved cleanly: " + name + " (" + f.length() + " bytes)");
                }
                finish(client);
            }
        }
    }

    private static void verifyShaderActive(final Minecraft client, final Identifier postId, final String label) {
        boolean valid = client.getShaderManager().isPostEffectValid(postId, LevelTargetBundle.MAIN_TARGETS);
        boolean applied = client.gameRenderer.getAppliedPostEffects().contains(postId);
        boolean noFail = !JPostEffectManager.getLastFailedPostEffects().contains(postId);
        assertTrue(valid && applied && noFail,
                "RenderPearl shader active & valid on GPU: " + label + " (valid=" + valid + ", applied=" + applied + ", noFail=" + noFail + ")");
    }

    private static void finish(final Minecraft client) {
        finished = true;
        if (failures.isEmpty()) {
            JCraft.LOGGER.info("[CLIENT-VISUAL] SUMMARY: PASSED={} FAILED=0", checksPassed);
            JCraft.LOGGER.info("[CLIENT-VISUAL] ALL_CLIENT_VISUAL_CHECKS_PASSED");
        } else {
            JCraft.LOGGER.error("[CLIENT-VISUAL] SUMMARY: PASSED={} FAILED={}", checksPassed, failures.size());
            for (String f : failures) {
                JCraft.LOGGER.error("[CLIENT-VISUAL]   {}", f);
            }
        }
        boolean ok = failures.isEmpty();
        client.stop();
        if (!ok) {
            System.exit(1);
        }
    }
}
