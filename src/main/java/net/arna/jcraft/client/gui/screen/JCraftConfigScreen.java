package net.arna.jcraft.client.gui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.AutoConfigClient;
import net.arna.jcraft.api.serverconfig.BooleanOption;
import net.arna.jcraft.api.serverconfig.ConfigOption;
import net.arna.jcraft.api.serverconfig.FloatOption;
import net.arna.jcraft.api.serverconfig.IntOption;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.JCraftClient;
import net.arna.jcraft.client.gui.ServerConfigUI;
import net.arna.jcraft.client.input.TrackedKeyBinding;
import net.arna.jcraft.common.config.JServerConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * Full 6-tab JCraft Configuration Screen (Time & Balance, World, Gameplay, Gravity, Client & HUD, Keybinds),
 * accessible from the Y Radial Wheel, ModMenu, and /jconfig bridge.
 */
public final class JCraftConfigScreen extends Screen {
    private static final int ROW_H = 18;
    private static final int CTRL_W = 122;
    private static final int VALUE_W = 78;

    private static final String[] TAB_KEYS = {
            "jcraft.config.tab.balance",
            "jcraft.config.tab.world",
            "jcraft.config.tab.game",
            "jcraft.config.tab.gravity",
            "jcraft.config.tab.hud",
            "jcraft.config.tab.keys"
    };

    @Nullable
    private final Screen parent;
    private final Set<ConfigOption> dirtyServerOptions = new LinkedHashSet<>();
    private final List<List<ConfigRow>> tabs = new ArrayList<>(6);

    private int activeTab = 0;
    private int scrollOffset = 0;
    private int rowsPerPage = 8;
    @Nullable
    private KeyMapping listeningKey = null;

    private int panelLeft;
    private int panelTop;
    private int panelW;
    private int panelH;
    private int bodyTop;
    private int bodyBottom;

    private record ButtonSpec(String label, int width, Runnable action, boolean active) {}

    private record ConfigRow(
            boolean serverSide,
            Component label,
            @Nullable Component tooltip,
            Supplier<String> valueDisplay,
            Supplier<Integer> valueColor,
            Supplier<List<ButtonSpec>> controls
    ) {}

    public JCraftConfigScreen(@Nullable Screen parent) {
        super(Component.translatable("text.autoconfig.jcraft.title"));
        this.parent = parent;
        JServerConfig.init();
        buildTabs();
    }

    private void buildTabs() {
        tabs.clear();
        tabs.add(buildTimeAndBalanceTab());
        tabs.add(buildInteractionTab());
        tabs.add(buildGameplayTab());
        tabs.add(buildGravityTab());
        tabs.add(buildClientAndHudTab());
        tabs.add(buildKeybindsTab());
    }

    private List<ConfigRow> buildTimeAndBalanceTab() {
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(new ConfigRow(
                true,
                Component.translatable("jcraft.config.presets"),
                Component.translatable("jcraft.config.presets.tooltip"),
                this::detectActivePresetLabel,
                () -> 0xFFFDE047,
                () -> {
                    boolean canEdit = canEditServerConfig();
                    return List.of(
                            new ButtonSpec(Component.translatable("jcraft.config.preset.coop").getString(), 39, this::applyCoopPreset, canEdit),
                            new ButtonSpec(Component.translatable("jcraft.config.preset.pvp").getString(), 37, this::applyPvpPreset, canEdit),
                            new ButtonSpec(Component.translatable("jcraft.config.preset.chaos").getString(), 42, this::applyChaosPreset, canEdit)
                    );
                }
        ));

        // Time Stop, Time Erasure & Time Acceleration durations (ticks + seconds)
        rows.add(serverTicks(JServerConfig.TW_TIME_STOP_DURATION, 5, 20, 1200));
        rows.add(serverTicks(JServerConfig.SPTW_TIME_STOP_DURATION, 5, 20, 1200));
        rows.add(serverTicks(JServerConfig.STW_TIME_STOP_DURATION, 5, 20, 1200));
        rows.add(serverTicks(JServerConfig.TWOH_TIME_STOP_DURATION, 5, 20, 1200));
        rows.add(serverTicks(JServerConfig.KC_TIME_ERASURE_DURATION, 5, 20, 1200));
        rows.add(serverTicks(JServerConfig.MIH_TIME_ACCELERATION_DURATION, 20, 100, 3600));
        rows.add(serverTicks(JServerConfig.CMOON_UTIL_DURATION, 20, 100, 3600));
        rows.add(serverInt(JServerConfig.CMOON_ULT_RANGE, 5, 20, 0, 256));

        // Cooldowns & Combat Balance
        rows.add(serverBool(JServerConfig.ENABLE_MOVE_COOLDOWNS));
        rows.add(serverFloat(JServerConfig.COOLDOWN_MULTIPLIER, 0.1f, 0.5f, 0.0f, 10.0f, "x"));
        rows.add(serverBool(JServerConfig.SURVIVAL_CDC));
        rows.add(serverFloat(JServerConfig.STAND_DAMAGE_MULTIPLIER, 0.1f, 0.5f, 0.0f, 10.0f, "x"));
        rows.add(serverFloat(JServerConfig.VS_STANDLESS_DAMAGE_MULTIPLIER, 0.1f, 0.5f, 0.0f, 10.0f, "x"));
        rows.add(serverBool(JServerConfig.IGNORE_ARMOR));
        rows.add(serverBool(JServerConfig.HEALTH_TO_DAMAGE_SCALING));
        rows.add(serverFloat(JServerConfig.DAMAGE_SCALING_MINIMUM, 0.05f, 0.2f, 0.0f, 1.0f, "x"));
        rows.add(serverFloat(JServerConfig.SCALING_PENALTY_PER_HIT, 0.01f, 0.05f, 0.0f, 1.0f, ""));
        rows.add(serverBool(JServerConfig.ENABLE_IPS));
        rows.add(serverBool(JServerConfig.KILL_VAMPIRISM));
        rows.add(serverBool(JServerConfig.ENABLE_FRIENDLY_FIRE));
        rows.add(serverInt(JServerConfig.BASE_AI_LEVEL, 1, 5, 0, 20));
        rows.add(serverFloat(JServerConfig.MIH_SPRINT_HUNGER_MULTIPLIER, 0.1f, 0.5f, 0.0f, 10.0f, "x"));
        return rows;
    }

