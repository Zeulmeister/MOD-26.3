package net.arna.jcraft.client.events;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.architectury.event.EventResult;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.registries.RegistrySupplier;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.registry.JPacketRegistry;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.sound.AerosmithSoundInstance;
import net.arna.jcraft.client.tracer.MuzzleTracker;
import net.arna.jcraft.client.util.JClientUtils;
import net.arna.jcraft.client.input.TrackedKeyBinding;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.arna.jcraft.common.network.c2s.PlayerInputPacket;
import net.arna.jcraft.common.network.c2s.StandBlockPacket;
import net.arna.jcraft.common.network.c2s.VariantInputPacket;
import net.arna.jcraft.common.tickable.Timestops;
import net.arna.jcraft.common.util.*;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.arna.jcraft.client.JCraftClient.*;
import static net.arna.jcraft.client.gui.hud.JCraftAbilityHud.cooldownTypeToKeybind;
import static net.arna.jcraft.client.gui.hud.JCraftAbilityHud.getHudX;
import static net.arna.jcraft.client.util.JClientUtils.activeTimestops;

@Environment(EnvType.CLIENT)
public class JClientEvents {

    // Tracks the game-time tick for each stand user (by UUID)
    // the 100-block menacing radius. Cleared when they leave range.
    private static final Map<UUID, Integer> menacingEntryTimes = new HashMap<>();
    // tracks which entities were last seen, cleared after a certain time only
    private static final Map<UUID, Integer> menacingCheckTimes = new HashMap<>();

    public static void onLast(final PoseStack matrixStack, final Vec3 cameraPos) {
    }

    public static void afterTranslucent(final PoseStack matrixStack, final Vec3 cameraPos, final LevelRenderer worldRenderer) {
    }

    public static void clientPlayerJoin(final LocalPlayer clientPlayerEntity) {
        if (clientPlayerEntity == null) {
            JCraft.LOGGER.fatal("onPlayReady was called with invalid client player!");
            return;
        }

        // Clear any stale Time Stop / shader / kinetic HUD state from a previous world or server session.
        JClientUtils.clearTimestops();
        net.arna.jcraft.client.rendering.handler.ZaWarudoShaderHandler.INSTANCE.reset();
        net.arna.jcraft.client.gui.hud.JoJoAnimeHudRenderer.setClientSuspendedProjectiles(0);

        // Sync initial prediction option
        // net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_PREDICTION_TRIGGER, PredictionTriggerPacket.write(JClientConfig.getInstance().isClientsidePrediction()));
    }

