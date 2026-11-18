package net.arna.jcraft.client.gui.hud;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import dev.architectury.event.events.client.ClientGuiEvent;
import lombok.experimental.UtilityClass;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.common.entity.stand.AerosmithEntity;
import net.arna.jcraft.common.entity.stand.MandomEntity;
import net.arna.jcraft.common.util.ColorUtils;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.function.Function;

import static net.arna.jcraft.client.JCraftClient.*;

@UtilityClass
public class JCraftAbilityHud {
    /// ICON HUD
    public static final Identifier GUI_ICONS_TEXTURE = Identifier.parse("textures/gui/icons.png");
    private static final Identifier BORDER = JCraft.id("textures/gui/ability_icons/border.png");
    private static final Identifier OVERLAY = JCraft.id("textures/gui/ability_icons/overlay.png");

    final IconPos ICON = new IconPos("icon", 10, 18 * 3 + 18);

    private static final int iconSpacing = 8;

    // Stand icons
    private static final IconPos LIGHT = new IconPos("light", 0, iconSpacing * 2); // 2 + 3 * N
    private static final IconPos HEAVY = new IconPos("heavy", 0, iconSpacing * 5);
    private static final IconPos BARRAGE = new IconPos("barrage", 0, iconSpacing * 8);
    private static final IconPos ULT = new IconPos("ult", 0, iconSpacing * 23);
    private static final IconPos SPECIAL_1 = new IconPos("special1", 0, iconSpacing * 14);
    private static final IconPos SPECIAL_2 = new IconPos("special2", 0, iconSpacing * 17);
    private static final IconPos SPECIAL_3 = new IconPos("special3", 0, iconSpacing * 20);
    private static final IconPos UTILITY = new IconPos("utility", 0, iconSpacing * 11);

    private static final IconPos MID_SPECIAL_1 = new IconPos("special1", 24, iconSpacing * 11);
    private static final IconPos MID_SPECIAL_2 = new IconPos("special2", 24, iconSpacing * 14);
    private static final IconPos MID_SPECIAL_3 = new IconPos("special3", 24, iconSpacing * 17);
    private static final IconPos MID_ULT = new IconPos("ult", 24, iconSpacing * 20);

    // Universal icons
    private static final IconPos COMBO_BREAKER = new IconPos("combobreaker", 24, iconSpacing * 2);
    private static final IconPos COOLDOWN_CANCEL = new IconPos("cooldowncancel", 24, iconSpacing * 5);
    private static final IconPos DASH = new IconPos("dash", 24, iconSpacing * 8);

    // Mandom icons
    private static final IconPos MANDOM_UTILITY = new IconPos("utility", 24, iconSpacing * 11);
    private static final IconPos MANDOM_ULT = new IconPos("ult", 24, iconSpacing * 14);

    // Spec-only icons
    private static final IconPos SPEC_HEAVY = new IconPos("heavy", 0, iconSpacing * 5);
    private static final IconPos SPEC_BARRAGE = new IconPos("barrage", 0, iconSpacing * 8);
    private static final IconPos SPEC_SPECIAL_1 = new IconPos("special1", 0, iconSpacing * 11);
    private static final IconPos SPEC_SPECIAL_2 = new IconPos("special2", 0, iconSpacing * 14);
    private static final IconPos SPEC_SPECIAL_3 = new IconPos("special3", 0, iconSpacing * 17);
    private static final IconPos SPEC_ULT = new IconPos("ult", 0, iconSpacing * 20);

