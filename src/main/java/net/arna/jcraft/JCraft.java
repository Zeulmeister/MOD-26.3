package net.arna.jcraft;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.StringReader;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import mod.azure.azurelib.animation.cache.AzIdentityRegistry;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.misc.ConditionalFlightHandler;
import net.arna.jcraft.api.misc.JBlockBreaker;
import net.arna.jcraft.api.registry.*;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.argumenttype.StandArgumentType;
import net.arna.jcraft.common.block.CoffinBlock;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.effects.DazedStatusEffect;
import net.arna.jcraft.common.entity.projectile.KnifeProjectile;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.config.GravityChangerConfig;
import net.arna.jcraft.common.gravity.util.GravityChannel;
import net.arna.jcraft.common.loot.JLootTableHelper;
import net.arna.jcraft.common.network.RemoteStandInteractPacket;
import net.arna.jcraft.common.network.c2s.*;
import net.arna.jcraft.common.network.s2c.*;
import net.arna.jcraft.common.saveddata.ExclusiveStandsData;
import net.arna.jcraft.common.saveddata.ServerJoinData;
import net.arna.jcraft.common.spec.AnubisSpec;
import net.arna.jcraft.common.spec.BrawlerSpec;
import net.arna.jcraft.common.spec.HamonSpec;
import net.arna.jcraft.common.spec.VampireSpec;
import net.arna.jcraft.common.tickable.JEnemies;
import net.arna.jcraft.common.tickable.MoveTickQueue;
import net.arna.jcraft.common.tickable.PastDimensions;
import net.arna.jcraft.common.tickable.Timestops;
import net.arna.jcraft.common.util.*;
import net.arna.jcraft.common.villagertrades.VillagerTradesModifier;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

import static net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent.Shockwave;
import static net.arna.jcraft.api.registry.JBlockEntityTypeRegistry.BLOCK_ENTITY_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JBlockRegistry.BLOCK_REGISTRY;
import static net.arna.jcraft.api.registry.JEntityTypeRegistry.ENTITY_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JItemRegistry.ITEM_REGISTRY;
import static net.arna.jcraft.api.registry.JMarkerExtractorRegistry.EXTRACTOR_REGISTRY;
import static net.arna.jcraft.api.registry.JMarkerInjectorRegistry.INJECTOR_REGISTRY;
import static net.arna.jcraft.api.registry.JMoveActionTypeRegistry.MOVE_ACTION_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JMoveConditionTypeRegistry.MOVE_CONDITION_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JMoveTypeRegistry.MOVE_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JSpecTypeRegistry.SPEC_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JSplatterTypeRegistry.SPLATTER_TYPE_REGISTRY;
import static net.arna.jcraft.api.registry.JStandTypeRegistry.STAND_TYPE_REGISTRY;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;