    public static void renderHud(final GuiGraphicsExtractor ctx, final float tickDelta) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null) {
            JCraft.LOGGER.fatal("Attempted to render hud with no player!");
            return;
        }

        framesSinceCounted++;

        final JClientConfig config = JClientConfig.getInstance();

        final boolean isMid = config.getUiPosition() == JClientConfig.UIPos.MIDDLE;
        final boolean useIcons = config.isIconHud();

        final StandEntity<?, ?> stand = JUtils.getStand(player);
        final Font textRenderer = client.font;

        int selectedX = getHudX(client.getWindow().getGuiScaledWidth(), 128) + config.getHorizontalHudOffset();
        int selectedY = client.getWindow().getGuiScaledHeight() + config.getVerticalHudOffset();

        switch (config.getUiPosition()) {
            case LEFT -> selectedY /= 20;
            case MIDDLE -> selectedY /= 3;
            case RIGHT -> selectedY = (int) (selectedY / 2.25f);
        }

        // Draw text HUD
        if (!useIcons) {
            final CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(player);

            final CooldownType[] values = CooldownType.values();
            for (int i = 0; i < values.length; i++) {
                final CooldownType type = values[i];
                final int cooldownTicks = cooldowns.getCooldown(type);

                if (cooldownTicks == 0) {
                    continue;
                }
                final double cooldown = (cooldownTicks - tickDelta) / 20d;

                // These are (mainly) based off of keybindings which are client-only and thus have
                // to be done here and cannot be done in CooldownType.
                final String keyBindText = cooldownTypeToKeybind(type, false);

                CooldownType.Category category = type.getCategory();

                final boolean isSpec = category == CooldownType.Category.SPEC;

                boolean onStandHUD = stand != null;

                if (stand instanceof AerosmithEntity as && !as.allowMoveHandling())
                    onStandHUD = false;

                final boolean isUniversal = category == CooldownType.Category.UNIVERSAL;
                float defaultAlpha = 0.65f;
                int xOffset = 0;

                String finalText = keyBindText + " - " + JCraftClient.decimalFormat.get().format(Mth.clamp(cooldown, 0.0, 9999.0)) + "s";

                if (category == CooldownType.Category.STAND || isSpec) {
                    if (!isSpec) {
                        finalText = "s." + finalText;
                    }

                    if (
                            (isSpec && onStandHUD) ||
                            (!isSpec && !onStandHUD)
                    ) {
                        xOffset = 48;
                        defaultAlpha = 0.3f;
                    }
                }

                int offsetIndex = i;
                if (isSpec) {
                    offsetIndex -= 7;
                } else if (isUniversal) {
                    offsetIndex -= 6;
                }
                final float offsetY = selectedY * 1.25f + 9f * offsetIndex;

                ctx.text(
                        textRenderer,
                        finalText,
                        selectedX + xOffset,
                        (int) offsetY,
                        ColorUtils.HSBAtoRGBA(0.3f - (float) cooldown * 10f / 720f, (cooldown < 1.6) ? 0.0f : 1.0f, 1.0f, (cooldown < 1.6) ? 1.0f : defaultAlpha)
                );

            }
        }

        // Draw Combo Counter
        if (comboCounter > 0 && JClientConfig.getInstance().isComboCounter() && framesSinceCounted <= 180) {
            String remark = "epic tod free download";
            if (comboCounter < JCraftClient.comboRemarks.size() * 7) {
                remark = comboRemarks.get(Math.floorDiv(comboCounter, 7));
            }

            final boolean recentHit = framesSinceCounted < 5;

            final RandomSource random = player.getRandom();

            if (comboStarted && ++framesSinceComboStarted > 59) {
                comboStarted = false;
            }

            boolean ipsTriggered = IPSTriggerFramesLeft-- > 0;
            if (ipsTriggered) {
                selectedX += random.nextFloat() * IPSTriggerFramesLeft / 20.0f;
                selectedY += random.nextFloat() * IPSTriggerFramesLeft / 20.0f;
            }

            ctx.text(
                    textRenderer,
                    comboCounter + " - (" + Math.round(damageScaling * 100f) + "%) - " + remark,
                    (int) (selectedX + (isMid && useIcons ? 54f : 0) + (recentHit ? tickDelta * random.nextFloat() * 5f : 0)),
                    (int) (selectedY * (1.15f) + (recentHit ? tickDelta * random.nextFloat() * 5f : 0)),
                    ipsTriggered ?
                            ColorUtils.HSBAtoRGBA(0.5f, (IPS_TRIGGER_FRAMES - IPSTriggerFramesLeft) / (float)IPS_TRIGGER_FRAMES, 1f, 0.8f) :
                            ColorUtils.HSBAtoRGBA(comboCounter / 360f - 1f, comboStarted ? framesSinceComboStarted / 60f : 1f, 1f, 0.8f)
            );
        }

        net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.render(ctx, tickDelta);
        net.arna.jcraft.client.gui.hud.JoJoAnimeHudRenderer.render(ctx, tickDelta);
    }

    public static void tickClient(final Minecraft client) {
        final LocalPlayer player = client.player;
        if (player == null) {
            net.arna.jcraft.client.remote.StandRemoteControlHandler.resetCamera(client);
            return;
        }

        JCraftClient.sanitizeKeyBindings(client);

        // Quick 8-Slice JoJo Radial Wheel (default key: Y)
        if (client.gui.screen() == null && RADIAL_MENU_KEY != null && RADIAL_MENU_KEY.isPressedThisTick()) {
            client.gui.setScreen(new net.arna.jcraft.client.gui.screen.JoJoRadialWheelScreen());
        }

        final StandEntity<?, ?> stand = JUtils.getStand(player);

        // Sync aerial and crouch variants BEFORE move inputs so variant moves see up-to-date flags on the same tick
        final boolean aerialChanged = AERIAL_VARIANT_KEY.isChangedThisTick();
        final boolean crouchChanged = CROUCH_VARIANT_KEY.isChangedThisTick();
        if (aerialChanged || crouchChanged) {
            FriendlyByteBuf buf = VariantInputPacket.write(AERIAL_VARIANT_KEY.isDown(), CROUCH_VARIANT_KEY.isDown());
            net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_VARIANT, buf);
        }

        // Handle JCraft inputs (stand, spec, universal controls)
        if (player.isAlive()) {
            final Object2BooleanMap<MovementInputType> movementInput = getChangedInputs(getMovementBindings());
            final Object2BooleanMap<MoveInputType> moveInput = getChangedInputs(getBindings());

            if (!movementInput.isEmpty() || !moveInput.isEmpty()) {
                net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_PLAYER_INPUT, PlayerInputPacket.write(movementInput, moveInput));
            }

            // 6-Tick Smart Combat Input Buffer
            net.arna.jcraft.client.combat.CombatInputBuffer.onClientTick(
                    player, moveInput, JClientConfig.getInstance().isInputBufferEnabled());

            final Object2BooleanMap<MoveInputType> heldMoves = new Object2BooleanOpenHashMap<>();
            getBindings().forEach((key, value) -> {
                if (key.isDown()) {
                    heldMoves.put(value, true);
                }
            });

            if (!heldMoves.isEmpty()) {
                FriendlyByteBuf buf = PlayerInputPacket.write(null, heldMoves);
                net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_PLAYER_INPUT_HOLD, buf);
            }
        }

        // Block
        if (getTrackedUseKey().isChangedThisTick()) {
            final boolean pressed = getTrackedUseKey().isPressedThisTick();
            net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_STAND_BLOCK, StandBlockPacket.write(pressed));
            if (stand != null && stand.isRemoteAndControllable() && pressed) {
                net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_REMOTE_STAND_INTERACT, new FriendlyByteBuf(Unpooled.buffer()));
            }
        }

        // Cooldown Cancel
        if (COOLDOWN_CANCEL_KEY.isPressedThisTick()) {
            net.arna.jcraft.common.network.JNetwork.sendToServer(JPacketRegistry.C2S_COOLDOWN_CANCEL, new FriendlyByteBuf(Unpooled.buffer()));
        }

        // Custom 1.20.1 + 26.3 Client Feature Handlers
        net.arna.jcraft.client.zoom.StarPlatinumPrecisionHandler.tick(client);
        net.arna.jcraft.client.taunt.PlayerPoseTauntHandler.tick(client);
        net.arna.jcraft.client.remote.StandRemoteControlHandler.tick(client);
        net.arna.jcraft.client.rendering.JoJoOnomatopoeiaRenderer.tick(client);

        if (client.isPaused() && client.isLocalServer()) {
            return;
        }

        // Timestop handling (dynamically follows active timestopper across 160m sphere)
        final Iterator<DimensionData> iter = activeTimestops.iterator();

        while (iter.hasNext()) {
            final DimensionData timestop = iter.next();
            if (timestop == null) {
                iter.remove();
                continue;
            }
            final LivingEntity user = timestop.getUser();

            if (user != null && user.isAlive() && !user.isRemoved() && user.level() == client.level
                    && timestop.getWorldKey().equals(client.level.dimension())) {
                timestop.decreaseTimer();
                if (timestop.getTimer() <= 0) {
                    iter.remove();
                    continue;
                }
            }
            else {
                iter.remove();
                continue;
            }

            final Vec3 pos = user.position();
            timestop.setPos(pos);

            final boolean hasLocalServer = client.hasSingleplayerServer();
            final List<? extends Entity> toStop = user.level().getEntitiesOfClass(Entity.class,
                    new AABB(pos.add(160.0, 160.0, 160.0), pos.subtract(160.0, 160.0, 160.0)), Timestops.TIMESTOP_PREDICATE);

            for (final Entity entity : toStop) {
                if (entity == user || entity == JUtils.getStand(user) || entity == user.getVehicle() || user.isPassengerOfSameVehicle(entity)) {
                    continue;
                }
                final boolean allowKineticTravel = hasLocalServer
                        ? net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.shouldAllowInitialTravel(entity, user)
                        : (entity instanceof net.minecraft.world.entity.projectile.Projectile proj
                            && net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.isOwnedByTimestopUser(proj, user)
                            && entity.tickCount <= net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.MAX_INITIAL_TRAVEL_TICKS + 1
                            && entity.getDeltaMovement().lengthSqr() > net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.SUSPENSION_SPEED_SQR_THRESHOLD);
                if (!allowKineticTravel) {
                    JComponentPlatformUtils.getTimeStopData(entity)
                            .ifPresent(d -> d.setClientTicks(3));
                }
            }
        }
        TrackedKeyBinding.resetValues(client.gui.screen() != null);

        // Menacing (ゴ/ド) particles — 4-second burst when a stand user enters 100-block radius.
        tickMenacing(client, player);
    }

    @Nullable
    private static Vec3 getMainHandMuzzle(Minecraft client, Object2BooleanMap<MoveInputType> moveInput) {
        if (client.player == null || !client.options.getCameraType().isFirstPerson()) {
            return null;
        }
        if (!moveInput.getOrDefault(MoveInputType.LIGHT, false)) {
            return null;
        }
        return MuzzleTracker.get(client.player.getUUID(), InteractionHand.MAIN_HAND);
    }

    public static EventResult onEntityAdd(final Entity entity, final Level level) {
        if (entity instanceof AerosmithEntity aerosmith) {
            Minecraft.getInstance().getSoundManager().play(new AerosmithSoundInstance(aerosmith));
        }

        return EventResult.pass();
    }

    private static void tickMenacing(final Minecraft client, final LocalPlayer player) {
        final ClientLevel level = client.level;
        final boolean playerWarning = JClientConfig.getInstance().isShowStandUserWarningPlayer();
        final boolean mobWarning = JClientConfig.getInstance().isShowStandUserWarningMob();
        if (level == null || (!playerWarning && !mobWarning) || player.isSpectator() || player.isInvisible()) {
            return;
        }

        // Local player must be a stand user themselves
        final StandType type = JComponentPlatformUtils.getStandComponent(player).getType();
        if (StandTypeUtil.isNone(type)) {
            menacingEntryTimes.clear();
            menacingCheckTimes.clear();
            return;
        }

        if (!JClientUtils.shouldRenderStands()) {
            menacingEntryTimes.clear();
            menacingCheckTimes.clear();
            return;
        }

        final double radius = 100.0;
        final double radiusSq = radius * radius;
        final AABB searchBox = AABB.ofSize(player.position(), radius * 2, radius * 2, radius * 2);
        final Set<UUID> inRangeIds = new HashSet<>();

        // find all stand users nearby, for each do
        if (playerWarning) {
            int count = 0;
            for (final Player p : level.getEntitiesOfClass(Player.class, searchBox,
                    p -> {
                        var pType = JComponentPlatformUtils.getStandComponent(p).getType();
                        return p != player && !p.isSpectator() && !p.isCreative()
                                && p.distanceToSqr(player) <= radiusSq
                                && !p.isInvisible() && !JClientUtils.shouldNotRender(p)
                                && !StandTypeUtil.isNone(pType);
                    }
            )) {
                tickMenacing(p, inRangeIds, level, JParticleTypeRegistry.DO, count++ > 15);
            }
        }

        // get all other stand users via their stands
        if (mobWarning) {
            int count = 0;
            for (final StandEntity<?, ?> stand : level.getEntitiesOfClass(
                    StandEntity.class, searchBox,
                    stand -> stand.hasUser() && stand.distanceToSqr(player) <= radiusSq && !stand.isInvisible())
            ) {
                final LivingEntity user = stand.getUserOrThrow();
                if (user instanceof Player) { // handled before
                    continue;
                }
                if (JClientUtils.shouldNotRender(user)) {
                    continue;
                }
                tickMenacing(user, inRangeIds, level, JParticleTypeRegistry.GO, count++ > 15);
            }
        }

        // Remove entries for stand users who left range
        menacingEntryTimes.keySet().removeIf(id -> !inRangeIds.contains(id));
        // remove entries for stand users who have been checked for a long time
        menacingCheckTimes.entrySet().removeIf(entry -> entry.getValue() > 600);
    }

    private static void tickMenacing(final LivingEntity living, final Set<UUID> inRangeIds, final ClientLevel level, final RegistrySupplier<SimpleParticleType> particle, boolean limited) {
        final UUID uuid = living.getUUID();
        inRangeIds.add(uuid);
        menacingEntryTimes.putIfAbsent(uuid, -1);
        menacingEntryTimes.put(uuid, menacingEntryTimes.get(uuid) + 1);
        menacingCheckTimes.putIfAbsent(uuid, -1);
        menacingCheckTimes.put(uuid, menacingCheckTimes.get(uuid) + 1);
        if (menacingEntryTimes.get(uuid) >= 80 || menacingCheckTimes.get(uuid) >= 80) {
            return;
        }
        final RandomSource rng = level.getRandom();
        if (limited || rng.nextDouble() > 1d/8) {
            return;
        }
        spawnMenacingParticle(level, rng, particle.get());
    }

    private static void spawnMenacingParticle(final ClientLevel level, final RandomSource rng,
                                              final SimpleParticleType type) {
        LivingEntity entity = Minecraft.getInstance().player;
        if (entity == null) {
            return;
        }
        final Vec3 pos = entity.position();
        final float spread = entity.getBbWidth() * 2.0f;
        final double minY = pos.y + entity.getBbHeight() * 0.7;
        final double maxY = pos.y + entity.getBbHeight() * 1.4;
        level.addParticle(type, false, false, pos.x + rng.triangle(0, spread), minY + rng.nextDouble() * (maxY - minY), pos.z + rng.triangle(0, spread), 0, 0, 0);
    }
}