    private static final Map<CooldownType, IconPos> STAND_ICONS = ImmutableMap.<CooldownType, IconPos>builder()
            .put(CooldownType.STAND_LIGHT, LIGHT)
            .put(CooldownType.STAND_HEAVY, HEAVY)
            .put(CooldownType.STAND_BARRAGE, BARRAGE)
            .put(CooldownType.STAND_ULTIMATE, ULT)
            .put(CooldownType.STAND_SP1, SPECIAL_1)
            .put(CooldownType.STAND_SP2, SPECIAL_2)
            .put(CooldownType.STAND_SP3, SPECIAL_3)
            .put(CooldownType.UTILITY, UTILITY)
            .build();
    // Used for JConfig.UIPos.MIDDLE, to prevent overwhelming verticallity
    private static final Map<CooldownType, IconPos> STAND_ICONS_MID = ImmutableMap.<CooldownType, IconPos>builder()
            .put(CooldownType.STAND_LIGHT, LIGHT)
            .put(CooldownType.STAND_HEAVY, HEAVY)
            .put(CooldownType.STAND_BARRAGE, BARRAGE)
            .put(CooldownType.STAND_ULTIMATE, MID_ULT)
            .put(CooldownType.STAND_SP1, MID_SPECIAL_1)
            .put(CooldownType.STAND_SP2, MID_SPECIAL_2)
            .put(CooldownType.STAND_SP3, MID_SPECIAL_3)
            .put(CooldownType.UTILITY, UTILITY)
            .build();
    private static final Map<CooldownType, IconPos> UNIVERSAL_ICONS = ImmutableMap.<CooldownType, IconPos>builder()
            .put(CooldownType.COMBO_BREAKER, COMBO_BREAKER)
            .put(CooldownType.COOLDOWN_CANCEL, COOLDOWN_CANCEL)
            .put(CooldownType.DASH, DASH)
            .build();
    private static final Map<CooldownType, IconPos> SPEC_ICONS = ImmutableMap.<CooldownType, IconPos>builder()
            .put(CooldownType.HEAVY, SPEC_HEAVY)
            .put(CooldownType.BARRAGE, SPEC_BARRAGE)
            //.put(CooldownType.ULTIMATE, SPEC_ULT)
            .put(CooldownType.SPECIAL1, SPEC_SPECIAL_1)
            .put(CooldownType.SPECIAL2, SPEC_SPECIAL_2)
            .put(CooldownType.SPECIAL3, SPEC_SPECIAL_3)
            .build();
    private static final Map<CooldownType, IconPos> MANDOM_WITH_SPEC_ICONS = ImmutableMap.<CooldownType, IconPos>builder()
            .put(CooldownType.HEAVY, SPEC_HEAVY)
            .put(CooldownType.BARRAGE, SPEC_BARRAGE)
            .put(CooldownType.SPECIAL1, SPEC_SPECIAL_1)
            .put(CooldownType.SPECIAL2, SPEC_SPECIAL_2)
            .put(CooldownType.SPECIAL3, SPEC_SPECIAL_3)
            .put(CooldownType.STAND_ULTIMATE, MANDOM_ULT)
            .put(CooldownType.UTILITY, MANDOM_UTILITY)
            .build();

    private static final Identifier UNIVERSAL_ID = JCraft.id("universal");

    public static int getHudX(final int scaledX, final int rightOffset) {
        return switch (JClientConfig.getInstance().getUiPosition()) {
            case LEFT -> 2;
            case RIGHT -> scaledX - rightOffset;
            case MIDDLE -> (int) (scaledX * 0.55f);
        };
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register((ctx, tickDelta) -> render(ctx));
    }

    public static void render(final GuiGraphicsExtractor ctx) {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        timeSinceNoCooldowns++;

        final boolean isMid = JClientConfig.getInstance().getUiPosition() == JClientConfig.UIPos.MIDDLE;
        final boolean useIcons = JClientConfig.getInstance().isIconHud();
        final StandEntity<?, ?> stand = JUtils.getStand(player);

        if (useIcons) {
            int selectedX = getHudX(client.getWindow().getGuiScaledWidth(), 48);
            int selectedY = isMid ? iconSpacing * 11 : 0;

            final JSpec<?, ?> spec = JUtils.getSpec(player);
            if (stand == null || (stand instanceof AerosmithEntity as && !as.isAllowingMoveHandling())
            ) {
                // Render cooldown HUD for specs
                if (spec != null) {
                    renderIcons(ctx, SPEC_ICONS, selectedX, selectedY, cooldown -> spec.getType().getId());
                }
            } else if (stand instanceof MandomEntity && spec != null) {
                renderIcons(ctx, MANDOM_WITH_SPEC_ICONS, selectedX, selectedY, cooldown -> {
                    if (cooldown == CooldownType.UTILITY || cooldown == CooldownType.STAND_ULTIMATE) {
                        return stand.getStandType().getId();
                    }
                    return spec.getType().getId();
                });
            } else {
                // Render cooldown HUD for stands
                renderIcons(ctx, isMid ? STAND_ICONS_MID : STAND_ICONS, selectedX, selectedY, cooldown -> stand.getStandType().getId());
            }
            renderIcons(ctx, UNIVERSAL_ICONS, selectedX, selectedY, cooldown -> UNIVERSAL_ID);
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
            case UTILITY, STAND_TOSS ->         generateName(JCraftClient.UTILITY_KEY.getParent(), makeShort);
            case COMBO_BREAKER ->               makeShort ? "CB" : "Combo Breaker";
            case COOLDOWN_CANCEL ->             generateName(JCraftClient.COOLDOWN_CANCEL_KEY.getParent(), makeShort);
            case DASH ->                        generateName(DASH_KEY.getParent(), makeShort);
        };
    }

