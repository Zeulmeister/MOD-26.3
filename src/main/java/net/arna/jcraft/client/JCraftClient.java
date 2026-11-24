package net.arna.jcraft.client;

import com.google.common.base.MoreObjects;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import dev.architectury.registry.client.particle.ParticleProviderRegistry;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import lombok.Getter;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import mod.azure.azurelib.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.render.item.AzItemRendererRegistry;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.attack.enums.MoveInputType;
import net.arna.jcraft.api.pose.PoseModifiers;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.aim.GunAimHandler;
import net.arna.jcraft.client.gravity.util.GravityChannelClient;
import net.arna.jcraft.client.gui.hud.JCraftAbilityHud;
import net.arna.jcraft.client.gui.hud.JCraftHudOverlay;
import net.arna.jcraft.client.input.AerialKeyBinding;
import net.arna.jcraft.client.input.CrouchKeyBinding;
import net.arna.jcraft.client.input.IJKeyBinding;
import net.arna.jcraft.client.input.TrackedKeyBinding;
import net.arna.jcraft.client.net.ClientPacketHandler;
import net.arna.jcraft.client.particle.*;
import net.arna.jcraft.client.registry.JClientEventsRegistry;
import net.arna.jcraft.client.registry.JRenderLayerRegistry;
import net.arna.jcraft.client.renderer.armor.ArmorRenderer;
import net.arna.jcraft.client.renderer.effects.AttackHitboxEffectRenderer;
import net.arna.jcraft.client.renderer.effects.TimeErasePredictionEffectRenderer;
import net.arna.jcraft.client.renderer.item.GasCanItemRenderer;
import net.arna.jcraft.client.renderer.item.PeacemakerItemRenderer;
import net.arna.jcraft.client.rendering.StandUserPoseLoader;
import net.arna.jcraft.client.rendering.handler.*;
import net.arna.jcraft.client.sound.BoundSoundClient;
import net.arna.jcraft.client.util.BlockBreakerClient;
import net.arna.jcraft.client.util.ClientEntityHandlerImpl;
import net.arna.jcraft.common.util.MovementInputType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import com.mojang.blaze3d.platform.InputConstants;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class JCraftClient {
    // Keybinds
    public static final String JCRAFT_KEY_CAT = "key.category.jcraft";
    public static final TrackedKeyBinding STAND_SUMMON_KEY = TrackedKeyBinding.create("key.jcraft.standsummon",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_N, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding HEAVY_KEY = TrackedKeyBinding.create("key.jcraft.heavy",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_R, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding BARRAGE_KEY = TrackedKeyBinding.create("key.jcraft.barrage",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_B, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding ULT_KEY = TrackedKeyBinding.create("key.jcraft.ultimate",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_H, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding SPECIAL1_KEY = TrackedKeyBinding.create("key.jcraft.special1",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_V, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding SPECIAL2_KEY = TrackedKeyBinding.create("key.jcraft.special2",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_G, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding SPECIAL3_KEY = TrackedKeyBinding.create("key.jcraft.special3",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_M, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding COOLDOWN_CANCEL_KEY = TrackedKeyBinding.create("key.jcraft.cooldowncancel",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_LALT, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding UTILITY_KEY = TrackedKeyBinding.create("key.jcraft.utility",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_J, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding DASH_KEY = TrackedKeyBinding.create("key.jcraft.dash",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_X, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding PRECISION_VISION_KEY = TrackedKeyBinding.create("key.jcraft.precision_vision",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Z, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding POSE_TAUNT_KEY = TrackedKeyBinding.create("key.jcraft.pose_taunt",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_P, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding PILOT_MODE_KEY = TrackedKeyBinding.create("key.jcraft.pilot_mode",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_O, JCRAFT_KEY_CAT);
    public static final TrackedKeyBinding RADIAL_MENU_KEY = TrackedKeyBinding.create("key.jcraft.radial_menu",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_Y, JCRAFT_KEY_CAT);
    public static final IJKeyBinding AERIAL_VARIANT_KEY = AerialKeyBinding.INSTANCE;
    public static final IJKeyBinding CROUCH_VARIANT_KEY = CrouchKeyBinding.INSTANCE;

    @Getter(lazy = true)
    private static final Map<IJKeyBinding, MoveInputType> bindings = ImmutableMap.<IJKeyBinding, MoveInputType>builder()
            .put(STAND_SUMMON_KEY, MoveInputType.STAND_SUMMON)
            .put(TrackedKeyBinding.wrap(Minecraft.getInstance().options.keyAttack), MoveInputType.LIGHT)
            .put(HEAVY_KEY, MoveInputType.HEAVY)
            .put(BARRAGE_KEY, MoveInputType.BARRAGE)
            .put(SPECIAL1_KEY, MoveInputType.SPECIAL1)
            .put(SPECIAL2_KEY, MoveInputType.SPECIAL2)
            .put(SPECIAL3_KEY, MoveInputType.SPECIAL3)
            .put(ULT_KEY, MoveInputType.ULTIMATE)
            .put(UTILITY_KEY, MoveInputType.UTILITY)
            .put(TrackedKeyBinding.wrap(Minecraft.getInstance().options.keyPickItem), MoveInputType.TOSS)
            .build();
    @Getter(lazy = true)
    private static final Map<IJKeyBinding, MovementInputType> movementBindings = createMovementBindingsMap();
    @Getter(lazy = true)
    private static final TrackedKeyBinding trackedUseKey = TrackedKeyBinding.wrap(Minecraft.getInstance().options.keyUse);
    public static Supplier<DecimalFormat> decimalFormat = Suppliers.memoize(JCraftClient::createDecimalFormat);
    // public static KeyMapping menuKey;
    public static boolean comboStarted = false;
    public static int framesSinceComboStarted = 0;

    public static void init() {
        JCraft.setClientEntityHandler(ClientEntityHandlerImpl.INSTANCE);
        net.arna.jcraft.common.tickable.Timestops.setClientTsRangeChecker(net.arna.jcraft.client.util.JClientUtils::isInTSRange);

        AutoConfig.register(JClientConfig.class, JanksonConfigSerializer::new);
        JClientConfig.load();

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, new DecimalFormatUpdater(), JCraft.id("decimal_format_updater"));
        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, net.arna.jcraft.api.registry.JServerReloadListenerRegistry.adapt(StandUserPoseLoader::onReload), JCraft.id("stand_user_pose_loader"));

        GravityChannelClient.init();

        BlockBreakerClient.init();
        BoundSoundClient.init();
        GunAimHandler.init();

        // Rendering
        JRenderLayerRegistry.init();
        
        JClientEventsRegistry.registerClientEvents();
        JCraftAbilityHud.init();
        JCraftHudOverlay.init();
        PoseModifiers.register();

        AzArmorRendererRegistry.register(ArmorRenderer.simple("stone_mask"), JItemRegistry.STONE_MASK.get());
        AzArmorRendererRegistry.register(ArmorRenderer.simple("red_hat"), JItemRegistry.RED_HAT.get());

        AzItemRendererRegistry.register(JItemRegistry.GAS_CAN.get(), GasCanItemRenderer::new);
        AzItemRendererRegistry.register(JItemRegistry.PEACEMAKER.get(), PeacemakerItemRenderer::new);

        SpecialParticleShaderHandler.INSTANCE.init();
        ZaWarudoShaderHandler.INSTANCE.init();
        CrimsonShaderHandler.INSTANCE.init();
        EpitaphVignetteShaderHandler.INSTANCE.init();
        MandomRewindShaderHandler.INSTANCE.init();
        RangerFocusShaderHandler.INSTANCE.init();
        DevilsPalmShaderHandler.INSTANCE.init();
        ColorShiftShaderHandler.INSTANCE.init();

        // Renderer registration

        ClientPacketHandler.init();

        AttackHitboxEffectRenderer.init();
        TimeErasePredictionEffectRenderer.init();
    }
    
    private static boolean keyBindingsSanitized = false;

    public static List<TrackedKeyBinding> getJCraftKeyBindings() {
        return List.of(
                STAND_SUMMON_KEY, HEAVY_KEY, BARRAGE_KEY,
                SPECIAL1_KEY, SPECIAL2_KEY, SPECIAL3_KEY,
                ULT_KEY, UTILITY_KEY, DASH_KEY,
                COOLDOWN_CANCEL_KEY, PRECISION_VISION_KEY,
                PILOT_MODE_KEY, POSE_TAUNT_KEY, RADIAL_MENU_KEY
        );
    }

    public static void registerKeyBindings(@Nullable Consumer<KeyMapping> register) {
        Consumer<KeyMapping> consumer = MoreObjects.firstNonNull(register, KeyMappingRegistry::register);
        getJCraftKeyBindings().forEach(kb -> consumer.accept(kb.getParent()));
    }

    /**
     * Resolves duplicate JCraft ability keybinds (e.g. from older options.txt saves)
     * and unbinds non-combat Vanilla popup keys that collide with JCraft defaults.
     */
    public static void sanitizeKeyBindings(@Nullable Minecraft client) {
        if (keyBindingsSanitized || client == null || client.options == null) {
            return;
        }
        keyBindingsSanitized = true;
        applyKeyBindingSanitization(client);
    }

    private static void applyKeyBindingSanitization(Minecraft client) {
        boolean changed = false;
        List<TrackedKeyBinding> all = getJCraftKeyBindings();

        for (int i = 0; i < all.size(); i++) {
            KeyMapping first = all.get(i).getParent();
            if (first.isUnbound()) continue;
            for (int j = i + 1; j < all.size(); j++) {
                KeyMapping second = all.get(j).getParent();
                if (!second.isUnbound() && first.same(second)) {
                    second.setKey(second.getDefaultKey());
                    changed = true;
                }
            }
        }

        changed |= unbindConflictingVanillaPopupKeys(client.options, all);

        if (changed) {
            KeyMapping.resetMapping();
            client.options.save();
        }
    }

    public static void resetKeyBindingsToDefaults(Minecraft client) {
        List<TrackedKeyBinding> all = getJCraftKeyBindings();
        for (TrackedKeyBinding tracked : all) {
            KeyMapping kb = tracked.getParent();
            kb.setKey(kb.getDefaultKey());
        }
        if (client != null && client.options != null) {
            unbindConflictingVanillaPopupKeys(client.options, all);
            KeyMapping.resetMapping();
            client.options.save();
        } else {
            KeyMapping.resetMapping();
        }
    }

    private static boolean unbindConflictingVanillaPopupKeys(Options options, List<TrackedKeyBinding> all) {
        boolean changed = false;
        List<KeyMapping> vanillaPopupKeys = List.of(
                options.keyQuickActions,
                options.keyFriends,
                options.keySocialInteractions,
                options.keyLoadHotbarActivator
        );
        for (KeyMapping vanilla : vanillaPopupKeys) {
            if (vanilla == null || vanilla.isUnbound()) continue;
            for (TrackedKeyBinding tracked : all) {
                KeyMapping jcraftKey = tracked.getParent();
                if (!jcraftKey.isUnbound() && vanilla.same(jcraftKey)) {
                    vanilla.setKey(InputConstants.UNKNOWN);
                    changed = true;
                    break;
                }
            }
        }
        return changed;
    }

    /// TEXT HUD
    public static final List<String> comboRemarks = List.of("admin rdm!!!", "baby combo", "caught lackin", "kinda ez", "skill issue", "cancelled on twitter", "sent to bulgaria", "down bad");
    public static int comboCounter = 0;
    public static float damageScaling = 1.00f;
    public static int framesSinceCounted = 0;

    public static int IPSTriggerFramesLeft = 0;
    public static final int IPS_TRIGGER_FRAMES = 120;

    public static void markIPSTriggered() {
        IPSTriggerFramesLeft = IPS_TRIGGER_FRAMES;
    }


    public static void markComboStarted() {
        comboStarted = true;
        framesSinceComboStarted = 0;
    }

    private static Map<IJKeyBinding, MovementInputType> createMovementBindingsMap() {
        final Options options = Minecraft.getInstance().options;
        return ImmutableMap.<IJKeyBinding, MovementInputType>builder()
                .put(TrackedKeyBinding.wrap(options.keyUp), MovementInputType.FORWARD)
                .put(TrackedKeyBinding.wrap(options.keyDown), MovementInputType.BACKWARD)
                .put(TrackedKeyBinding.wrap(options.keyLeft), MovementInputType.LEFT)
                .put(TrackedKeyBinding.wrap(options.keyRight), MovementInputType.RIGHT)
                .put(TrackedKeyBinding.wrap(options.keyJump), MovementInputType.JUMP)
                .put(TrackedKeyBinding.wrap(options.keyShift), MovementInputType.CROUCH)
                .put(DASH_KEY, MovementInputType.DASH)
                .put(TrackedKeyBinding.wrap(options.keyPickItem), MovementInputType.THROW)
                .build();
    }

    /**
     * @return a cleaned-up version of TranslatableText name of button
     */
    public static String generateName(final KeyMapping keyBinding, boolean makeShort) {
        if (keyBinding.isUnbound()) {
            return "-";
        }
        final String str = keyBinding.saveString();
        if (makeShort) {
            switch (str) {
                case "key.mouse.left" -> { return "M1"; }
                case "key.mouse.right" -> { return "M2"; }
                case "key.mouse.middle" -> { return "M3"; }
                case "key.keyboard.left.alt", "key.keyboard.right.alt" -> { return "ALT"; }
                case "key.keyboard.left.shift", "key.keyboard.right.shift" -> { return "SHF"; }
                case "key.keyboard.left.control", "key.keyboard.right.control" -> { return "CTL"; }
                case "key.keyboard.space" -> { return "SPC"; }
                case "key.keyboard.tab" -> { return "TAB"; }
                case "key.keyboard.caps.lock" -> { return "CAP"; }
                case "key.keyboard.grave.accent" -> { return "`"; }
                default -> {
                    if (str.startsWith("key.mouse.")) {
                        String btn = str.substring("key.mouse.".length());
                        try {
                            return "M" + (Integer.parseInt(btn) + 1);
                        } catch (NumberFormatException ignored) {
                            return "M" + btn.toUpperCase(Locale.ROOT);
                        }
                    }
                    if (str.startsWith("key.keyboard.keypad.")) {
                        return "N" + str.substring("key.keyboard.keypad.".length()).toUpperCase(Locale.ROOT);
                    }
                    if (str.startsWith("key.keyboard.f") && str.length() <= "key.keyboard.f12".length()) {
                        return str.substring("key.keyboard.".length()).toUpperCase(Locale.ROOT);
                    }
                }
            }
        }
        final String[] components = str.split("\\.");
        if (components.length < 2) {
            return makeShort && !str.isEmpty() ? str.substring(0, 1).toUpperCase(Locale.ROOT) : str;
        }

        String last = components[components.length - 1];
        String secondLast = components[components.length - 2] + " ";

        if (components[components.length - 2].equals("keyboard")) {
            secondLast = "";
        }

        if (makeShort) {
            if (!secondLast.isEmpty()) secondLast = secondLast.substring(0, 1);
            if (!last.isEmpty()) last = last.substring(0, 1);
        }

        return StringUtils.capitalize(secondLast) + StringUtils.capitalize(last);
    }

    public static <E extends Enum<E>> Object2BooleanMap<E> getChangedInputs(final Map<? extends IJKeyBinding, E> bindings) {
        return bindings.entrySet().stream()
                .filter(entry -> entry.getKey().isChangedThisTick())
                .collect(Object2BooleanOpenHashMap::new, (map, entry) ->
                                map.put(entry.getValue(), entry.getKey().isPressedThisTick()),
                        Object2BooleanMap::putAll);
    }

    @Nullable
    public static StandEntity<?, ?> getStandEntity() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }

        for (Entity e : player.getPassengers()) {
            if (e instanceof StandEntity<?, ?> s) {
                return s;
            }
        }
        return net.arna.jcraft.common.util.JUtils.getStand(player);
    }

    private static DecimalFormat createDecimalFormat() {
        return new DecimalFormat("#.#", DecimalFormatSymbols.getInstance(
                Locale.forLanguageTag(Minecraft.getInstance().options.languageCode)));
    }

    public static void registerParticleSpriteSets() {
        // TODO: merge Forge version handling with this somehow. until then _KEEP THEM IN SYNC_
        // See JCraftForgeClient#onParticleFactoryRegistration(RegisterParticleProvidersEvent)

        ParticleProviderRegistry.register(JParticleTypeRegistry.COMBO_BREAK, ComboBreakerParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.COOLDOWN_CANCEL, CooldownCancelParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_1, provider -> new HitsparkParticle.Factory(provider, 0.4f, 5));
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_2, provider -> new HitsparkParticle.Factory(provider, 0.66f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.HITSPARK_3, provider -> new HitsparkParticle.Factory(provider, 1f, 8));
        ParticleProviderRegistry.register(JParticleTypeRegistry.BLOOD_HITSPARK_2, provider -> new HitsparkParticle.Factory(provider, 0.66f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.INVERTED_HITSPARK_3, provider -> new InvertedHitsparkParticle.Factory(provider, 1f, 8));
        ParticleProviderRegistry.register(JParticleTypeRegistry.STUN_SLASH, provider -> new HitsparkParticle.Factory(provider, 0.6f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.STUN_PIERCE, provider -> new HitsparkParticle.Factory(provider, 0.6f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.KCPARTICLE, KCParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BACKSTAB, BackstabParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.SPEED_PARTICLE, SpeedParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BITES_THE_DUST, BitesTheDustParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BOOM_1, BoomParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.PIXEL, PixelParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BLOCKSPARK, provider -> new BlocksparkParticle.Factory(provider, 0.15f));
        ParticleProviderRegistry.register(JParticleTypeRegistry.GO, GoParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.DO, GoParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.AURA_ARC, AuraArcParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.AURA_BLOB, AuraBlobParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.INVERSION, InversionParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.BREATH, BreathParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.SUN_LOCK_ON, BackstabParticle.Factory::new); // 9 frames, reusing
        ParticleProviderRegistry.register(JParticleTypeRegistry.LOCK_ON, LockOnParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.PURPLE_HAZE_CLOUD, PurpleHazeCloudParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.PURPLE_HAZE_PARTICLE, PurpleHazeErraticParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.DAMAGE_NUMBER, DamageNumberParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.HAMON_SPARK, provider -> new HitsparkParticle.Factory(provider, 0.2f, 6));
        ParticleProviderRegistry.register(JParticleTypeRegistry.LEMON, LemonParticle.Factory::new);
        ParticleProviderRegistry.register(JParticleTypeRegistry.METALLICA_MOSH, MoshParticle.Factory::new);
    }

    @Getter
    private static class DecimalFormatUpdater implements PreparableReloadListener {
        @Override
        public CompletableFuture<Void> reload(final SharedState sharedState, final Executor backgroundExecutor, final PreparationBarrier synchronizer, final Executor gameExecutor) {
            return synchronizer.wait(null).thenRunAsync(
                    () -> {
                        JCraftAbilityHud.clearTextureCache();
                        decimalFormat.get().setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(
                                java.util.Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().replace('_', '-'))));
                    }, gameExecutor);
        }
    }
}