    private List<ConfigRow> buildInteractionTab() {
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(serverBool(JServerConfig.MINING_BARRAGE));
        rows.add(serverFloat(JServerConfig.BLOCK_BREAKAGE_MULTIPLIER, 0.1f, 0.5f, 0.0f, 10.0f, "x"));
        rows.add(serverFloat(JServerConfig.METEOR_SPAWN_RATE, 0.01f, 0.05f, 0.0f, 1.0f, ""));
        rows.add(serverFloat(JServerConfig.STAND_ARROW_SPAWN_RATE, 0.01f, 0.05f, 0.0f, 1.0f, ""));
        rows.add(serverInt(JServerConfig.DUMMY_DAMAGE_INDICATOR_RANGE, 8, 32, 0, 512));
        rows.add(serverBool(JServerConfig.CREAM_ITEM_ERASE));
        rows.add(serverBool(JServerConfig.ROLLER_FLATTENING));
        rows.add(serverBool(JServerConfig.ROLLER_DESTROYING));
        rows.add(serverBool(JServerConfig.PLAYER_VAMPS_DIE_TO_HAMON));
        rows.add(serverBool(JServerConfig.MANDOM_AFFECTS_BLOCKS));
        rows.add(serverBool(JServerConfig.WS_STEAL_STANDS_FROM_PLAYERS));
        rows.add(serverBool(JServerConfig.UNIVERSAL_ABILITIES));
        rows.add(serverBool(JServerConfig.SPTW_IGNITE_CAMPFIRES));
        rows.add(serverInt(JServerConfig.SHA_SEARCH_RADIUS, 1, 5, 3, 32));
        rows.add(serverBool(JServerConfig.MIH_ACCELERATE_TICKS));
        rows.add(serverBool(JServerConfig.USE_FOOLISH_SAND));
        rows.add(serverBool(JServerConfig.REDUCE_DEADLY_EXPLOSIONS));
        rows.add(serverBool(JServerConfig.DISABLE_COMBAT_ELYTRA));
        return rows;
    }

    private List<ConfigRow> buildGameplayTab() {
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(serverBool(JServerConfig.ENABLE_HITSTOP));
        rows.add(serverBool(JServerConfig.EXCLUSIVE_STANDS));
        rows.add(serverBool(JServerConfig.START_WITH_STAND));
        rows.add(serverBool(JServerConfig.START_WITH_SPEC));
        rows.add(serverBool(JServerConfig.STAND_USER_SIGHT));
        rows.add(serverBool(JServerConfig.CREAM_VOID_VISIBLE));
        rows.add(serverBool(JServerConfig.SPAWNER_STANDS));
        rows.add(serverBool(JServerConfig.ORIGIN_FREE_BINDS));
        rows.add(serverInt(JServerConfig.ORIGIN_MAX_ULTIMATES, 1, 2, 0, 9));
        return rows;
    }

    private List<ConfigRow> buildGravityTab() {
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(serverBool(JServerConfig.GRAVITY_ONLY_AFFECTS_PLAYERS));
        rows.add(serverBool(JServerConfig.GRAVITY_KEEP_WORLD_LOOK));
        rows.add(serverMs(JServerConfig.GRAVITY_ROTATION_TIME, 50, 250, 5000));
        rows.add(serverBool(JServerConfig.GRAVITY_WORLD_VELOCITY));
        rows.add(serverBool(JServerConfig.RESET_GRAVITY_ON_DIMENSION_CHANGE));
        rows.add(serverBool(JServerConfig.RESET_GRAVITY_ON_RESPAWN));
        rows.add(serverBool(JServerConfig.VOID_DAMAGE_ABOVE_WORLD));
        return rows;
    }