    static int timeSinceNoCooldowns = 100;
    /**
     * Renders specified list of icons.
     *
     * @param icons     list of icons to render
     * @param selectedX x offset (in pixels) accounting for player's config choice
     * @param selectedY y offset (in pixels) accounting for player's config choice
     * @param type      decides which resource folder is loaded when rendering icons
     */
    private static void renderIcons(final GuiGraphicsExtractor ctx, final Map<CooldownType, IconPos> icons, final int selectedX,
                                    final int selectedY, final Function<CooldownType, Identifier> type) {
        final JClientConfig.UIDuration uiDuration = JClientConfig.getInstance().getUiDuration();
        if (uiDuration == JClientConfig.UIDuration.NEVER) {
            return;
        }

        final Font textRenderer = Minecraft.getInstance().font;

        for (Map.Entry<CooldownType, IconPos> entry : icons.entrySet()) {
            final CooldownType cooldownType = entry.getKey();
            final IconPos iconPos = entry.getValue();
            final int iconX = iconPos.x() + selectedX;
            final int iconY = iconPos.y() + selectedY;

            final double cd = getCooldownProgress(cooldownType);
            final boolean coolingDown = cd > 0.0d;

            float alpha = JClientConfig.getInstance().isIconHudPeekAllMoves() ? 0.1f : 0.0f;

            if (coolingDown) {
                timeSinceNoCooldowns = 0;
                alpha = 1.0f;
            }

            if (timeSinceNoCooldowns >= 100 && uiDuration == JClientConfig.UIDuration.COOLDOWN) {
                continue;
            }

            if (alpha <= 0.0f) continue;

            int tint = ARGB.colorFromFloat(alpha, 1f, 1f, 1f);
            renderBorder(ctx, iconX, iconY, tint);
            renderIcon(ctx, iconX, iconY, type.apply(cooldownType), iconPos.name(), tint);
            if (coolingDown) renderCooldown(ctx, cd, iconX, iconY);

            final String finalText = cooldownTypeToKeybind(cooldownType, true);

            ctx.text(
                    textRenderer,
                    finalText,
                    iconX,
                    iconY,
                    ColorUtils.HSBAtoRGBA(0.3f - (float) cd * 10f / 720f, (cd < 1.6) ? 0.0f : 1.0f, 1.0f, alpha * 2.0f)
            );
        }
    }

    public static void renderIcon(final GuiGraphicsExtractor ctx, final int x, final int y, final Identifier type, final String icon) {
        renderIcon(ctx, x, y, type, icon, -1);
    }

    public static void renderIcon(final GuiGraphicsExtractor ctx, final int x, final int y, final Identifier type, final String icon, final int tint) {
        final Identifier texture = type.withPath(p -> "textures/gui/ability_icons/" + p + "/" + icon + ".png");
        renderAbsIcon(ctx, x, y, texture, icon, tint);
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
        final ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        try {
            return resourceManager.getResource(textureLocation).isPresent();
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * @param type The type of the cooldown
     * @return Progress value of cooldown between zero and one if it is present, otherwise defaults to -1
     */
    private static double getCooldownProgress(final CooldownType type) {
        final CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(Minecraft.getInstance().player);
        final int cooldown = cooldowns.getCooldown(type);
        final int initialDuration = cooldowns.getInitialDuration(type);

        return cooldown > 0 && initialDuration != 0 ? normalize(cooldown, 0, initialDuration) : -1;
    }

    public static void renderCooldown(final GuiGraphicsExtractor ctx, final double cd, final int x, final int y) {
        ctx.blit(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y + Mth.floor(22.0 * (1.0 - cd)), 0f, (float) Math.floor((1.0 - cd) * 22),
                22, Mth.ceil(22.0 * cd), 22, 22);
    }

    private static double normalize(final double value, final double min, final double max) {
        return ((value - min) / (max - min));
    }

    private record IconPos(String name, int x, int y) {
        // intentionally left empty
    }
}