public final class JCraft {
    // Unchanging mod values
    public static final String MOD_ID = "jcraft";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);


    public static final int SPEC_QUEUE_MOVESTUN_LIMIT = 11; // exclusive, 10 -> 0.5s window for queueing moves
    public static final int QUEUE_MOVESTUN_LIMIT = 7; // exclusive, 6 -> 0.3s window for queueing moves

    public static final GravityChangerConfig gravityConfig = new GravityChangerConfig(); // TODO incorporate this into our own config

    // List of spec classes that will be initialized to get their movesets.
    public static final List<Class<?>> SPEC_CLASSES = ImmutableList.of(BrawlerSpec.class, HamonSpec.class, VampireSpec.class, AnubisSpec.class);

    //Obligatory lazy Registry


    public static final DeferredRegister<CreativeModeTab> CREATIVE_TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(MOD_ID, Registries.PARTICLE_TYPE);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(MOD_ID, Registries.MOB_EFFECT);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(MOD_ID, Registries.SOUND_EVENT);
    public static final DeferredRegister<Enchantment> ENCHANTMENT = DeferredRegister.create(MOD_ID, Registries.ENCHANTMENT);
    public static final DeferredRegister<Identifier> STATS = DeferredRegister.create(MOD_ID, Registries.CUSTOM_STAT);
    public static final DeferredRegister<MenuType<?>> MENU_REGISTRY = DeferredRegister.create(JCraft.MOD_ID, Registries.MENU);

    // Gamerules
    //public static final GameRules.Key<GameRules.BooleanRule> KINGCRIMSON_TELEPORT_EFFECT = GameRuleRegistry.register("kingCrimsonTeleportEffect", GameRules.Category.MISC, GameRuleFactory.createBooleanRule(false));
    //public static final GameRules.Key<BooleanValue> COMBO_COUNTER = register("comboCounter", Category.MISC, BooleanValue.create(true));
        public static final GameRule<Integer> CHANCE_MOB_SPAWNS_WITH_STAND = GameRuleBuilder.forInteger(5).category(GameRuleCategory.MOBS).buildAndRegister(id("chance_mob_spawns_with_stand"));
    public static final GameRule<Boolean> ALLOW_MOB_EVOLVED_STANDS = GameRuleBuilder.forBoolean(false).category(GameRuleCategory.MOBS).buildAndRegister(id("allow_mob_evolved_stands"));
    public static final GameRule<Boolean> STAND_GRIEFING = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.MISC).buildAndRegister(id("stand_griefing"));
    public static final GameRule<Boolean> KEEP_STAND = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(id("keep_stand"));
    public static final GameRule<Boolean> DROP_STAND_AS_DISC = GameRuleBuilder.forBoolean(false).category(GameRuleCategory.DROPS).buildAndRegister(id("drop_stand_as_disc"));
    public static final GameRule<Boolean> KEEP_SPEC = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.PLAYER).buildAndRegister(id("keep_spec"));
    public static final GameRule<Integer> STAND_ARROW_BASE_DAMAGE = GameRuleBuilder.forInteger(2).category(GameRuleCategory.MISC).buildAndRegister(id("stand_arrow_base_damage"));
    public static final GameRule<Boolean> FALLING_METEORS = GameRuleBuilder.forBoolean(true).category(GameRuleCategory.SPAWNING).buildAndRegister(id("do_falling_meteors"));

    /**
     * String ID of the base controller.
     */
    public static final String BASE_CONTROLLER = "base_controller";

    // General-purpose gun controllers
    public static final String FIRE_CONTROLLER = "fire_controller";

    // Dimensional travel bullshit
    /**
     * Used to lock the AU chunks from being unloaded automatically by JServerTickEvents
     */
    public static int preloadLockTicks = 0;
    public static ServerLevel auWorld;
    private static final List<ChunkPos> preloadedChunks = new ArrayList<>();

    public static final Object2IntMap<LivingEntity> burstTimers = new Object2IntOpenHashMap<>();
    public static final Object2IntMap<LivingEntity> pushblockCooldowns = new Object2IntOpenHashMap<>();

    public static final Map<LivingEntity, DashData> dashes = new WeakHashMap<>();

    @Getter
    private static final Map<Entity, EntityInterest> entitiesOfInterest = new HashMap<>();

    // Standardized cool-downs
    public static final int DASH_COOLDOWN = 40, LIGHT_COOLDOWN = 20;

    @Getter
    @Setter
    private static IClientEntityHandler clientEntityHandler = DummyClientEntityHandler.INSTANCE;

    @Getter
    @Setter
    private static ExclusiveStandsData exclusiveStandsData = null;

    @Getter
    @Setter
    private static ServerJoinData serverJoinData = null;

    public static void init() {
        /*  NAMING PATTERN:
                JThingRegistry.init() - for interfaces with an EMPTY init() method, used for instantiating them
                JThingRegistry.registerThings() - for interfaces/classes with a NON-EMPTY registerThings() method
         */

        JRegistries.init();

        // Particle registration (serverside)
        JParticleTypeRegistry.init();
        PARTICLES.register();

        JSoundRegistry.init();
        SOUNDS.register();

        ENTITY_TYPE_REGISTRY.register();
        BLOCK_REGISTRY.register();
        ITEM_REGISTRY.register();
        BLOCK_ENTITY_TYPE_REGISTRY.register();

        JRecipeRegistry.register();

        // Custom registries
        STAND_TYPE_REGISTRY.register();
        SPEC_TYPE_REGISTRY.register();
        MOVE_ACTION_TYPE_REGISTRY.register();
        MOVE_CONDITION_TYPE_REGISTRY.register();
        MOVE_TYPE_REGISTRY.register();
        EXTRACTOR_REGISTRY.register();
        INJECTOR_REGISTRY.register();
        SPLATTER_TYPE_REGISTRY.register();

        JTagRegistry.init();
        JAdvancementTriggerRegistry.init();

        JCreativeMenuTabRegistry.init();
        CREATIVE_TAB_REGISTRY.register();

        JMenuRegistry.init();
        MENU_REGISTRY.register();

        CommandRegistrationEvent.EVENT.register(JCommandRegistry::registerCommands);

        JEventsRegistry.registerEvents();

        JStatusRegistry.init();
        EFFECTS.register();

        JEntityTypeRegistry.registerAttributes();

        JStructureTypeRegistry.register();
        JStructurePieceRegistry.register();

        JDimensionRegistry.init();

        // Command Arguments are registered separately in JCraftFabric and JCraftForge

        JEnchantmentRegistry.init();
        
        JLootTableHelper.registerLootTables();

        TimeAccelStatePacket.init(); // register event handlers for time acceleration

        // TODO: Loot Table registration is currently only finalized on Fabric, which @Ayutac needs to fix

        JServerConfig.init();

        JStatRegistry.init();
        STATS.register();

        MoveTickQueue.registerMoveTickQueue();
        GravityChannel.registerReceivers();
        JBlockBreaker.init();
        VillagerTradesModifier.init();
        ConditionalFlightHandler.init();

        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_PLAYER_INPUT, PlayerInputPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_PLAYER_INPUT_HOLD, PlayerInputPacket::handleHold);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), ConfigUpdatePacket.ID, ConfigUpdatePacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_STAND_BLOCK, StandBlockPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_COOLDOWN_CANCEL, CooldownCancelPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_REMOTE_STAND_INTERACT, RemoteStandInteractPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_PREDICTION_TRIGGER, PredictionTriggerPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_MENU_CALL, MenuCallPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_VARIANT, VariantInputPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_GUN_AIM, GunAimPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_POSE_TAUNT, PoseTauntPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_REMOTE_CONTROL_TOGGLE, RemoteControlTogglePacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_STAND_ROTATION, StandRotationPacket::handle);
        net.arna.jcraft.common.network.JNetwork.registerReceiver(NetworkManager.c2s(), JPacketRegistry.C2S_RADIAL_ACTION, RadialMenuActionPacket::handle);

        // Pre-register all S2C payload channels for dedicated server compatibility
        for (Identifier s2cId : List.of(
                JPacketRegistry.S2C_SERVER_CHANNEL_FEEDBACK, JPacketRegistry.S2C_PLAYER_ANIMATION,
                JPacketRegistry.S2C_SHADER_ACTIVATION, JPacketRegistry.S2C_SHADER_DEACTIVATION,
                JPacketRegistry.S2C_TIME_ACCELERATION_STATE, JPacketRegistry.S2C_EPITAPH_STATE,
                JPacketRegistry.S2C_TIME_ERASE_PREDICTION_STATE, JPacketRegistry.S2C_SERVER_CONFIG,
                JPacketRegistry.S2C_J_EXPLOSION, JPacketRegistry.S2C_COMBO_COUNTER,
                JPacketRegistry.S2C_TIME_STOP, JPacketRegistry.S2C_SPLATTER,
                JPacketRegistry.S2C_STAND_HURT, JPacketRegistry.S2C_PREDICTION_UPDATE,
                JPacketRegistry.S2C_MAGNETIC_FIELD_PARTICLE, JPacketRegistry.S2C_ATTACKER_DATA,
                JPacketRegistry.S2C_MANDOM_DATA, JPacketRegistry.S2C_STONE_MASK_CLENCH,
                JPacketRegistry.S2C_IPS_TRIGGERED, JPacketRegistry.S2C_DAMAGE_NUMBER,
                JPacketRegistry.S2C_BLOCK_BREAKAGE, JPacketRegistry.S2C_BOUND_SOUND,
                JPacketRegistry.S2C_COLOR_SHIFT, JPacketRegistry.S2C_KINETIC_TS_COUNT,
                JPacketRegistry.S2C_SYNC
        )) {
            net.arna.jcraft.common.network.JNetwork.registerS2CPayload(s2cId);
        }
    }

    private static void registerAzArmor() {
        AzIdentityRegistry.register(JItemRegistry.STONE_MASK.get());
        AzIdentityRegistry.register(JItemRegistry.RED_HAT.get());
    }

    // Held guns animate per stack, so each one needs its own identity rather than sharing the item's.
    private static void registerAzItems() {
        AzIdentityRegistry.register(JItemRegistry.PEACEMAKER.get());
    }

    public static void postInit() {
        initBlockPostLoad();
        EvolutionItemHandler.init();
        initDispenserBehaviors();
        JStatRegistry.initFormatters();
        registerAzArmor();
        registerAzItems();
        initMoveSets();
    }

    private static void initBlockPostLoad() {
        CoffinBlock.init();
    }

    private static void initDispenserBehaviors() {
        DispenserBlock.registerBehavior(JItemRegistry.KNIFE.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource pointer, ItemStack stack) {
                Level level = pointer.level();
                Position position = DispenserBlock.getDispensePosition(pointer);
                Direction direction = pointer.state().getValue(DispenserBlock.FACING);
                KnifeProjectile knife = new KnifeProjectile(level);
                knife.pickup = AbstractArrow.Pickup.ALLOWED;
                knife.setPos(position.x(), position.y(), position.z());
                knife.shoot(direction.getStepX(), direction.getStepY() + 0.1F, direction.getStepZ(), 1.5F, 1.0F);
                level.addFreshEntity(knife);
                stack.shrink(1);
                return stack;
            }
        });
    }

    private static void initMoveSets() {
        // Movesets are defined in static fields in the stand/spec classes.
        // These classes aren't loaded until the stand or spec is first used in-game.
        // This is too late as we want the movesets to be instantiated before they're loaded,
        // so the compatibility check happens during data load.

        // To fix this, we'll be collecting all stand classes from JEntityTypeRegistry
        // and instantiating them here, thus instantiating their movesets.

        List<Class<?>> classes = new ArrayList<>();
        for (Field field : JEntityTypeRegistry.class.getFields()) {
            Type type = field.getGenericType();
            // Check if the type of the field is RegistrySupplier<EntityType<? extends StandEntity<?, ?>>
            if (type instanceof ParameterizedType regSupType
                    && regSupType.getRawType() == RegistrySupplier.class
                    && regSupType.getActualTypeArguments().length == 1
                    && regSupType.getActualTypeArguments()[0] instanceof ParameterizedType entTypeType
                    && entTypeType.getActualTypeArguments().length == 1
                    && entTypeType.getActualTypeArguments()[0] instanceof Class<?> entityClass
                    && StandEntity.class.isAssignableFrom(entityClass)) {
                classes.add(entityClass);
            }
        }

        classes.addAll(SPEC_CLASSES);

        // Initialize classes
        // This will initialize their static fields which will initialize all move sets.
        for (Class<?> c : classes) {
            try {
                Class.forName(c.getName(), true, JCraft.class.getClassLoader());
            } catch (ClassNotFoundException ignored) {}
        }
    }

    public static void registerEntitySelectorOptions(final EntitySelectorOptionsRegistrar registrar) {
        registrar.register("jcraft_timestopped", parser -> {
            parser.setSuggestions((builder, consumer) ->
                    SharedSuggestionProvider.suggest(Arrays.asList("true", "false"), builder));

            boolean invert = parser.shouldInvertValue();
            StringReader reader = parser.getReader();
            boolean value = reader.readBoolean() && !invert;

            parser.addPredicate(e -> JUtils.isAffectedByTimeStop(e) == value);
        }, p -> true, Component.translatable("argument.entity.options.jcraft_timestopped"));

        registrar.register("jcraft_stand", parser -> {
            StringReader reader = parser.getReader();
            int cursor = reader.getCursor();
            Identifier resourceLocation = Identifier.read(reader);
            StandType standType;

            parser.setSuggestions((builder, consumer) ->
                    SharedSuggestionProvider.suggest(JRegistries.STAND_TYPE_REGISTRY.getIds().stream()
                                    .map(Identifier::toString), builder));

            try {
                standType = JRegistries.STAND_TYPE_REGISTRY.get(resourceLocation);
            } catch (IllegalArgumentException e) {
                reader.setCursor(cursor);
                throw StandArgumentType.NOT_FOUND.createWithContext(reader);
            }

            parser.addPredicate(e -> {
                if (!(e instanceof LivingEntity entity)) {
                    return false;
                }

                CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(entity);
                return standData.getType() == standType;
            });
        }, p -> true, Component.translatable("argument.entity.options.jcraft_stand"));
    }

    public static void markItemOfInterest(@NonNull Entity entity, @NonNull EntityInterest interest) {
        entitiesOfInterest.put(entity, interest);
    }

    /**
     * Starts tracking a timestop on the server.
     * Synchronizes with clients (upon timestop creation, not repeatedly)
     * Puts nearby players' items on cooldown.
     *
     * @param position in world
     */
    public static void beginTimestop(@NonNull final LivingEntity timestopper, @NonNull final Vec3 position,
                                     @NonNull final ServerLevel world, final int duration) {
        // Registration
        ResourceKey<Level> worldRegistryKey = world.dimension();
        Timestops.enqueue(new DimensionData(timestopper, position, worldRegistryKey, duration));

        // Synchronization - Sends to unaffected players because they may walk into range
        TimeStopStatePacket.send(world.players(), TimeStopStatePacket.createStartPacket(timestopper.getId(), position, worldRegistryKey, duration));

        final List<ServerPlayer> toStop = world.getEntitiesOfClass(ServerPlayer.class,
                new AABB(position.add(160.0, 160.0, 160.0), position.subtract(160.0, 160.0, 160.0)), EntitySelector.LIVING_ENTITY_STILL_ALIVE);

        for (ServerPlayer serverPlayer : toStop) {
            // Shader handling
            ShaderActivationPacket.send(serverPlayer, timestopper, 0, duration, ShaderActivationPacket.Type.ZA_WARUDO);
            if (serverPlayer == timestopper || serverPlayer.isCreative()) {
                continue;
            }

            // Puts all player items besides armor into cooldown for entire duration of timestop
            for (int i = 0; i < serverPlayer.getInventory().items.size(); i++) {
                serverPlayer.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(serverPlayer.getInventory().items.get(i).getItem()), duration);
            }
            serverPlayer.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(serverPlayer.getOffhandItem().getItem()), duration);
        }
    }

    public static void stopTimestop(Entity timestopper) {
        DimensionData timestop = Timestops.getTimestop(timestopper);
        Level world = timestopper.level();

        if (world instanceof ServerLevel serverWorld) {
            net.arna.jcraft.common.combat.TimeStopKineticPhysicsHandler.onTimeStopEnd(timestopper, serverWorld);
        }

        if (timestop == null || !(world instanceof ServerLevel serverWorld)) {
            return;
        }

        // Synchronization
        TimeStopStatePacket.send(serverWorld.players(), TimeStopStatePacket.createStopPacket(timestopper.getId()));

        Vec3 position = timestopper.position();

        List<ServerPlayer> toUnfreeze = serverWorld.getEntitiesOfClass(ServerPlayer.class,
                new AABB(position.add(160.0, 160.0, 160.0), position.subtract(160.0, 160.0, 160.0)), EntitySelector.LIVING_ENTITY_STILL_ALIVE);

        for (ServerPlayer serverPlayer : toUnfreeze) {
            // Shader handling
            ShaderDeactivationPacket.send(serverPlayer, ShaderActivationPacket.Type.ZA_WARUDO);

            // Removes cooldowns
            for (int i = 0; i < 36; i++) {
                ItemStack st = serverPlayer.getInventory().getItem(i);
                if (!st.isEmpty()) {
                    serverPlayer.getCooldowns().removeCooldown(serverPlayer.getCooldowns().getCooldownGroup(st));
                }
            }
            if (!serverPlayer.getOffhandItem().isEmpty()) {
                serverPlayer.getCooldowns().removeCooldown(serverPlayer.getCooldowns().getCooldownGroup(serverPlayer.getOffhandItem()));
            }
        }

        Timestops.remove(timestop);
    }

    /**
     * Clears pre/force loaded chunks in the AU
     */
    public static void clearPreloadedChunks() {
        if (preloadedChunks.isEmpty()) {
            return;
        }
        for (ChunkPos p : preloadedChunks) {
            auWorld.setChunkForced(p.x(), p.z(), false);
        }
        preloadedChunks.clear();
    }

    public static void preloadChunk(ServerLevel auWorld, int chunkX, int chunkZ) {
        // Already loaded, no need to do so again.
        ChunkPos targetChunk = new ChunkPos(chunkX, chunkZ);
        if (preloadedChunks.contains(targetChunk)) {
            return;
        }

        preloadedChunks.add(targetChunk);
        auWorld.setChunkForced(chunkX, chunkZ, true);
    }

    public static StandEntity<?, ?> summon(LivingEntity user) { return summon(user.level(), user); }

    public static StandEntity<?, ?> summon(Level world, LivingEntity user) {
        if (user.hasEffect(JStatusRegistry.STANDLESS.asHolder()) || user.isSpectator()) {
            return null;
        }
        CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(user);
        StandType type = standData.getType();
        if (StandTypeUtil.isNone(type)) {
            return null;
        }
        StandEntity<?, ?> stand = type.createEntity(world);

        int skin = standData.getSkin();
        stand.setSkin(skin);
        stand.setPos(user.position().subtract(user.getLookAngle()));
        stand.startRiding(user);
        stand.setUser(user);

        if (user instanceof ServerPlayer player) {
            if (JUtils.canAct(user) && StandBlockPacket.isBlocking(player)) {
                stand.wantToBlock = true;
                stand.tryBlock();
            }
        } else if (user instanceof Mob mob) {
            JEnemies.add(mob);
        }
        world.addFreshEntity(stand);
        standData.setStand(stand);
        return stand;
    }

    public static void createParticle(ServerLevel world, double x, double y, double z, JParticleType type) {
        if (world == null || type == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeShort(3);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(type);

        ServerChannelFeedbackPacket.send(JUtils.around(world, new Vec3(x, y, z), 128), buf);
    }

    public static void createHitsparks(ServerLevel world, double x, double y, double z, JParticleType type, int sparkCount, double sparkSpeed) {
        if (world == null || type == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeShort(5);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        buf.writeEnum(type);
        buf.writeInt(sparkCount);
        buf.writeDouble(sparkSpeed);

        ServerChannelFeedbackPacket.send(JUtils.around(world, new Vec3(x, y, z), 128), buf);
    }

    public static void createHitscanTraceParticle(ServerLevel world, Vec3 start, Vec3 velocity, JParticleType type) {
        if (world == null || type == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());

        buf.writeShort(14);
        buf.writeDouble(start.x());
        buf.writeDouble(start.y());
        buf.writeDouble(start.z());
        buf.writeDouble(velocity.x());
        buf.writeDouble(velocity.y());
        buf.writeDouble(velocity.z());
        buf.writeEnum(type);

        ServerChannelFeedbackPacket.send(JUtils.around(world, start, 128), buf);
    }

    public static void tryPushBlock(final ServerLevel world, final LivingEntity user, final @NonNull StandEntity<?, ?> stand) {
        if (pushblockCooldowns.getOrDefault(user, -1) > 0) {
            return;
        }

        final float third = stand.getMaxStandGauge() / 3.0f;
        final float gauge = stand.getStandGauge();

        if (gauge <= third) {
            return;
        }

        pushblockCooldowns.put(user, 8);

        stand.setStandGauge(gauge - third);

        stand.setMoveStun(10);

        world.playSound(null, user, JSoundRegistry.STAND_PUSHBLOCK.get(), SoundSource.PLAYERS, 1, 1);

        JUtils.setVelocity(user, Vec3.ZERO);
        final Vec3 position = user.position();
        final double userWidth = user.getBbWidth();

        final IntSet pushed = new IntOpenHashSet();

        for (final Entity entity : world.getAllEntities()) {
            if (entity == user || entity == stand) continue;
            if (!(entity instanceof final LivingEntity living)) continue;

            LivingEntity target = living;

            // If the target is using a stand, we consider the closer entity for pushblock distance calculations
            final CommonStandComponent standComponent = JComponentPlatformUtils.getStandComponent(living);
            if (standComponent.getStand() != null) {
                final StandEntity<?, ?> targetStand = standComponent.getStand();
                if (targetStand.distanceToSqr(position) < living.distanceToSqr(position)) target = targetStand;
            }

            final double distance = target.position().distanceTo(position);
            final double radius = target.getBbWidth() + 2.0;

            // If the target is a non-remote stand, we want to target its user instead
            if (target instanceof StandEntity<?,?> standTarget) {
                if (!standTarget.isRemote() && standTarget.hasUser()) {
                    target = standTarget.getUserOrThrow();
                }
            }

            // Mark pushed and don't push the same entity more than once
            final int id = target.getId();
            if (pushed.contains(id)) continue;

            if (distance < radius) {
                if (DashData.isDashing(target)) {
                    DashData.getDash(target).finished = true;
                }

                double launchVel = target.onGround() ? 1.5 : 0.75;

                // The closer they are, the harder they're pushed
                if (distance < userWidth * 2.0) {
                    launchVel += (userWidth * 2.0 - distance) / 1.5;
                }

                Vec3 delta = target.position().subtract(position).normalize();

                // The launch should be horizontal from the victims POV
                delta = switch (GravityChangerAPI.getGravityDirection(target).getAxis()) {
                    case X -> new Vec3(0, delta.y, delta.z);
                    case Y -> new Vec3( delta.x, 0, delta.z);
                    case Z -> new Vec3(delta.x, delta.y, 0);
                };

                JComponentPlatformUtils.getShockwaveHandler(world).addShockwave(stand.getEyePosition(), delta, 1.5f, Shockwave.Type.PUSHBLOCK);

                JUtils.addVelocity(target,
                        delta
                        .scale(launchVel)
                );

                pushed.add(id);

                // if (user instanceof ServerPlayer serverPlayer) serverPlayer.sendSystemMessage( Component.literal(launchVel + "; " + id) );
            }
        }
    }

    /**
     * Breaks out of a combo using a slightly delayed attack centered at the player.
     * This attack is blockable, launches and stuns on hit.
     */
    public static void comboBreak(ServerLevel world, LivingEntity player, MobEffectInstance stun) {
        if (stun == null) return;
        if (player.isSpectator()) return;
        CommonCooldownsComponent cooldowns = JComponentPlatformUtils.getCooldowns(player);

        if (stun.getDuration() > 1 && DazedStatusEffect.canBeComboBroken(stun.getAmplifier()) && cooldowns.getCooldown(CooldownType.COMBO_BREAKER) <= 0) {
            cooldowns.startCooldown(CooldownType.COMBO_BREAKER);

            stun(player, 5, 2); // Player is slowed down considerably pre-burst

            world.playSound(null, player, JSoundRegistry.COMBO_BREAK.get(), SoundSource.PLAYERS, 1, 1);

            Vec3 pPos = player.getEyePosition();
            burstTimers.put(player, 4);
            createParticle(world, pPos.x, pPos.y, pPos.z, JParticleType.COMBO_BREAK);
        }
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T extends Entity> T teleportToWorld(T e, ServerLevel w, double x, double y, double z) {
        if (!e.isRemoved()) {
            e.unRide();
            T entity = (T) e.getType().create(w, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (entity != null) {
                entity.restoreFrom(e);
                entity.snapTo(x, y, z, e.getYRot(), e.getXRot());
                entity.setDeltaMovement(e.getDeltaMovement());
                w.addDuringTeleport(entity);
                e.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
                w.resetEmptyTime();
                return entity;
            }
        }
        return null;
    }

    /**
     * @param entity the entity to hop
     * @param heightOffset the height offset
     * @param time the time in the other dimension
     * @throws IllegalArgumentException If <code>time</code> is not positive.
     */
    public static void dimensionHop(final LivingEntity entity, final int heightOffset, final int time) {
        final ServerLevel original = (ServerLevel) entity.level();
        final MinecraftServer server = original.getServer();
        final ServerLevel au = server.getLevel(JDimensionRegistry.AU_DIMENSION_KEY);
        if (au == null) {
            JCraft.LOGGER.fatal("Alternate universe world does not exist!");
            return;
        }
        if (original == au) {
            return;
        }

        final Vec3 pos = entity.position();
        LivingEntity finalEnt = entity;

        if (entity instanceof ServerPlayer player) {
            final ChunkPos chunkPos = ChunkPos.containing(BlockPos.containing(pos.x, pos.y, pos.z));
            au.getChunkSource().addTicketWithRadius(TicketType.PORTAL, chunkPos, 1);
            player.teleportTo(au, pos.x, pos.y - heightOffset, pos.z, java.util.Set.of(), entity.getYRot(), entity.getXRot(), false);
            player.connection.send(
                    new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(JSoundRegistry.D4C_ALT_UNIVERSE_AMBIENCE.get()), SoundSource.MUSIC, pos.x, pos.y - heightOffset, pos.z, 1.0F, 1.0F, 0)
            );
        } else {
            finalEnt = teleportToWorld(entity, au, entity.getX(), entity.getY() - heightOffset, entity.getZ());
        }

        if (finalEnt == null) {
            JCraft.LOGGER.error("Failed to teleport " + entity + " to alternate universe!");
            return;
        }

        finalEnt.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 9, true, false, true));
        PastDimensions.enqueue(new DimensionData(finalEnt, pos, original.dimension(), time)); // throws IAE
    }

    public static boolean wasRecentlyAttacked(CombatTracker tracker) {
        tracker.recheckStatus();
        return tracker.getCombatDuration() > 0;
    }

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, name);
    }

    /**
     * Stuns specified {@link LivingEntity}
     *
     * @param victim    victim to stun
     * @param duration  in ticks
     * @param amplifier level of stun
     */
    public static void stun(LivingEntity victim, int duration, @Range(from = 0, to = 4) int amplifier) {
        stun(victim, duration, amplifier, null);
    }

    /**
     * Stuns specified {@link LivingEntity}
     *
     * @param victim    victim to stun
     * @param duration  in ticks
     * @param amplifier level of stun, see {@link DazedStatusEffect} for specifics
     * @param attacker  cause of stun
     */
    public static void stun(LivingEntity victim, int duration, @Range(from = 0, to = 4) int amplifier, @Nullable Entity attacker) {
        if (victim == null || !victim.isAlive() || duration == 0) {
            return;
        }
//        if (attacker instanceof ServerPlayer serverPlayer) {
//
//        }
        victim.addEffect(new MobEffectInstance(JStatusRegistry.DAZED.asHolder(), duration, amplifier, false, false, true));
        //JCraft.LOGGER.info("Stunned: " + entity.getEntityName() + " for: " + duration + " with stunType: " + amplifier);
    }

    public static void prefixedLog(boolean isClient, String msg) {
        LOGGER.info(isClient ? "[CLIENT]: " + msg : "[SERVER]: " + msg);
    }
}
