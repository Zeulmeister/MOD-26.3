package net.arna.jcraft.client.gui.hud;

import dev.architectury.event.events.client.ClientGuiEvent;
import lombok.experimental.UtilityClass;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.arna.jcraft.common.entity.stand.MandomEntity;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.arna.jcraft.client.JCraftClient.*;

/**
 * Clean, unobtrusive Stand & Spec Ability Dock:
 * - Hidden completely when Stand is inactive (except Spec moves on cooldown / ALWAYS mode)
 * - Supports configurable HUD Scale (0.75x - 1.50x) anchored cleanly to its screen corner
 * - Highlights actively executing abilities via synced Stand state, shows crouch/aerial variant pips
 *   when holding Sneak or airborne, and hides universal chips when UNIVERSAL_ABILITIES is disabled.
 */
@UtilityClass
public class JCraftAbilityHud {
    public static final Identifier GUI_ICONS_TEXTURE = Identifier.parse("textures/gui/icons.png");
    private static final Identifier BORDER = JCraft.id("textures/gui/ability_icons/border.png");
    private static final Identifier OVERLAY = JCraft.id("textures/gui/ability_icons/overlay.png");
    private static final Identifier UNIVERSAL_ID = JCraft.id("universal");

    private static final int TILE_SIZE = 20;
    private static final int ICON_SIZE = 16;
    private static final int TILE_GAP = 3;
    private static final int UNIVERSAL_TILE = 16;
    private static final int UNIVERSAL_ICON = 12;

    private static final List<AbilitySlot> STAND_SLOTS = List.of(
            new AbilitySlot(CooldownType.STAND_LIGHT, MoveClass.LIGHT, "light"),
            new AbilitySlot(CooldownType.STAND_HEAVY, MoveClass.HEAVY, "heavy"),
            new AbilitySlot(CooldownType.STAND_BARRAGE, MoveClass.BARRAGE, "barrage"),
            new AbilitySlot(CooldownType.UTILITY, MoveClass.UTILITY, "utility"),
            new AbilitySlot(CooldownType.STAND_SP1, MoveClass.SPECIAL1, "special1"),
            new AbilitySlot(CooldownType.STAND_SP2, MoveClass.SPECIAL2, "special2"),
            new AbilitySlot(CooldownType.STAND_SP3, MoveClass.SPECIAL3, "special3"),
            new AbilitySlot(CooldownType.STAND_ULTIMATE, MoveClass.ULTIMATE, "ult")
    );

    private static final List<AbilitySlot> SPEC_SLOTS = List.of(
            new AbilitySlot(CooldownType.HEAVY, null, "heavy"),
            new AbilitySlot(CooldownType.BARRAGE, null, "barrage"),
            new AbilitySlot(CooldownType.SPECIAL1, null, "special1"),
            new AbilitySlot(CooldownType.SPECIAL2, null, "special2"),
            new AbilitySlot(CooldownType.SPECIAL3, null, "special3")
    );

    private static final List<AbilitySlot> UNIVERSAL_SLOTS = List.of(
            new AbilitySlot(CooldownType.DASH, null, "dash"),
            new AbilitySlot(CooldownType.COOLDOWN_CANCEL, null, "cooldowncancel"),
            new AbilitySlot(CooldownType.COMBO_BREAKER, null, "combobreaker")
    );

    private static final Map<Identifier, Boolean> TEXTURE_EXISTS_CACHE = new ConcurrentHashMap<>();
    private static final Map<CooldownType, Boolean> WAS_COOLING_DOWN = new EnumMap<>(CooldownType.class);
    private static final Map<CooldownType, Long> READY_PULSE_UNTIL_MS = new EnumMap<>(CooldownType.class);
    private static final long READY_PULSE_DURATION_MS = 650L;

    private record AbilitySlot(CooldownType cooldownType, @Nullable MoveClass moveClass, String iconName) {}

    private record ResolvedSlot(
            CooldownType cooldownType,
            Identifier iconOwner,
            String iconName,
            boolean activeNow,
            boolean crouchVariantActive,
            boolean aerialVariantActive
    ) {}

    public static float getClampedHudScale() {
        return JClientConfig.getInstance().getClampedHudScale();
    }