    private List<ConfigRow> buildClientAndHudTab() {
        JClientConfig cfg = JClientConfig.getInstance();
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(clientBool("jcraft.config.anime_hud", "Modern JoJo Anime HUD", cfg::isAnimeHudEnabled, cfg::setAnimeHudEnabled, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.iconHud", "Icon HUD", cfg::isIconHud, cfg::setIconHud, true));
        rows.add(clientFloat("jcraft.config.hud_scale", "HUD Scale", cfg::getHudScale, cfg::setHudScale, 0.05f, 0.25f, 0.75f, 1.50f, 1.0f, "x"));
        rows.add(clientBool("text.autoconfig.jcraft.option.iconHudPeekAllMoves", "Peek Moves in Icon HUD", cfg::isIconHudPeekAllMoves, cfg::setIconHudPeekAllMoves, true));
        rows.add(clientEnum("text.autoconfig.jcraft.option.uiPosition", "UI Position", cfg::getUiPosition, cfg::setUiPosition, JClientConfig.UIPos.values(), JClientConfig.UIPos.RIGHT));
        rows.add(clientEnum("text.autoconfig.jcraft.option.uiDuration", "UI Duration", cfg::getUiDuration, cfg::setUiDuration, JClientConfig.UIDuration.values(), JClientConfig.UIDuration.COOLDOWN));
        rows.add(clientInt("text.autoconfig.jcraft.option.horizontalHudOffset", "Horizontal HUD Offset", cfg::getHorizontalHudOffset, cfg::setHorizontalHudOffset, 5, 25, -400, 400, 0));
        rows.add(clientInt("text.autoconfig.jcraft.option.verticalHudOffset", "Vertical HUD Offset", cfg::getVerticalHudOffset, cfg::setVerticalHudOffset, 5, 25, -400, 400, 0));
        rows.add(clientBool("jcraft.config.onomatopoeia", "Manga Onomatopoeia", cfg::isOnomatopoeiaEnabled, cfg::setOnomatopoeiaEnabled, true));
        rows.add(clientBool("jcraft.config.input_buffer", "Smart 6-Tick Input Buffer", cfg::isInputBufferEnabled, cfg::setInputBufferEnabled, true));
        rows.add(clientBool("jcraft.config.color_shift", "Araki Color Shift Shader", cfg::isColorShiftEnabled, cfg::setColorShiftEnabled, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.standAuras", "Stand Auras", cfg::isStandAuras, cfg::setStandAuras, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.timeEraseShader", "Time Erase Shader", cfg::isTimeEraseShader, cfg::setTimeEraseShader, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.epitaphOverlay", "Epitaph Overlay", cfg::isEpitaphOverlay, cfg::setEpitaphOverlay, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.comboCounter", "Combo Counter", cfg::isComboCounter, cfg::setComboCounter, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.dynamicFirstPersonStandOpacity", "Dynamic 1st-Person Opacity", cfg::isDynamicFirstPersonStandOpacity, cfg::setDynamicFirstPersonStandOpacity, true));
        rows.add(clientFloat("text.autoconfig.jcraft.option.firstPersonStandOpacityMult", "1st-Person Opacity Mult", cfg::getFirstPersonStandOpacityMult, cfg::setFirstPersonStandOpacityMult, 0.05f, 0.25f, 0.0f, 1.0f, 1.0f, "x"));
        rows.add(clientBool("text.autoconfig.jcraft.option.showStandUserWarningPlayer", "Stand Warning (Players)", cfg::isShowStandUserWarningPlayer, cfg::setShowStandUserWarningPlayer, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.showStandUserWarningMob", "Stand Warning (Mobs)", cfg::isShowStandUserWarningMob, cfg::setShowStandUserWarningMob, true));
        rows.add(clientBool("text.autoconfig.jcraft.option.disableMihAfterimages", "Disable MiH Afterimages", cfg::isDisableMihAfterimages, cfg::setDisableMihAfterimages, false));
        rows.add(clientBool("jcraft.config.aura_afterimages", "Aura Afterimage Trails", cfg::isAuraAfterimagesEnabled, cfg::setAuraAfterimagesEnabled, true));
        rows.add(clientBool("jcraft.config.impact_effects", "Impact Frames & Shockwave", cfg::isImpactEffectsEnabled, cfg::setImpactEffectsEnabled, true));
        rows.add(clientFloat("jcraft.config.screen_shake_strength", "Screen Shake Strength", cfg::getScreenShakeStrength, cfg::setScreenShakeStrength, 0.1f, 0.25f, 0.0f, 2.0f, 1.0f, "x"));
        rows.add(clientBool("jcraft.config.runic_charge_circles", "Runic Charge Circles", cfg::isRunicChargeCirclesEnabled, cfg::setRunicChargeCirclesEnabled, true));
        return rows;
    }

    private List<ConfigRow> buildKeybindsTab() {
        List<ConfigRow> rows = new ArrayList<>();
        rows.add(new ConfigRow(
                false,
                Component.translatable("jcraft.config.keys.reset_all"),
                Component.translatable("jcraft.config.keys.reset_all.tooltip"),
                () -> countKeybindConflicts() > 0
                        ? Component.translatable("jcraft.config.keys.conflicts", countKeybindConflicts()).getString()
                        : Component.translatable("jcraft.config.keys.all_clean").getString(),
                () -> countKeybindConflicts() > 0 ? 0xFFF87171 : 0xFF34D399,
                () -> List.of(new ButtonSpec(Component.translatable("jcraft.config.keys.reset_btn").getString(), CTRL_W, () -> {
                    listeningKey = null;
                    JCraftClient.resetKeyBindingsToDefaults(Minecraft.getInstance());
                }, true))
        ));

        for (TrackedKeyBinding tracked : JCraftClient.getJCraftKeyBindings()) {
            KeyMapping kb = tracked.getParent();
            rows.add(new ConfigRow(
                    false,
                    Component.translatable(kb.getName()),
                    Component.translatable("jcraft.config.keys.row_tooltip"),
                    () -> {
                        if (hasJCraftConflict(kb)) return Component.translatable("jcraft.config.keys.dup").getString();
                        String vanillaConflict = findVanillaConflictName(kb);
                        if (vanillaConflict != null) return "⚠ " + vanillaConflict;
                        return kb.isDefault()
                                ? Component.translatable("jcraft.config.keys.default").getString()
                                : Component.translatable("jcraft.config.keys.custom").getString();
                    },
                    () -> {
                        if (hasJCraftConflict(kb)) return 0xFFF87171;
                        if (hasVanillaConflict(kb)) return 0xFFFBBF24;
                        return kb.isDefault() ? 0xFF34D399 : 0xFFFDE68A;
                    },
                    () -> {
                        boolean listening = (listeningKey == kb);
                        String keyName = kb.getTranslatedKeyMessage().getString();
                        String btnLabel;
                        if (listening) {
                            btnLabel = "§e> " + keyName + " <";
                        } else if (hasJCraftConflict(kb) || hasVanillaConflict(kb)) {
                            btnLabel = "§c⚠ " + keyName;
                        } else {
                            btnLabel = keyName;
                        }
                        return List.of(
                                new ButtonSpec(btnLabel, 96, () -> listeningKey = kb, true),
                                new ButtonSpec("↺", 20, () -> {
                                    listeningKey = null;
                                    kb.setKey(kb.getDefaultKey());
                                    KeyMapping.resetMapping();
                                    saveKeyMappings();
                                }, !kb.isDefault())
                        );
                    }
            ));
        }
        return rows;
    }

    private static int countKeybindConflicts() {
        int count = 0;
        for (TrackedKeyBinding tracked : JCraftClient.getJCraftKeyBindings()) {
            KeyMapping kb = tracked.getParent();
            if (hasJCraftConflict(kb) || hasVanillaConflict(kb)) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasJCraftConflict(KeyMapping target) {
        if (target.isUnbound()) return false;
        for (TrackedKeyBinding other : JCraftClient.getJCraftKeyBindings()) {
            KeyMapping kb = other.getParent();
            if (kb != target && !kb.isUnbound() && kb.same(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * MC 26.3 registers F3+key debug shortcuts (showHitboxes=B, showChunkBorders=G, showAdvancedTooltips=H,
     * dumpVersion=V, improvedTransparency=X, focusPause=P, etc.) as KeyMappings in Category.DEBUG.
     * Those chords only trigger while holding F3 and never conflict with normal gameplay keybinds.
     */
    public static boolean isIgnoredVanillaKey(KeyMapping other, @Nullable net.minecraft.client.Options options) {
        if (other == null) return true;
        if (KeyMapping.Category.DEBUG.equals(other.getCategory())) {
            return true;
        }
        return options != null && other == options.keySpectatorHotbar;
    }

    private static boolean hasVanillaConflict(KeyMapping target) {
        return findVanillaConflictName(target) != null;
    }

    private static @Nullable String findVanillaConflictName(KeyMapping target) {
        Minecraft mc = Minecraft.getInstance();
        if (target.isUnbound() || mc.options == null) return null;
        Set<KeyMapping> jcraftSet = new HashSet<>();
        for (TrackedKeyBinding tracked : JCraftClient.getJCraftKeyBindings()) {
            jcraftSet.add(tracked.getParent());
        }
        for (KeyMapping other : mc.options.keyMappings) {
            if (!jcraftSet.contains(other)
                    && !other.isUnbound()
                    && !isIgnoredVanillaKey(other, mc.options)
                    && other.same(target)) {
                String name = Component.translatable(other.getName()).getString();
                return name.length() > 11 ? "Vanilla" : name;
            }
        }
        return null;
    }

    private static void saveKeyMappings() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            mc.options.save();
        }
    }

    private static Component serverLabel(ConfigOption opt) {
        Identifier key = opt.getKey();
        String langKey = String.format("%s.serverconfig.option.%s", key.getNamespace(), key.getPath());
        return Language.getInstance().has(langKey)
                ? Component.translatable(langKey)
                : Component.literal(key.getPath());
    }

    private static @Nullable Component serverTooltip(ConfigOption opt) {
        Identifier key = opt.getKey();
        String tooltipKey = String.format("%s.serverconfig.option.%s.tooltip", key.getNamespace(), key.getPath());
        return Language.getInstance().has(tooltipKey) ? Component.translatable(tooltipKey) : null;
    }

    private static Component clientLabel(String key, String fallback) {
        return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(fallback);
    }

    private static @Nullable Component clientTooltip(String key) {
        String tipKey = key + ".tooltip";
        if (Language.getInstance().has(tipKey)) {
            return Component.translatable(tipKey);
        }
        String clothTip = key + ".@Tooltip";
        if (Language.getInstance().has(clothTip)) {
            return Component.translatable(clothTip);
        }
        return null;
    }

    private ConfigRow serverTicks(IntOption opt, int smallStep, int bigStep, int maxCap) {
        int max = opt.getMax() != null ? opt.getMax() : maxCap;
        return new ConfigRow(
                true,
                serverLabel(opt),
                serverTooltip(opt),
                () -> String.format(Locale.ROOT, "%dt (%.2fs)", opt.getValue(), opt.getValue() / 20.0f),
                () -> 0xFFFDE68A,
                () -> intControls(opt::getValue, v -> setServerInt(opt, v), smallStep, bigStep,
                        opt.getMin() != null ? opt.getMin() : 0, max, opt.getDefaultValue(), canEditServerConfig())
        );
    }

    private ConfigRow serverMs(IntOption opt, int smallStep, int bigStep, int maxCap) {
        int max = opt.getMax() != null ? opt.getMax() : maxCap;
        return new ConfigRow(
                true,
                serverLabel(opt),
                serverTooltip(opt),
                () -> opt.getValue() + "ms",
                () -> 0xFFFDE68A,
                () -> intControls(opt::getValue, v -> setServerInt(opt, v), smallStep, bigStep,
                        opt.getMin() != null ? opt.getMin() : 0, max, opt.getDefaultValue(), canEditServerConfig())
        );
    }

    private ConfigRow serverInt(IntOption opt, int smallStep, int bigStep, int fallbackMin, int fallbackMax) {
        int min = opt.getMin() != null ? opt.getMin() : fallbackMin;
        int max = opt.getMax() != null ? opt.getMax() : fallbackMax;
        return new ConfigRow(
                true,
                serverLabel(opt),
                serverTooltip(opt),
                () -> Integer.toString(opt.getValue()),
                () -> 0xFF93C5FD,
                () -> intControls(opt::getValue, v -> setServerInt(opt, v), smallStep, bigStep,
                        min, max, opt.getDefaultValue(), canEditServerConfig())
        );
    }

    private ConfigRow serverFloat(FloatOption opt, float smallStep, float bigStep, float fallbackMin, float fallbackMax, String suffix) {
        float min = opt.getMin() != null ? opt.getMin() : fallbackMin;
        float max = opt.getMax() != null ? opt.getMax() : fallbackMax;
        return new ConfigRow(
                true,
                serverLabel(opt),
                serverTooltip(opt),
                () -> String.format(Locale.ROOT, "%.2f%s", opt.getValue(), suffix),
                () -> 0xFF93C5FD,
                () -> floatControls(opt::getValue, v -> setServerFloat(opt, v), smallStep, bigStep,
                        min, max, opt.getDefaultValue(), canEditServerConfig())
        );
    }

    private ConfigRow serverBool(BooleanOption opt) {
        return new ConfigRow(
                true,
                serverLabel(opt),
                serverTooltip(opt),
                () -> opt.getValue() ? "ON" : "OFF",
                () -> opt.getValue() ? 0xFF34D399 : 0xFFF87171,
                () -> boolControls(opt::getValue, v -> {
                    opt.setValue(v);
                    dirtyServerOptions.add(opt);
                    JServerConfig.syncGravityConfig();
                }, opt.getDefaultValue(), canEditServerConfig())
        );
    }

    private ConfigRow clientBool(String key, String fallback, BooleanSupplier get, Consumer<Boolean> set, boolean def) {
        return new ConfigRow(
                false,
                clientLabel(key, fallback),
                clientTooltip(key),
                () -> get.getAsBoolean() ? "ON" : "OFF",
                () -> get.getAsBoolean() ? 0xFF34D399 : 0xFFF87171,
                () -> boolControls(get, v -> {
                    set.accept(v);
                    saveClientConfig();
                }, def, true)
        );
    }

    private ConfigRow clientInt(String key, String fallback, IntSupplier get, IntConsumer set,
                                int smallStep, int bigStep, int min, int max, int def) {
        return new ConfigRow(
                false,
                clientLabel(key, fallback),
                clientTooltip(key),
                () -> Integer.toString(get.getAsInt()),
                () -> 0xFF93C5FD,
                () -> intControls(get, v -> {
                    set.accept(v);
                    saveClientConfig();
                }, smallStep, bigStep, min, max, def, true)
        );
    }

    private ConfigRow clientFloat(String key, String fallback, Supplier<Float> get, Consumer<Float> set,
                                  float smallStep, float bigStep, float min, float max, float def, String suffix) {
        return new ConfigRow(
                false,
                clientLabel(key, fallback),
                clientTooltip(key),
                () -> String.format(Locale.ROOT, "%.2f%s", get.get(), suffix),
                () -> 0xFF93C5FD,
                () -> floatControls(get, v -> {
                    set.accept(v);
                    saveClientConfig();
                }, smallStep, bigStep, min, max, def, true)
        );
    }

    private <E extends Enum<E>> ConfigRow clientEnum(String key, String fallback, Supplier<E> get, Consumer<E> set, E[] values, E def) {
        return new ConfigRow(
                false,
                clientLabel(key, fallback),
                clientTooltip(key),
                () -> get.get().name(),
                () -> 0xFF38BDF8,
                () -> List.of(
                        new ButtonSpec(get.get().name(), 96, () -> {
                            E cur = get.get();
                            set.accept(values[(cur.ordinal() + 1) % values.length]);
                            saveClientConfig();
                        }, true),
                        new ButtonSpec("↺", 20, () -> {
                            set.accept(def);
                            saveClientConfig();
                        }, true)
                )
        );
    }

    private List<ButtonSpec> intControls(IntSupplier get, IntConsumer set, int smallStep, int bigStep, int min, int max, int def, boolean enabled) {
        return List.of(
                new ButtonSpec("-" + bigStep, 26, () -> set.accept(Mth.clamp(get.getAsInt() - bigStep, min, max)), enabled),
                new ButtonSpec("-" + smallStep, 22, () -> set.accept(Mth.clamp(get.getAsInt() - smallStep, min, max)), enabled),
                new ButtonSpec("+" + smallStep, 22, () -> set.accept(Mth.clamp(get.getAsInt() + smallStep, min, max)), enabled),
                new ButtonSpec("+" + bigStep, 26, () -> set.accept(Mth.clamp(get.getAsInt() + bigStep, min, max)), enabled),
                new ButtonSpec("↺", 18, () -> set.accept(def), enabled)
        );
    }

    private List<ButtonSpec> floatControls(Supplier<Float> get, Consumer<Float> set, float smallStep, float bigStep, float min, float max, float def, boolean enabled) {
        return List.of(
                new ButtonSpec("--", 22, () -> set.accept(round2(Mth.clamp(get.get() - bigStep, min, max))), enabled),
                new ButtonSpec("-", 22, () -> set.accept(round2(Mth.clamp(get.get() - smallStep, min, max))), enabled),
                new ButtonSpec("+", 22, () -> set.accept(round2(Mth.clamp(get.get() + smallStep, min, max))), enabled),
                new ButtonSpec("++", 22, () -> set.accept(round2(Mth.clamp(get.get() + bigStep, min, max))), enabled),
                new ButtonSpec("↺", 18, () -> set.accept(def), enabled)
        );
    }

    private List<ButtonSpec> boolControls(BooleanSupplier get, Consumer<Boolean> set, boolean def, boolean enabled) {
        boolean on = get.getAsBoolean();
        return List.of(
                new ButtonSpec(on ? "§aON" : "§cOFF", 96, () -> set.accept(!on), enabled),
                new ButtonSpec("↺", 20, () -> set.accept(def), enabled)
        );
    }

    private void setServerInt(IntOption opt, int value) {
        opt.setValue(value);
        dirtyServerOptions.add(opt);
        JServerConfig.syncGravityConfig();
    }

    private void setServerFloat(FloatOption opt, float value) {
        opt.setValue(value);
        dirtyServerOptions.add(opt);
    }

    private void setServerBool(BooleanOption opt, boolean value) {
        opt.setValue(value);
        dirtyServerOptions.add(opt);
    }

    private void applyCoopPreset() {
        setServerBool(JServerConfig.ENABLE_IPS, true);
        setServerBool(JServerConfig.SURVIVAL_CDC, true);
        setServerBool(JServerConfig.ENABLE_FRIENDLY_FIRE, false);
        setServerFloat(JServerConfig.STAND_DAMAGE_MULTIPLIER, 1.0f);
        setServerFloat(JServerConfig.COOLDOWN_MULTIPLIER, 1.0f);
        setServerBool(JServerConfig.HEALTH_TO_DAMAGE_SCALING, true);
        setServerBool(JServerConfig.KILL_VAMPIRISM, false);
        setServerBool(JServerConfig.DISABLE_COMBAT_ELYTRA, true);
        setServerBool(JServerConfig.MINING_BARRAGE, false);
    }

    private void applyPvpPreset() {
        setServerBool(JServerConfig.ENABLE_IPS, true);
        setServerBool(JServerConfig.SURVIVAL_CDC, false);
        setServerBool(JServerConfig.ENABLE_FRIENDLY_FIRE, true);
        setServerFloat(JServerConfig.STAND_DAMAGE_MULTIPLIER, 1.0f);
        setServerFloat(JServerConfig.COOLDOWN_MULTIPLIER, 1.0f);
        setServerBool(JServerConfig.HEALTH_TO_DAMAGE_SCALING, true);
        setServerBool(JServerConfig.KILL_VAMPIRISM, true);
        setServerBool(JServerConfig.DISABLE_COMBAT_ELYTRA, true);
    }

    private void applyChaosPreset() {
        setServerBool(JServerConfig.ENABLE_IPS, false);
        setServerBool(JServerConfig.SURVIVAL_CDC, true);
        setServerBool(JServerConfig.ENABLE_FRIENDLY_FIRE, true);
        setServerFloat(JServerConfig.STAND_DAMAGE_MULTIPLIER, 1.5f);
        setServerFloat(JServerConfig.COOLDOWN_MULTIPLIER, 0.6f);
        setServerBool(JServerConfig.HEALTH_TO_DAMAGE_SCALING, false);
        setServerBool(JServerConfig.KILL_VAMPIRISM, true);
        setServerBool(JServerConfig.MINING_BARRAGE, true);
    }

    private String detectActivePresetLabel() {
        if (JServerConfig.ENABLE_IPS.getValue()
                && JServerConfig.SURVIVAL_CDC.getValue()
                && !JServerConfig.ENABLE_FRIENDLY_FIRE.getValue()
                && !JServerConfig.KILL_VAMPIRISM.getValue()
                && Math.abs(JServerConfig.COOLDOWN_MULTIPLIER.getValue() - 1.0f) < 0.01f) {
            return Component.translatable("jcraft.config.preset.coop").getString();
        }
        if (JServerConfig.ENABLE_IPS.getValue()
                && !JServerConfig.SURVIVAL_CDC.getValue()
                && JServerConfig.ENABLE_FRIENDLY_FIRE.getValue()
                && JServerConfig.KILL_VAMPIRISM.getValue()
                && Math.abs(JServerConfig.COOLDOWN_MULTIPLIER.getValue() - 1.0f) < 0.01f) {
            return Component.translatable("jcraft.config.preset.pvp").getString();
        }
        if (!JServerConfig.ENABLE_IPS.getValue()
                && JServerConfig.SURVIVAL_CDC.getValue()
                && JServerConfig.ENABLE_FRIENDLY_FIRE.getValue()
                && JServerConfig.STAND_DAMAGE_MULTIPLIER.getValue() >= 1.49f
                && JServerConfig.COOLDOWN_MULTIPLIER.getValue() <= 0.61f) {
            return Component.translatable("jcraft.config.preset.chaos").getString();
        }
        return Component.translatable("jcraft.config.keys.custom").getString();
    }

    private static float round2(float v) {
        return Math.round(v * 100.0f) / 100.0f;
    }

    private boolean canEditServerConfig() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.getSingleplayerServer() != null) {
            return true;
        }
        return mc.player != null && mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.panelW = Math.min(590, this.width - 10);
        this.panelH = Math.min(264, this.height - 10);
        this.panelLeft = (this.width - this.panelW) / 2;
        this.panelTop = (this.height - this.panelH) / 2;

        // 6 top category tabs
        int tabCount = TAB_KEYS.length;
        int tabGap = 4;
        int tabW = (this.panelW - 16 - tabGap * (tabCount - 1)) / tabCount;
        int tabY = this.panelTop + 18;
        for (int t = 0; t < tabCount; t++) {
            final int tabIdx = t;
            String title = Component.translatable(TAB_KEYS[t]).getString();
            if (tabIdx == activeTab) {
                title = "§e" + title;
            }
            this.addRenderableWidget(Button.builder(Component.literal(title), b -> switchTab(tabIdx))
                    .bounds(this.panelLeft + 8 + t * (tabW + tabGap), tabY, tabW, 16)
                    .build());
        }

        this.bodyTop = this.panelTop + 40;
        int footerTop = this.panelTop + this.panelH - 42;
        this.bodyBottom = footerTop - 4;
        this.rowsPerPage = Math.max(4, Math.min(8, (this.bodyBottom - this.bodyTop) / ROW_H));

        List<ConfigRow> activeRows = tabs.get(activeTab);
        int maxStart = Math.max(0, activeRows.size() - rowsPerPage);
        this.scrollOffset = Mth.clamp(this.scrollOffset, 0, maxStart);

        int endIdx = Math.min(activeRows.size(), scrollOffset + rowsPerPage);
        int ctrlRight = this.panelLeft + this.panelW - 10;

        for (int i = scrollOffset; i < endIdx; i++) {
            ConfigRow row = activeRows.get(i);
            int rowY = bodyTop + (i - scrollOffset) * ROW_H;
            List<ButtonSpec> specs = row.controls().get();
            int gap = 2;
            int totalSpecW = specs.stream().mapToInt(ButtonSpec::width).sum() + Math.max(0, specs.size() - 1) * gap;
            int bx = ctrlRight - totalSpecW;

            for (ButtonSpec spec : specs) {
                Button btn = Button.builder(Component.literal(spec.label()), b -> {
                    spec.action().run();
                    this.init();
                }).bounds(bx, rowY + 1, spec.width(), 16).build();
                btn.active = spec.active();
                this.addRenderableWidget(btn);
                bx += spec.width() + gap;
            }
        }

        // Bottom controls row 1: Prev Page, Next Page, Server Cloth UI, Client Cloth UI
        int totalPages = Math.max(1, (int) Math.ceil(activeRows.size() / (double) rowsPerPage));
        int currentPage = Math.min(totalPages, (scrollOffset / rowsPerPage) + 1);
        int footW = this.panelW - 16;
        int navW = Math.min(58, (footW - 16) / 6);
        int clothW = (footW - navW * 2 - 48) / 2;

        Button prevBtn = Button.builder(Component.translatable("jcraft.config.nav.prev"), b -> {
            scrollOffset = Math.max(0, scrollOffset - rowsPerPage);
            this.init();
        }).bounds(this.panelLeft + 8, footerTop, navW, 18).build();
        prevBtn.active = scrollOffset > 0;
        this.addRenderableWidget(prevBtn);

        Button nextBtn = Button.builder(Component.translatable("jcraft.config.nav.next"), b -> {
            scrollOffset = Math.min(maxStart, scrollOffset + rowsPerPage);
            this.init();
        }).bounds(this.panelLeft + 12 + navW, footerTop, navW, 18).build();
        nextBtn.active = scrollOffset < maxStart;
        this.addRenderableWidget(nextBtn);

        int clothStartX = this.panelLeft + this.panelW - 8 - clothW * 2 - 4;
        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.config.cloth.server"), b -> {
            Minecraft.getInstance().gui.setScreen(ServerConfigUI.buildScreen(this, canEditServerConfig()));
        }).bounds(clothStartX, footerTop, clothW, 18).build());

        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.config.cloth.client"), b -> {
            try {
                Screen cloth = AutoConfigClient.getConfigScreen(JClientConfig.class, this).get();
                Minecraft.getInstance().gui.setScreen(cloth);
            } catch (Throwable ignored) {}
        }).bounds(clothStartX + clothW + 4, footerTop, clothW, 18).build());

        // Bottom controls row 2: Guide + Done
        int guideW = Math.min(92, this.panelW / 5);
        int doneW = Math.min(132, this.panelW / 4);
        int bottomPairStart = this.panelLeft + (this.panelW - (guideW + 6 + doneW)) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable("jcraft.config.open_guide"),
                        b -> Minecraft.getInstance().gui.setScreen(new StandGuideScreen(this)))
                .bounds(bottomPairStart, footerTop + 21, guideW, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> saveAndClose(true))
                .bounds(bottomPairStart + guideW + 6, footerTop + 21, doneW, 18)
                .build());
    }

