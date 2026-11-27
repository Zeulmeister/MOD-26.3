package net.arna.jcraft.client.debug;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonMiscComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.registry.JEntityTypeRegistry;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.api.registry.JSplatterTypeRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.splatter.JSplatterManager;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.gui.screen.OriginArsenalScreen;
import net.arna.jcraft.client.renderer.effects.StandRunicCircleRenderer;
import net.arna.jcraft.client.rendering.AuraAfterimageManager;
import net.arna.jcraft.client.rendering.CombatImpactEffects;
import net.arna.jcraft.client.rendering.MihAfterimageTrail;
import net.arna.jcraft.client.rendering.handler.ColorShiftShaderHandler;
import net.arna.jcraft.client.rendering.handler.CrimsonShaderHandler;
import net.arna.jcraft.client.rendering.handler.JPostEffectManager;
import net.arna.jcraft.client.rendering.handler.MandomRewindShaderHandler;
import net.arna.jcraft.client.rendering.handler.ZaWarudoShaderHandler;
import net.arna.jcraft.client.rendering.skybox.SkyBoxManager;
import net.arna.jcraft.common.entity.spec.BrawlerSpecUser;
import net.arna.jcraft.common.entity.stand.MadeInHeavenEntity;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.arna.jcraft.common.entity.stand.TheSunEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.Gravity;
import net.arna.jcraft.common.network.c2s.OriginLoadoutPacket;
import net.arna.jcraft.common.stand.OriginProfile;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private static final boolean ORIGIN_GALLERY = Boolean.getBoolean("jcraft.selftest.origin_gallery") || OUT_DIR_PROP.contains("origin_gallery");

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
        if (totalTicks > 1900) {
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
                if (ORIGIN_GALLERY) {
                    JCraft.LOGGER.info("[CLIENT-VISUAL] === Origin Showcase Gallery Mode ===");
                    final IntegratedServer srv = client.getSingleplayerServer();
                    srv.execute(() -> {
                        ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                        ServerPlayer sp = getServerPlayer(srv);
                        if (overworld == null || sp == null) return;
                        JUtils.setDayTime(overworld, 6000L);
                        for (int x = -6; x <= 6; x++) {
                            for (int z = -6; z <= 6; z++) {
                                overworld.setBlockAndUpdate(new BlockPos(x, 112, z),
                                        ((x + z) & 1) == 0 ? Blocks.POLISHED_BLACKSTONE.defaultBlockState() : Blocks.DEEPSLATE_TILES.defaultBlockState());
                                for (int y = 113; y <= 120; y++) {
                                    overworld.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                                }
                            }
                        }
                        if (JUtils.getStand(sp) != null) {
                            JUtils.getStand(sp).desummon();
                        }
                        sp.setInvisible(true);
                        sp.teleportTo(overworld, 0.5, 113.0, 0.0, java.util.Set.of(), 0.0f, 5.0f, false);
                        CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                        sc.setType(JStandTypeRegistry.ORIGIN.get());
                        sc.setSkin(0);
                        JCraft.summon(overworld, sp);
                    });
                    client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    phase = 100;
                    waitTicks = 30;
                    return;
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
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 5: Time Erase SkyBox, The Sun Emissive & SpecUserMob Armor ===");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    // Open-sky observation deck at y=112
                    for (int x = -3; x <= 4; x++) {
                        for (int z = -2; z <= 8; z++) {
                            overworld.setBlockAndUpdate(new BlockPos(x, 112, z),
                                    ((x + z) & 1) == 0 ? Blocks.QUARTZ_BLOCK.defaultBlockState() : Blocks.SMOOTH_STONE.defaultBlockState());
                            for (int y = 113; y <= 122; y++) {
                                overworld.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                            }
                        }
                    }
                    sp.setGameMode(GameType.CREATIVE);
                    sp.teleportTo(overworld, 0.5, 113.0, 0.0, java.util.Set.of(), 0.0f, -20.0f, false);

                    // Summon The Sun overhead for unshaded emissive + corona verification
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.THE_SUN.get());
                    StandEntity<?, ?> sunStand = JCraft.summon(overworld, sp);
                    if (sunStand instanceof TheSunEntity sun) {
                        sun.setPos(2.6, 116.8, 6.2);
                    }

                    // Spawn SpecUserMob wearing full diamond armor + Stone Mask to validate SpecUserArmorLayer
                    BrawlerSpecUser npc = JEntityTypeRegistry.BRAWLER_SPEC_USER.get().create(overworld, EntitySpawnReason.COMMAND);
                    if (npc != null) {
                        npc.setNoAi(true);
                        npc.setPos(-0.8, 113.0, 3.4);
                        npc.setYRot(170.0f);
                        npc.setYHeadRot(170.0f);
                        npc.setItemSlot(EquipmentSlot.HEAD, new ItemStack(JItemRegistry.STONE_MASK.get()));
                        npc.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
                        npc.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
                        npc.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                        overworld.addFreshEntity(npc);
                    }
                });
                client.options.setCameraType(CameraType.FIRST_PERSON);
                SkyBoxManager.timeEraseSkyDraws = 0;
                SkyBoxManager.getInstance().setForceTimeErase(true);
                phase = 21;
                waitTicks = 10;
            }
            case 21 -> {
                LocalPlayer p = client.player;
                if (p != null) {
                    p.setYRot(0.0f);
                    p.setXRot(-22.0f);
                }
                assertTrue(SkyBoxManager.timeEraseSkyDraws > 0,
                        "Time Erase SkyBox rendered natively on GPU via SkyRendererMixin (draws=" + SkyBoxManager.timeEraseSkyDraws + ")");
                capture(client, "08_skybox_time_erase_and_the_sun.png");

                // Switch to Made in Heaven celestial star-trails skybox
                SkyBoxManager.getInstance().setForceTimeErase(false);
                SkyBoxManager.timeAccelStarTrailDraws = 0;
                SkyBoxManager.getInstance().setForceTimeAccel(true);
                phase = 22;
                waitTicks = 8;
            }
            case 22 -> {
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 6: MIH Celestial Star-Trails SkyBox ===");
                assertTrue(SkyBoxManager.timeAccelStarTrailDraws > 0,
                        "MIH celestial star-trails rendered natively on GPU via SkyRendererMixin (draws=" + SkyBoxManager.timeAccelStarTrailDraws + ")");
                capture(client, "09_skybox_mih_and_spec_armor.png");
                SkyBoxManager.getInstance().setForceTimeAccel(false);
                phase = 23;
                waitTicks = 6;
            }
            case 23 -> {
                JCraft.LOGGER.info("[CLIENT-VISUAL] === Phase 7: Origin Stand Renderer, Attack Pose & Origin Arsenal UI ===");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    sp.teleportTo(overworld, 0.5, 113.0, 0.0, java.util.Set.of(), 0.0f, 5.0f, false);
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.ORIGIN.get());
                    sc.setSkin(0);
                    JCraft.summon(overworld, sp);
                });
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                phase = 24;
                waitTicks = 28;
            }
            case 24 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity,
                        "OriginEntity summoned and rendered in third-person client view");
                capture(client, "10_origin_stand.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        origin.initMove(MoveClass.HEAVY);
                    }
                });
                phase = 25;
                waitTicks = 14;
            }
            case 25 -> {
                boolean attackingOnClient = JUtils.getStand(client.player) instanceof OriginEntity origin
                        && origin.getMoveStun() > 0 && origin.getState() == OriginEntity.State.HEAVY;
                assertTrue(attackingOnClient, "OriginEntity active Heavy attack pose & particles visible on client");
                capture(client, "12_origin_attack_pose.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        origin.setMoveStun(0);
                        origin.initMove(MoveClass.BARRAGE);
                    }
                });
                phase = 255;
                waitTicks = 6;
            }
            case 255 -> {
                boolean nativeBarrageActive = JUtils.getStand(client.player) instanceof OriginEntity origin
                        && origin.getMoveStun() > 0 && origin.getState() == OriginEntity.State.BARRAGE;
                assertTrue(nativeBarrageActive, "OriginEntity native MainBarrageAttack active with smear-frame barrage pose & VFX");
                capture(client, "12b_origin_native_barrage.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getCooldowns(sp).clear();
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) {
                            origin.setMoveStun(0);
                        }
                    }
                });
                phase = 26;
                waitTicks = 4;
            }
            case 26 -> {
                client.gui.setScreen(new OriginArsenalScreen(null));
                phase = 27;
                waitTicks = 4;
            }
            case 27 -> {
                assertTrue(client.gui.screen() instanceof OriginArsenalScreen,
                        "OriginArsenalScreen opened cleanly on client");
                if (client.gui.screen() instanceof OriginArsenalScreen arsenal) {
                    assertTrue(!arsenal.isPauseScreen(),
                            "OriginArsenalScreen must not pause integrated server so C2S skill packets apply live");
                    boolean slotClicked = arsenal.mouseClicked(new MouseButtonEvent(
                            arsenal.getLeftSlotCenterX(),
                            arsenal.getLeftSlotCenterY(MoveClass.BARRAGE),
                            new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                    assertTrue(slotClicked && arsenal.getSelectedSlot() == MoveClass.BARRAGE,
                            "OriginArsenalScreen selects BARRAGE bind slot on left-click");

                    int spIdx = arsenal.findFirstSkillIndexFromStand("jcraft:star_platinum#");
                    assertTrue(spIdx >= 0, "OriginArsenalScreen catalog exposes Star Platinum skills");
                    if (spIdx >= 0) {
                        int rowPitch = arsenal.getRowPitch();
                        int guard = 0;
                        while ((spIdx * rowPitch - arsenal.getScrollPixels()) > 48 && guard++ < 450) {
                            arsenal.mouseScrolled(arsenal.getFirstNonOriginSkillCenterX(), arsenal.getSkillRowCenterY(0), 0.0, -1.0);
                        }
                        double skillClickY = arsenal.getSkillRowCenterY(0) + (spIdx * rowPitch - arsenal.getScrollPixels());
                        boolean skillClicked = arsenal.mouseClicked(new MouseButtonEvent(
                                arsenal.getFirstNonOriginSkillCenterX(),
                                skillClickY,
                                new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                        assertTrue(skillClicked, "OriginArsenalScreen accepts click on catalog skill row");
                    }

                    // Also test the new ▼ arrow + [⇄ Swap] button workflow on SPECIAL1 using real SDL3 left-click
                    boolean sp1Selected = arsenal.mouseClicked(new MouseButtonEvent(
                            arsenal.getLeftSlotCenterX(),
                            arsenal.getLeftSlotCenterY(MoveClass.SPECIAL1),
                            new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                    assertTrue(sp1Selected && arsenal.getSelectedSlot() == MoveClass.SPECIAL1,
                            "OriginArsenalScreen selects SPECIAL1 bind slot on SDL3 left-click");
                    boolean arrowDownClicked = arsenal.mouseClicked(new MouseButtonEvent(
                            arsenal.getDownArrowButtonCenterX(),
                            arsenal.getDownArrowButtonCenterY(),
                            new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                    assertTrue(arrowDownClicked, "OriginArsenalScreen ▼ arrow button responds to SDL3 left-click");
                    boolean swapBtnClicked = arsenal.mouseClicked(new MouseButtonEvent(
                            arsenal.getEquipButtonCenterX(),
                            arsenal.getEquipButtonCenterY(),
                            new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
                    assertTrue(swapBtnClicked, "OriginArsenalScreen ⇄ Swap button responds to SDL3 left-click");
                }
                phase = 28;
                waitTicks = 6;
            }
            case 28 -> {
                String clientSlot = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginSlot(MoveClass.BARRAGE)
                        : null;
                String clientSp1 = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginSlot(MoveClass.SPECIAL1)
                        : null;
                assertTrue(clientSlot != null && clientSlot.startsWith("jcraft:star_platinum#"),
                        "Client CommonStandComponent synced BARRAGE -> " + clientSlot);
                assertTrue(clientSp1 != null && !clientSp1.startsWith("jcraft:origin#"),
                        "Client ▼ + ⇄ Swap bound SPECIAL1 to another Stand skill -> " + clientSp1);

                final IntegratedServer srv = client.getSingleplayerServer();
                final String[] serverSlot = new String[1];
                final String[] liveEntitySlot = new String[1];
                final String[] serverSp1 = new String[1];
                final String[] liveSp1 = new String[1];
                try {
                    srv.submit(() -> {
                        ServerPlayer sp = getServerPlayer(srv);
                        if (sp != null) {
                            serverSlot[0] = JComponentPlatformUtils.getStandComponent(sp).getOriginSlot(MoveClass.BARRAGE);
                            serverSp1[0] = JComponentPlatformUtils.getStandComponent(sp).getOriginSlot(MoveClass.SPECIAL1);
                            if (JUtils.getStand(sp) instanceof OriginEntity origin) {
                                var equipped = origin.getEquippedSkill(MoveClass.BARRAGE);
                                liveEntitySlot[0] = equipped != null ? equipped.id() : null;
                                var eqSp1 = origin.getEquippedSkill(MoveClass.SPECIAL1);
                                liveSp1[0] = eqSp1 != null ? eqSp1.id() : null;
                            }
                        }
                    }).get();
                } catch (Exception e) {
                    failures.add("Failed reading server Origin loadout: " + e.getMessage());
                }
                assertTrue(serverSlot[0] != null && serverSlot[0].startsWith("jcraft:star_platinum#"),
                        "IntegratedServer persisted Origin BARRAGE -> " + serverSlot[0]);
                assertTrue(liveEntitySlot[0] != null && liveEntitySlot[0].startsWith("jcraft:star_platinum#"),
                        "Live OriginEntity applied new BARRAGE skill -> " + liveEntitySlot[0]);
                assertTrue(serverSp1[0] != null && !serverSp1[0].startsWith("jcraft:origin#")
                                && serverSp1[0].equals(liveSp1[0]),
                        "IntegratedServer & live OriginEntity applied ▼ + ⇄ Swap SPECIAL1 -> " + liveSp1[0]);

                capture(client, "11_origin_arsenal_ui.png");
                if (client.gui.screen() instanceof OriginArsenalScreen arsenal) {
                    arsenal.selectTab(OriginArsenalScreen.TAB_STUDIO);
                    arsenal.applyChromaForTest(0x00FFAA);
                }
                phase = 29;
                waitTicks = 6;
            }
            case 29 -> {
                assertTrue(client.gui.screen() instanceof OriginArsenalScreen arsenal && arsenal.hasRendered3DPreview(),
                        "OriginArsenalScreen Tab 1 rendered live 3D rotating OriginEntity preview");
                int clientRgb = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginGlowRgb()
                        : -1;
                assertTrue(clientRgb == 0x00FFAA,
                        "Client synced custom Chroma RGB -> #00FFAA (actual=" + String.format("0x%06X", clientRgb) + ")");

                final IntegratedServer srv = client.getSingleplayerServer();
                final int[] serverGlow = {-1};
                final int[] entityGlow = {-1};
                try {
                    srv.submit(() -> {
                        ServerPlayer sp = getServerPlayer(srv);
                        if (sp != null) {
                            serverGlow[0] = JComponentPlatformUtils.getStandComponent(sp).getOriginGlowRgb();
                            if (JUtils.getStand(sp) instanceof OriginEntity origin) {
                                entityGlow[0] = origin.getCustomGlowRgb();
                            }
                        }
                    }).get();
                } catch (Exception e) {
                    failures.add("Failed reading server Chroma state: " + e.getMessage());
                }
                assertTrue(serverGlow[0] == 0x00FFAA && entityGlow[0] == 0x00FFAA,
                        "IntegratedServer & live OriginEntity applied Chroma RGB -> #00FFAA");

                capture(client, "13_origin_studio_3d_chroma.png");
                if (client.gui.screen() instanceof OriginArsenalScreen arsenal) {
                    arsenal.selectTab(OriginArsenalScreen.TAB_PROFILES);
                    arsenal.saveProfileForTest(0, "Boss Killer");
                }
                phase = 30;
                waitTicks = 5;
            }
            case 30 -> {
                OriginProfile clientProf = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginProfile(0)
                        : null;
                assertTrue(clientProf != null && clientProf.customGlowRgb() == 0x00FFAA
                                && "Boss Killer".equals(clientProf.name()),
                        "Client saved Origin Profile 0 (Boss Killer + Chroma #00FFAA)");

                final IntegratedServer srv = client.getSingleplayerServer();
                final OriginProfile[] serverProf = new OriginProfile[1];
                try {
                    srv.submit(() -> {
                        ServerPlayer sp = getServerPlayer(srv);
                        if (sp != null) {
                            serverProf[0] = JComponentPlatformUtils.getStandComponent(sp).getOriginProfile(0);
                        }
                    }).get();
                } catch (Exception e) {
                    failures.add("Failed reading server Origin Profile 0: " + e.getMessage());
                }
                assertTrue(serverProf[0] != null && serverProf[0].customGlowRgb() == 0x00FFAA,
                        "IntegratedServer persisted Origin Profile 0 cleanly");

                capture(client, "14_origin_profiles_synergies.png");
                if (client.player != null) {
                    CommonStandComponent cc = JComponentPlatformUtils.getStandComponent(client.player);
                    cc.clearOriginLoadout();
                    cc.setOriginGlowRgb(-1);
                }
                OriginLoadoutPacket.sendResetDefault();
                OriginLoadoutPacket.sendSetChroma(-1);
                phase = 31;
                waitTicks = 6;
            }
            case 31 -> {
                if (client.gui.screen() instanceof OriginArsenalScreen arsenal) {
                    arsenal.loadProfileForTest(0);
                }
                phase = 32;
                waitTicks = 6;
            }
            case 32 -> {
                String restoredSlot = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginSlot(MoveClass.BARRAGE)
                        : null;
                int restoredGlow = client.player != null
                        ? JComponentPlatformUtils.getStandComponent(client.player).getOriginGlowRgb()
                        : -1;
                assertTrue(restoredSlot != null && restoredSlot.startsWith("jcraft:star_platinum#") && restoredGlow == 0x00FFAA,
                        "1-click Profile Load restored BARRAGE (" + restoredSlot + ") and Chroma RGB (#"
                                + String.format("%06X", restoredGlow) + ")");

                client.gui.setScreen(null);
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                final IntegratedServer srv = client.getSingleplayerServer();
                final boolean[] swappedBarrageFired = {false};
                final String[] executedMoveName = {""};
                try {
                    srv.submit(() -> {
                        ServerPlayer sp = getServerPlayer(srv);
                        if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                            origin.setMoveStun(0);
                            boolean started = origin.initMove(MoveClass.BARRAGE);
                            var cur = origin.getCurrentMove();
                            swappedBarrageFired[0] = started && cur != null && origin.getMoveStun() > 0;
                            executedMoveName[0] = cur != null ? cur.getName().getString() : "null";
                        }
                    }).get();
                } catch (Exception e) {
                    failures.add("Failed executing swapped Origin BARRAGE: " + e.getMessage());
                }
                assertTrue(swappedBarrageFired[0],
                        "Live OriginEntity executed swapped Star Platinum BARRAGE move in-game (" + executedMoveName[0] + ")");
                phase = 325;
                waitTicks = 4;
            }
            case 325 -> {
                capture(client, "14b_origin_using_swapped_skill.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    StandEntity<?, ?> curStand = JUtils.getStand(sp);
                    if (curStand != null) {
                        curStand.setMoveStun(0);
                        curStand.discard();
                        sc.setStand(null);
                    }
                    sc.setOriginGlowRgb(-1);
                    sc.setSkin(5);
                    JCraft.summon(overworld, sp);
                });
                phase = 33;
                waitTicks = 8;
            }
            case 33 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity origin && origin.getSkin() == 5,
                        "OriginEntity summoned Skin 5 (Celestial Divinity) with custom pillar VFX");
                capture(client, "15_origin_summon_celestial.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setSkin(4);
                    JCraft.summon(overworld, sp);
                });
                phase = 34;
                waitTicks = 8;
            }
            case 34 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity origin && origin.getSkin() == 4,
                        "OriginEntity summoned Skin 4 (Absolute Abyss) with custom dark smoke wave VFX");
                capture(client, "16_origin_summon_abyss.png");
                StandRunicCircleRenderer.renderedCirclesThisSession = 0;
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.clearOriginLoadout();
                    sc.setOriginGlowRgb(-1);
                    sc.setSkin(5);
                    StandEntity<?, ?> summoned = JCraft.summon(overworld, sp);
                    if (summoned instanceof OriginEntity origin) {
                        origin.rebuildLoadoutFromUser(sc);
                        origin.setMoveStun(0);
                        origin.initMove(MoveClass.ULTIMATE);
                    }
                });
                phase = 35;
                waitTicks = 3;
            }
            case 35 -> {
                assertTrue(StandRunicCircleRenderer.renderedCirclesThisSession > 0,
                        "Runic charge circle rendered under Stand during Ultimate windup (draws="
                                + StandRunicCircleRenderer.renderedCirclesThisSession + ")");
                assertTrue(CombatImpactEffects.isActive(),
                        "CombatImpactEffects micro-shake & edge flash triggered on Ultimate activation");
                capture(client, "17_runic_circle_and_impact.png");

                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        origin.setMoveStun(0);
                        JComponentPlatformUtils.getCooldowns(sp).clear();
                        origin.initMove(MoveClass.UTILITY);
                    }
                });
                phase = 36;
                waitTicks = 3;
            }
            case 36 -> {
                assertTrue(AuraAfterimageManager.getActiveGhostCount() > 0,
                        "AuraAfterimageManager spawned aura-colored warp echoes on Time Skip (ghosts="
                                + AuraAfterimageManager.getActiveGhostCount() + ")");
                capture(client, "18_aura_afterimage_trail.png");

                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    JUtils.setDayTime(overworld, 18000L);
                    sp.setInvisible(true);
                    sp.teleportTo(overworld, 0.5, 113.0, 0.0, java.util.Set.of(), -28.0f, 5.0f, false);
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.KING_CRIMSON.get());
                    sc.setSkin(0);
                    JCraft.summon(overworld, sp);
                });
                if (client.player != null) {
                    client.player.setInvisible(true);
                }
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                phase = 37;
                waitTicks = 22;
            }
            case 37 -> {
                capture(client, "19_iconic_glow_king_crimson.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    sp.setInvisible(true);
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon();
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.MADE_IN_HEAVEN.get());
                    sc.setSkin(0);
                    JCraft.summon(overworld, sp);
                });
                phase = 38;
                waitTicks = 22;
            }
            case 38 -> {
                capture(client, "20_iconic_glow_made_in_heaven.png");
                phase = 381;
                waitTicks = 4;
            }
            case 381 -> {
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerLevel overworld = srv.getLevel(Level.OVERWORLD);
                    ServerPlayer sp = getServerPlayer(srv);
                    if (overworld == null || sp == null) return;
                    JUtils.setDayTime(overworld, 6000L);
                    sp.setInvisible(true);
                    sp.teleportTo(overworld, 0.5, 113.0, 0.0, java.util.Set.of(), 0.0f, 5.0f, false);
                    if (JUtils.getStand(sp) != null) {
                        JUtils.getStand(sp).desummon(false);
                    }
                    CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(sp);
                    sc.setType(JStandTypeRegistry.ORIGIN.get());
                    sc.clearOriginLoadout();
                    sc.setOriginGlowRgb(-1);
                    sc.setSkin(0);
                    StandEntity<?, ?> summoned = JCraft.summon(overworld, sp);
                    if (summoned instanceof OriginEntity o) {
                        o.rebuildLoadoutFromUser(sc);
                    }
                });
                if (client.player != null) {
                    client.player.setInvisible(true);
                    client.player.setYRot(0.0f);
                    client.player.setYHeadRot(0.0f);
                }
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                phase = 382;
                waitTicks = 12;
            }
            case 382 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity,
                        "Origin showcase Summon active in daylight with wings spread");
                capture(client, "21_origin_showcase_summon.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        JComponentPlatformUtils.getCooldowns(sp).clear();
                        origin.setMoveStun(0);
                        origin.initMove(MoveClass.BARRAGE);
                    }
                });
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                phase = 383;
                waitTicks = 6;
            }
            case 383 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity o
                                && o.getMoveStun() > 0 && o.getState() == OriginEntity.State.BARRAGE,
                        "Origin showcase Barrage active with open wings, spinning rings & smear arms");
                capture(client, "22_origin_showcase_barrage_wings.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        JComponentPlatformUtils.getCooldowns(sp).clear();
                        origin.setMoveStun(0);
                        origin.initMove(MoveClass.HEAVY);
                    }
                });
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                phase = 384;
                waitTicks = 11;
            }
            case 384 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity o
                                && o.getMoveStun() > 0 && o.getState() == OriginEntity.State.HEAVY,
                        "Origin showcase Heavy active with right-fist charge arc");
                capture(client, "23_origin_showcase_heavy.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null && JUtils.getStand(sp) instanceof OriginEntity origin) {
                        JComponentPlatformUtils.getCooldowns(sp).clear();
                        origin.setMoveStun(0);
                        origin.initMove(MoveClass.ULTIMATE);
                    }
                });
                phase = 385;
                waitTicks = 24;
            }
            case 385 -> {
                assertTrue(JUtils.getStand(client.player) instanceof OriginEntity o
                                && o.getMoveStun() > 0
                                && (o.getState() == OriginEntity.State.TIME_STOP || o.getState() == OriginEntity.State.ULTIMATE),
                        "Origin showcase Ultimate/Time Stop active with core pull & runic circle");
                capture(client, "24_origin_showcase_ultimate.png");
                phase = 39;
                waitTicks = 4;
            }
            case 39 -> {
                File shotDir = new File(getOutDir(), "screenshots");
                for (String name : expectedScreenshots) {
                    File f = new File(shotDir, name);
                    assertTrue(f.exists() && f.length() > 1024L, "Screenshot saved cleanly: " + name + " (" + f.length() + " bytes)");
                }
                finish(client);
            }
            case 100 -> {
                if (client.player != null) client.player.setInvisible(true);
                capture(client, "origin_0_default_front.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        sp.setYRot(-55.0f);
                        sp.setYHeadRot(-55.0f);
                    }
                });
                if (client.player != null) {
                    client.player.setYRot(-55.0f);
                    client.player.setYHeadRot(-55.0f);
                }
                phase = 101;
                waitTicks = 6;
            }
            case 101 -> {
                capture(client, "origin_0_default_diagonal.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        sp.setYRot(0.0f);
                        sp.setYHeadRot(0.0f);
                    }
                });
                if (client.player != null) {
                    client.player.setYRot(0.0f);
                    client.player.setYHeadRot(0.0f);
                }
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                phase = 102;
                waitTicks = 6;
            }
            case 102 -> {
                capture(client, "origin_0_default_back.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(1);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) origin.setSkin(1);
                    }
                });
                phase = 103;
                waitTicks = 8;
            }
            case 103 -> {
                capture(client, "origin_1_skin1_crimson.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(2);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) origin.setSkin(2);
                    }
                });
                phase = 104;
                waitTicks = 8;
            }
            case 104 -> {
                capture(client, "origin_2_skin2_cyan.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(3);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) origin.setSkin(3);
                    }
                });
                phase = 105;
                waitTicks = 8;
            }
            case 105 -> {
                capture(client, "origin_3_skin3_gold.png");
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(4);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) origin.setSkin(4);
                    }
                });
                phase = 106;
                waitTicks = 10;
            }
            case 106 -> {
                capture(client, "origin_4_skin4_abyss_front.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                phase = 107;
                waitTicks = 6;
            }
            case 107 -> {
                capture(client, "origin_4_skin4_abyss_back.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(5);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) origin.setSkin(5);
                    }
                });
                phase = 108;
                waitTicks = 12;
            }
            case 108 -> {
                capture(client, "origin_5_skin5_celestial_front.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                phase = 109;
                waitTicks = 6;
            }
            case 109 -> {
                capture(client, "origin_5_skin5_celestial_back.png");
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                final IntegratedServer srv = client.getSingleplayerServer();
                srv.execute(() -> {
                    ServerPlayer sp = getServerPlayer(srv);
                    if (sp != null) {
                        JComponentPlatformUtils.getStandComponent(sp).setSkin(0);
                        if (JUtils.getStand(sp) instanceof OriginEntity origin) {
                            origin.setSkin(0);
                            origin.initMove(MoveClass.BARRAGE);
                        }
                    }
                });
                phase = 110;
                waitTicks = 12;
            }
            case 110 -> {
                capture(client, "origin_0_default_barrage.png");
                phase = 39;
                waitTicks = 4;
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