    public static int getHudX(final int scaledX, final int rightOffset) {
        return switch (JClientConfig.getInstance().getUiPosition()) {
            case LEFT -> 8;
            case RIGHT -> scaledX - rightOffset;
            case MIDDLE -> (int) (scaledX * 0.55f);
        };
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register((ctx, tickDelta) -> render(ctx));
    }

    public static void clearTextureCache() {
        TEXTURE_EXISTS_CACHE.clear();
    }

    private static boolean matchesActiveState(final MoveMap.Entry<?, ?> entry, final @Nullable Enum<?> activeState) {
        if (activeState == null || entry == null) {
            return false;
        }
        if (activeState == entry.getAnimState()) {
            return true;
        }
        if (entry.getCrouchingVariant() != null && activeState == entry.getCrouchingVariant().getAnimState()) {
            return true;
        }
        if (entry.getAerialVariant() != null && activeState == entry.getAerialVariant().getAnimState()) {
            return true;
        }
        return entry.getFollowup() != null && activeState == entry.getFollowup().getAnimState();
    }

    public static void render(final GuiGraphicsExtractor ctx) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null || player.isSpectator()) {
            return;
        }

        final JClientConfig cfg = JClientConfig.getInstance();
        if (!cfg.isIconHud() || cfg.getUiDuration() == JClientConfig.UIDuration.NEVER) {
            return;
        }

        final StandEntity<?, ?> stand = JUtils.getStand(player);
        final JSpec<?, ?> spec = JUtils.getSpec(player);
        final CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(player);

        final boolean standActiveForMoves = stand != null
                && !(stand instanceof AerosmithEntity as && !as.isAllowingMoveHandling());
        final boolean crouching = player.isShiftKeyDown();
        final boolean airborne = !player.onGround() && !player.isInWater();
        final Enum<?> activeStandState = (standActiveForMoves && stand.getMoveStun() > 0 && !stand.blocking)
                ? stand.getState()
                : null;

        final List<ResolvedSlot> slots = new ArrayList<>();
        if (standActiveForMoves) {
            final Identifier standId = stand.getStandType().getId();
            if (stand instanceof MandomEntity && spec != null) {
                final Identifier specId = spec.getType().getId();
                for (AbilitySlot s : SPEC_SLOTS) {
                    slots.add(new ResolvedSlot(s.cooldownType(), specId, s.iconName(), false, false, false));
                }
                slots.add(resolveStandSlot(stand, new AbilitySlot(CooldownType.UTILITY, MoveClass.UTILITY, "utility"),
                        standId, activeStandState, crouching, airborne));
                slots.add(resolveStandSlot(stand, new AbilitySlot(CooldownType.STAND_ULTIMATE, MoveClass.ULTIMATE, "ult"),
                        standId, activeStandState, crouching, airborne));
            } else {
                final boolean hasMoveMapEntries = !stand.getMoveMap().getEntries().isEmpty();
                for (AbilitySlot s : STAND_SLOTS) {
                    if (hasMoveMapEntries && s.moveClass() != null
                            && stand.getMoveMap().getEntries(s.moveClass()).isEmpty()) {
                        continue;
                    }
                    slots.add(resolveStandSlot(stand, s, standId, activeStandState, crouching, airborne));
                }
                if (slots.isEmpty()) {
                    for (AbilitySlot s : STAND_SLOTS) {
                        slots.add(new ResolvedSlot(s.cooldownType(), standId, s.iconName(), false, false, false));
                    }
                }
            }
        } else if (spec != null) {
            // When Stand is OFF, keep screen calm: show Spec dock only if ALWAYS or while any Spec skill is on cooldown
            boolean anySpecCooldown = false;
            for (AbilitySlot s : SPEC_SLOTS) {
                if (cooldowns.getCooldown(s.cooldownType()) > 0) {
                    anySpecCooldown = true;
                    break;
                }
            }
            if (cfg.getUiDuration() == JClientConfig.UIDuration.ALWAYS || anySpecCooldown) {
                final Identifier specId = spec.getType().getId();
                for (AbilitySlot s : SPEC_SLOTS) {
                    slots.add(new ResolvedSlot(s.cooldownType(), specId, s.iconName(), false, false, false));
                }
            }
        }

        // Collect universal skills that are currently on cooldown (hidden if UNIVERSAL_ABILITIES is disabled)
        final List<ResolvedSlot> activeUniversalCooldowns = new ArrayList<>();
        if (JServerConfig.UNIVERSAL_ABILITIES.getValue()) {
            for (AbilitySlot u : UNIVERSAL_SLOTS) {
                if (cooldowns.getCooldown(u.cooldownType()) > 0) {
                    activeUniversalCooldowns.add(new ResolvedSlot(u.cooldownType(), UNIVERSAL_ID, u.iconName(), false, false, false));
                }
            }
        }

        if (slots.isEmpty() && activeUniversalCooldowns.isEmpty()) {
            return;
        }

        final int sw = client.getWindow().getGuiScaledWidth();
        final int sh = client.getWindow().getGuiScaledHeight();
        final Font font = client.font;
        final JClientConfig.UIPos uiPos = cfg.getUiPosition();
        final boolean horizontalStrip = uiPos == JClientConfig.UIPos.MIDDLE;

        final int cols = horizontalStrip ? Math.max(1, slots.size()) : Math.min(4, Math.max(1, slots.size()));
        final int rows = slots.isEmpty() ? 0 : (int) Math.ceil(slots.size() / (double) cols);
        final int gridW = cols * TILE_SIZE + Math.max(0, cols - 1) * TILE_GAP;
        final int gridH = rows * TILE_SIZE + Math.max(0, rows - 1) * TILE_GAP;

        final int baseX;
        final int baseY;
        final float anchorX;
        final float anchorY;
        if (horizontalStrip) {
            baseX = (sw - gridW) / 2 + cfg.getHorizontalHudOffset();
            baseY = sh - 66 - gridH + cfg.getVerticalHudOffset();
            anchorX = baseX + gridW * 0.5f;
            anchorY = baseY + gridH;
        } else if (uiPos == JClientConfig.UIPos.LEFT) {
            baseX = 8 + cfg.getHorizontalHudOffset();
            baseY = sh - 8 - gridH + cfg.getVerticalHudOffset();
            anchorX = baseX;
            anchorY = baseY + gridH;
        } else {
            baseX = sw - gridW - 8 + cfg.getHorizontalHudOffset();
            baseY = sh - 8 - gridH + cfg.getVerticalHudOffset();
            anchorX = baseX + gridW;
            anchorY = baseY + gridH;
        }

        final float hudScale = getClampedHudScale();
        final var pose = ctx.pose();
        final boolean applyScale = Math.abs(hudScale - 1.0f) > 0.005f;
        if (applyScale) {
            pose.pushMatrix();
            pose.translate(anchorX, anchorY);
            pose.scale(hudScale, hudScale);
            pose.translate(-anchorX, -anchorY);
        }

        // Subtle integrated Stand name + 3px Guard bar directly above the ability dock when Stand is summoned
        if (standActiveForMoves && cfg.isAnimeHudEnabled() && !slots.isEmpty()) {
            JoJoAnimeHudRenderer.drawMinimalStandHeader(ctx, font, stand, baseX, baseY - 14, gridW);
        }

        final long nowMs = System.currentTimeMillis();
        for (int i = 0; i < slots.size(); i++) {
            final ResolvedSlot slot = slots.get(i);
            final int col = i % cols;
            final int row = i / cols;
            final int tx = baseX + col * (TILE_SIZE + TILE_GAP);
            final int ty = baseY + row * (TILE_SIZE + TILE_GAP);
            renderAbilityTile(ctx, font, cooldowns, slot, tx, ty, TILE_SIZE, ICON_SIZE, nowMs);
        }

        // Render active universal cooldown micro-chips neatly beside the dock so they never clutter idle gameplay
        if (!activeUniversalCooldowns.isEmpty()) {
            final int chipGap = 2;
            final int stripW = activeUniversalCooldowns.size() * UNIVERSAL_TILE
                    + Math.max(0, activeUniversalCooldowns.size() - 1) * chipGap;
            final int chipY = (standActiveForMoves && cfg.isAnimeHudEnabled() && !slots.isEmpty())
                    ? (baseY - 14 - UNIVERSAL_TILE - 4)
                    : (slots.isEmpty() ? sh - 8 - UNIVERSAL_TILE + cfg.getVerticalHudOffset() : baseY - UNIVERSAL_TILE - 4);
            final int chipStartX = uiPos == JClientConfig.UIPos.LEFT
                    ? baseX
                    : (baseX + Math.max(gridW, stripW) - stripW);
            for (int i = 0; i < activeUniversalCooldowns.size(); i++) {
                final ResolvedSlot u = activeUniversalCooldowns.get(i);
                renderAbilityTile(ctx, font, cooldowns, u, chipStartX + i * (UNIVERSAL_TILE + chipGap), chipY,
                        UNIVERSAL_TILE, UNIVERSAL_ICON, nowMs);
            }
        }

        if (applyScale) {
            pose.popMatrix();
        }
    }

    private static ResolvedSlot resolveStandSlot(
            final StandEntity<?, ?> stand,
            final AbilitySlot slot,
            final Identifier standId,
            final @Nullable Enum<?> activeState,
            final boolean crouching,
            final boolean airborne
    ) {
        boolean activeNow = false;
        boolean hasCrouch = false;
        boolean hasAerial = false;
        if (slot.moveClass() != null) {
            for (MoveMap.Entry<?, ?> entry : stand.getMoveMap().getEntries(slot.moveClass())) {
                if (matchesActiveState(entry, activeState)) {
                    activeNow = true;
                }
                if (entry.getCrouchingVariant() != null) {
                    hasCrouch = true;
                }
                if (entry.getAerialVariant() != null) {
                    hasAerial = true;
                }
            }
        }
        Identifier iconOwner = standId;
        String iconName = slot.iconName();
        if (stand instanceof net.arna.jcraft.common.entity.stand.OriginEntity origin && slot.moveClass() != null) {
            net.arna.jcraft.common.stand.OriginSkillCatalog.SkillEntry eq = origin.getEquippedSkill(slot.moveClass());
            if (eq != null) {
                iconOwner = eq.standId();
                iconName = eq.getIconName();
            }
        }
        return new ResolvedSlot(
                slot.cooldownType(),
                iconOwner,
                iconName,
                activeNow,
                crouching && hasCrouch,
                airborne && hasAerial
        );
    }

    private static void renderAbilityTile(
            final GuiGraphicsExtractor ctx,
            final Font font,
            final CommonCooldownsComponent cooldowns,
            final ResolvedSlot slot,
            final int x,
            final int y,
            final int tileSize,
            final int iconSize,
            final long nowMs
    ) {
        final CooldownType type = slot.cooldownType();
        final int remainingTicks = cooldowns.getCooldown(type);
        final int initialDuration = cooldowns.getInitialDuration(type);
        final boolean coolingDown = remainingTicks > 0 && initialDuration > 0;
        final double progress = coolingDown ? Mth.clamp(remainingTicks / (double) initialDuration, 0.0, 1.0) : 0.0;

        final boolean wasCooling = WAS_COOLING_DOWN.getOrDefault(type, false);
        if (wasCooling && !coolingDown) {
            READY_PULSE_UNTIL_MS.put(type, nowMs + READY_PULSE_DURATION_MS);
        }
        WAS_COOLING_DOWN.put(type, coolingDown);

        final long pulseUntil = READY_PULSE_UNTIL_MS.getOrDefault(type, 0L);
        final float pulse = pulseUntil > nowMs
                ? Mth.clamp((pulseUntil - nowMs) / (float) READY_PULSE_DURATION_MS, 0.0f, 1.0f)
                : 0.0f;

        // 1. Soft translucent dark tile background (subtle golden wash while actively executing)
        int bgColor = slot.activeNow() ? 0xAA2D1C4A : (coolingDown ? 0x80090710 : 0x6E110E1B);
        ctx.fill(x, y, x + tileSize, y + tileSize, bgColor);

        // 2. Crisp ability logo centered in tile
        final int iconPad = (tileSize - iconSize) / 2;
        final float iconAlpha = slot.activeNow() ? 1.0f : (coolingDown ? 0.30f : 0.90f);
        final int iconTint = ARGB.colorFromFloat(iconAlpha, 1.0f, 1.0f, 1.0f);
        renderIconSized(ctx, x + iconPad, y + iconPad, iconSize, slot.iconOwner(), slot.iconName(), iconTint);

        // 3. Cooldown vertical recharge sweep + thin bottom progress / ready / active strip
        if (coolingDown) {
            int veilH = Mth.clamp(Mth.ceil(tileSize * progress), 1, tileSize);
            ctx.fill(x, y, x + tileSize, y + veilH, 0x7708060F);

            int chargeW = Math.round(tileSize * (float) (1.0 - progress));
            if (chargeW > 0) {
                ctx.fill(x, y + tileSize - 1, x + chargeW, y + tileSize, 0xCCF59E0B);
            }
            ctx.outline(x, y, tileSize, tileSize, slot.activeNow() ? 0xFFFACC15 : 0x449CA3AF);
        } else {
            // Ready indicator: subtle glass rim + 1px emerald ready bar (gold glow when executing or pulsing!)
            int borderColor;
            int readyBarColor;
            if (slot.activeNow()) {
                borderColor = 0xFFFDE047;
                readyBarColor = 0xFFFACC15;
            } else if (pulse > 0.0f) {
                borderColor = ARGB.colorFromFloat(0.45f + pulse * 0.50f, 0.99f, 0.90f, 0.54f);
                readyBarColor = 0xEEFDE68A;
            } else {
                borderColor = 0x4DFFFFFF;
                readyBarColor = 0xBB34D399;
            }
            ctx.outline(x, y, tileSize, tileSize, borderColor);
            ctx.fill(x + 1, y + tileSize - 1, x + tileSize - 1, y + tileSize, readyBarColor);
        }

        // 4. Compact keybind badge at top-left (scaled cleanly so it never crowds the logo)
        final String keyLabel = cooldownTypeToKeybind(type, true);
        if (!keyLabel.isEmpty() && !"-".equals(keyLabel)) {
            final float keyScale = tileSize >= 20 ? 0.62f : 0.55f;
            final int badgeW = Mth.ceil(font.width(keyLabel) * keyScale) + 2;
            final int badgeH = Mth.ceil(7 * keyScale) + 2;
            ctx.fill(x + 1, y + 1, x + 1 + badgeW, y + 1 + badgeH, 0x9907050D);

            var pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(x + 2, y + 2);
            pose.scale(keyScale, keyScale);
            ctx.text(font, keyLabel, 0, 0, coolingDown ? 0xAA9CA3AF : 0xF0F9FAFB, false);
            pose.popMatrix();
        }

        // 4b. Crouch / Aerial variant indicator pip at top-right when an alternate move is unlocked
        if (tileSize >= 20 && !coolingDown && (slot.crouchVariantActive() || slot.aerialVariantActive())) {
            final String pip = slot.crouchVariantActive() ? "↓" : "↑";
            final int pipCol = slot.crouchVariantActive() ? 0xFF38BDF8 : 0xFFFBBF24;
            final float pipScale = 0.58f;
            final int pipW = Mth.ceil(font.width(pip) * pipScale) + 2;
            final int pipH = Mth.ceil(7 * pipScale) + 2;
            ctx.fill(x + tileSize - 1 - pipW, y + 1, x + tileSize - 1, y + 1 + pipH, 0xAA07050D);

            var pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(x + tileSize - pipW, y + 2);
            pose.scale(pipScale, pipScale);
            ctx.text(font, pip, 0, 0, pipCol, false);
            pose.popMatrix();
        }

        // 5. Crisp remaining-seconds readout centered when on cooldown
        if (coolingDown) {
            final float sec = remainingTicks / 20.0f;
            final String cdStr = sec >= 10.0f
                    ? String.format(Locale.ROOT, "%d", Mth.ceil(sec))
                    : String.format(Locale.ROOT, "%.1f", sec);
            final float cdScale = tileSize >= 20 ? 0.72f : 0.62f;
            final float textW = font.width(cdStr) * cdScale;
            final float textH = 7.0f * cdScale;

            var pose = ctx.pose();
            pose.pushMatrix();
            pose.translate(x + (tileSize - textW) * 0.5f, y + (tileSize - textH) * 0.5f + 2.0f);
            pose.scale(cdScale, cdScale);
            ctx.text(font, cdStr, 0, 0, 0xFFFDE68A, true);
            pose.popMatrix();
        }
    }

    public static String cooldownTypeToKeybind(CooldownType type, boolean makeShort) {
        return switch (type) {
            case LIGHT, STAND_LIGHT ->          "M1";
            case HEAVY, STAND_HEAVY ->          generateName(HEAVY_KEY.getParent(), makeShort);
            case BARRAGE, STAND_BARRAGE ->      generateName(BARRAGE_KEY.getParent(), makeShort);
            case ULTIMATE, STAND_ULTIMATE ->    generateName(ULT_KEY.getParent(), makeShort);
            case SPECIAL1, STAND_SP1 ->         generateName(SPECIAL1_KEY.getParent(), makeShort);
            case SPECIAL2, STAND_SP2 ->         generateName(SPECIAL2_KEY.getParent(), makeShort);
            case SPECIAL3, STAND_SP3 ->         generateName(SPECIAL3_KEY.getParent(), makeShort);
            case UTILITY ->                     generateName(JCraftClient.UTILITY_KEY.getParent(), makeShort);
            case STAND_TOSS ->                  generateName(Minecraft.getInstance().options.keyPickItem, makeShort);
            case COMBO_BREAKER ->               makeShort ? "CB" : "Combo Breaker";
            case COOLDOWN_CANCEL ->             generateName(JCraftClient.COOLDOWN_CANCEL_KEY.getParent(), makeShort);
            case DASH ->                        generateName(DASH_KEY.getParent(), makeShort);
        };
    }

    public static void renderIcon(final GuiGraphicsExtractor ctx, final int x, final int y, final Identifier type, final String icon) {
        renderIcon(ctx, x, y, type, icon, -1);
    }

    public static void renderIcon(final GuiGraphicsExtractor ctx, final int x, final int y, final Identifier type, final String icon, final int tint) {
        renderIconSized(ctx, x + 2, y + 2, 18, type, icon, tint);
    }

    private static void renderIconSized(final GuiGraphicsExtractor ctx, final int x, final int y, final int size,
                                        final Identifier type, final String icon, final int tint) {
        Identifier texture = type.withPath(p -> "textures/gui/ability_icons/" + p + "/" + icon + ".png");
        if (!isTextureAvailable(texture)) {
            texture = JCraft.id("textures/gui/ability_icons/fallback/" + icon + ".png");
        }
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, size, size, size, size, tint);
    }

    public static void renderAbsIcon(final GuiGraphicsExtractor ctx, final int x, final int y, Identifier texture, final String fallback) {
        renderAbsIcon(ctx, x, y, texture, fallback, -1);
    }

    public static void renderAbsIcon(final GuiGraphicsExtractor ctx, final int x, final int y, Identifier texture, final String fallback, final int tint) {
        if (!isTextureAvailable(texture)) {
            texture = JCraft.id("textures/gui/ability_icons/fallback/" + fallback + ".png");
        }
        ctx.blit(RenderPipelines.GUI_TEXTURED, texture, x + 2, y + 2, 0f, 0f, 18, 18, 18, 18, tint);
    }

    public static void renderBorder(final GuiGraphicsExtractor ctx, final int x, final int y) {
        renderBorder(ctx, x, y, -1);
    }

    public static void renderBorder(final GuiGraphicsExtractor ctx, final int x, final int y, final int tint) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, BORDER, x, y, 0f, 0f, 22, 22, 22, 22, tint);
    }

    private static boolean isTextureAvailable(final Identifier textureLocation) {
        return TEXTURE_EXISTS_CACHE.computeIfAbsent(textureLocation, loc -> {
            final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
            try {
                return resourceManager.getResource(loc).isPresent();
            } catch (Exception ex) {
                return false;
            }
        });
    }

    public static void renderCooldown(final GuiGraphicsExtractor ctx, final double cd, final int x, final int y) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y + Mth.floor(22.0 * (1.0 - cd)), 0f, (float) Math.floor((1.0 - cd) * 22),
                22, Mth.ceil(22.0 * cd), 22, 22);
    }
}