    private void switchTab(int tabIdx) {
        this.listeningKey = null;
        this.activeTab = Mth.clamp(tabIdx, 0, tabs.size() - 1);
        this.scrollOffset = 0;
        this.init();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (listeningKey != null) {
            listeningKey.setKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
            listeningKey = null;
            KeyMapping.resetMapping();
            saveKeyMappings();
            this.init();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (listeningKey != null) {
            if (event.isEscape()) {
                listeningKey.setKey(InputConstants.UNKNOWN);
            } else {
                listeningKey.setKey(InputConstants.getKey(event));
            }
            listeningKey = null;
            KeyMapping.resetMapping();
            saveKeyMappings();
            this.init();
            return true;
        }
        int key = event.key();
        if (key >= InputConstants.KEY_1 && key <= InputConstants.KEY_6) {
            switchTab(key - InputConstants.KEY_1);
            return true;
        }
        if (key == InputConstants.KEY_UP) {
            return scrollRows(-1);
        }
        if (key == InputConstants.KEY_DOWN) {
            return scrollRows(1);
        }
        if (key == InputConstants.KEY_PAGEUP) {
            return scrollRows(-rowsPerPage);
        }
        if (key == InputConstants.KEY_PAGEDOWN) {
            return scrollRows(rowsPerPage);
        }
        return super.keyPressed(event);
    }

    private boolean scrollRows(int delta) {
        List<ConfigRow> activeRows = tabs.get(activeTab);
        int maxStart = Math.max(0, activeRows.size() - rowsPerPage);
        if (maxStart <= 0 || delta == 0) {
            return false;
        }
        int next = Mth.clamp(scrollOffset + delta, 0, maxStart);
        if (next != scrollOffset) {
            scrollOffset = next;
            this.init();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        List<ConfigRow> activeRows = tabs.get(activeTab);
        int maxStart = Math.max(0, activeRows.size() - rowsPerPage);
        if (maxStart > 0 && scrollY != 0) {
            int delta = scrollY > 0 ? -1 : 1;
            int next = Mth.clamp(scrollOffset + delta, 0, maxStart);
            if (next != scrollOffset) {
                scrollOffset = next;
                this.init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        gui.fill(0, 0, this.width, this.height, 0x9905030B);
        gui.fill(panelLeft, panelTop, panelLeft + panelW, panelTop + panelH, 0xEE141020);
        gui.outline(panelLeft, panelTop, panelW, panelH, 0xFF9333EA);

        // Alternating row backgrounds inside the scroll area
        List<ConfigRow> activeRows = tabs.get(activeTab);
        int endIdx = Math.min(activeRows.size(), scrollOffset + rowsPerPage);
        for (int i = scrollOffset; i < endIdx; i++) {
            int ry = bodyTop + (i - scrollOffset) * ROW_H;
            int rowBg = (i % 2 == 0) ? 0x881E1830 : 0x55181326;
            boolean hovered = mouseX >= panelLeft + 8 && mouseX <= panelLeft + panelW - 8
                    && mouseY >= ry && mouseY < ry + ROW_H;
            if (hovered) {
                rowBg = 0xAA2E234A;
            }
            gui.fill(panelLeft + 8, ry, panelLeft + panelW - 8, ry + ROW_H - 1, rowBg);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gui, mouseX, mouseY, partialTick);

        gui.centeredText(this.font, Component.translatable("jcraft.config.screen_title"), this.panelLeft + this.panelW / 2, this.panelTop + 6, 0xFFFDE68A);

        List<ConfigRow> activeRows = tabs.get(activeTab);
        int endIdx = Math.min(activeRows.size(), scrollOffset + rowsPerPage);
        int labelX = this.panelLeft + 13;
        int valueRightX = this.panelLeft + this.panelW - 10 - CTRL_W - 6;
        int maxLabelW = Math.max(60, valueRightX - VALUE_W - labelX - 6);

        ConfigRow hoveredRow = null;

        for (int i = scrollOffset; i < endIdx; i++) {
            ConfigRow row = activeRows.get(i);
            int ry = bodyTop + (i - scrollOffset) * ROW_H;
            int textY = ry + 5;

            String rawLabel = row.label().getString();
            String fittedLabel = this.font.width(rawLabel) > maxLabelW
                    ? this.font.plainSubstrByWidth(rawLabel, maxLabelW - 8) + "..."
                    : rawLabel;
            int labelColor = (row.serverSide() && !canEditServerConfig()) ? 0xFF9CA3AF : 0xFFF3F4F6;
            gui.text(this.font, fittedLabel, labelX, textY, labelColor, false);

            String val = row.valueDisplay().get();
            int valW = this.font.width(val);
            gui.text(this.font, val, valueRightX - valW, textY, row.valueColor().get(), false);

            if (mouseX >= this.panelLeft + 8 && mouseX <= valueRightX && mouseY >= ry && mouseY < ry + ROW_H) {
                hoveredRow = row;
            }
        }

        // Scrollbar indicator
        if (activeRows.size() > rowsPerPage) {
            int barX = this.panelLeft + this.panelW - 6;
            int trackH = bodyBottom - bodyTop;
            gui.fill(barX, bodyTop, barX + 3, bodyBottom, 0x44FFFFFF);
            int thumbH = Math.max(16, (int) ((rowsPerPage / (double) activeRows.size()) * trackH));
            int maxStart = activeRows.size() - rowsPerPage;
            int thumbY = bodyTop + (int) ((scrollOffset / (double) maxStart) * (trackH - thumbH));
            gui.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFC084FC);
        }

        // Status / page info on bottom footer
        int footerY = this.panelTop + this.panelH - 21 + 5;
        String rangeInfo = activeRows.isEmpty()
                ? "0 / 0"
                : String.format("%d-%d / %d", scrollOffset + 1, endIdx, activeRows.size());
        gui.text(this.font, rangeInfo, this.panelLeft + 10, footerY, 0xFFC4B5FD, false);

        if (activeTab < 4 && !canEditServerConfig()) {
            String opMsg = Component.translatable("jcraft.config.footer.op_required").getString();
            gui.text(this.font, opMsg, this.panelLeft + this.panelW - this.font.width(opMsg) - 10, footerY, 0xFFF87171, false);
        } else if (activeTab == 5) {
            String keyHint = listeningKey != null
                    ? Component.translatable("jcraft.config.footer.press_key").getString()
                    : Component.translatable("jcraft.config.footer.click_rebind").getString();
            gui.text(this.font, keyHint, this.panelLeft + this.panelW - this.font.width(keyHint) - 10, footerY,
                    listeningKey != null ? 0xFFFDE047 : 0xFF93C5FD, false);
        } else if (!dirtyServerOptions.isEmpty()) {
            String modMsg = Component.translatable("jcraft.config.footer.modified", dirtyServerOptions.size()).getString();
            gui.text(this.font, modMsg, this.panelLeft + this.panelW - this.font.width(modMsg) - 10, footerY, 0xFF34D399, false);
        }

        // Tooltip popup for hovered row (exact font.split wrapping so text never overflows)
        if (hoveredRow != null && hoveredRow.tooltip() != null) {
            int tipW = Math.min(250, this.width - 20);
            int innerW = Math.max(60, tipW - 12);
            List<net.minecraft.util.FormattedCharSequence> wrapped = this.font.split(hoveredRow.tooltip(), innerW);
            int actualW = 0;
            for (var line : wrapped) {
                actualW = Math.max(actualW, this.font.width(line));
            }
            int boxW = Math.min(tipW, Math.max(80, actualW + 12));
            int boxH = 10 + wrapped.size() * 10;
            int tx = Mth.clamp(mouseX + 10, 6, this.width - boxW - 6);
            int ty = Mth.clamp(mouseY - boxH - 6, 6, this.height - boxH - 6);
            gui.fill(tx, ty, tx + boxW, ty + boxH, 0xF2100A1C);
            gui.outline(tx, ty, boxW, boxH, 0xFFFACC15);
            int ly = ty + 5;
            for (var line : wrapped) {
                gui.text(this.font, line, tx + 6, ly, 0xFFE5E7EB, false);
                ly += 10;
            }
        }
    }

    private static void saveClientConfig() {
        try {
            AutoConfig.getConfigHolder(JClientConfig.class).save();
        } catch (Throwable ignored) {}
    }

    private void saveAndClose(boolean syncAll) {
        saveClientConfig();
        JServerConfig.syncGravityConfig();
        if (canEditServerConfig()) {
            Collection<ConfigOption> toSync = syncAll
                    ? ConfigOption.getOptions().values()
                    : dirtyServerOptions;
            ServerConfigUI.syncAndSaveOptions(toSync);
            dirtyServerOptions.clear();
        }
        Minecraft.getInstance().gui.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        saveAndClose(false);
    }
}
