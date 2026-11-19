package net.arna.jcraft.common.debug;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFutureListener;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.AttackData;
import net.arna.jcraft.api.Attacks;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.component.living.CommonCooldownsComponent;
import net.arna.jcraft.api.component.living.CommonGunslingerComponent;
import net.arna.jcraft.api.component.living.CommonHitPropertyComponent;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.registry.JBlockRegistry;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.registry.JSpecTypeRegistry;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.registry.JStatusRegistry;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.api.spec.SpecTypeUtil;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.block.CoffinBlock;
import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.container.DiscCaseContainer;
import net.arna.jcraft.common.enchantments.CinderellasKissEnchantment;
import net.arna.jcraft.common.entity.TrainingDummyEntity;
import net.arna.jcraft.common.entity.npc.AyaTsujiEntity;
import net.arna.jcraft.common.entity.npc.TonpettyEntity;
import net.arna.jcraft.common.entity.projectile.BulletProjectile;
import net.arna.jcraft.common.entity.projectile.KnifeProjectile;
import net.arna.jcraft.common.entity.projectile.MatchProjectile;
import net.arna.jcraft.common.entity.projectile.ScalpelProjectile;
import net.arna.jcraft.common.entity.vehicle.RoadRollerEntity;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.item.AnubisItem;
import net.arna.jcraft.common.item.BloodBottleItem;
import net.arna.jcraft.common.item.FVRevolverItem;
import net.arna.jcraft.common.item.MockItem;
import net.arna.jcraft.common.item.Peacemaker;
import net.arna.jcraft.common.item.RewindMockItem;
import net.arna.jcraft.common.item.SpecDiscItem;
import net.arna.jcraft.common.item.StandDiscItem;
import net.arna.jcraft.common.menu.DiscCaseMenu;
import net.arna.jcraft.common.menu.MainMenu;
import net.arna.jcraft.common.minigame.Wager;
import net.arna.jcraft.common.minigame.card.texasholdem.TexasHoldEm;
import net.arna.jcraft.common.spec.AnubisSpec;
import net.arna.jcraft.common.spec.BrawlerSpec;
import net.arna.jcraft.common.spec.HamonSpec;
import net.arna.jcraft.common.spec.RangerSpec;
import net.arna.jcraft.common.spec.VampireSpec;
import net.arna.jcraft.common.system.GunAiming;
import net.arna.jcraft.common.tickable.PeacemakerReload;
import net.arna.jcraft.common.util.CooldownType;
import net.arna.jcraft.common.util.DashData;
import net.arna.jcraft.common.util.JItemData;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Husk;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Live Minecraft 26.3 server runtime self-test covering ALL non-Stand mechanics:
 * Menus, Containers, Anvil/Grindstone/FoodData/ItemUse/Villager mixins, Items, Blocks,
 * All 5 Specs, Status Effects, NPCs, Dash, Minigame NBT, and Commands.
 * Inactive unless JVM property {@code -Djcraft.selftest.nonstand=true} is passed.
 */
public final class NonStandRuntimeDiagnostic {
    public static final boolean ENABLED = Boolean.getBoolean("jcraft.selftest.nonstand");

    private static int phase = 0;
    private static int waitTicks = 0;
    private static boolean finished = false;

    private static ServerLevel world;
    private static ServerPlayer player;
    private static Husk target;
    private static ItemStack reloadingPeacemaker;
    private static int bulletsBeforeReload;

    private static final double ARENA_X = 40.5;
    private static final double ARENA_Y = 120.0;
    private static final double ARENA_Z = 40.5;

    private static final List<String> failures = new ArrayList<>();
    private static int checksPassed = 0;

    private NonStandRuntimeDiagnostic() {}

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
            failures.add("Phase " + phase + " threw exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
            JCraft.LOGGER.error("[NONSTAND-DEBUG] Exception in phase {}", phase, t);
            finish(server);
        }
    }

    private static void assertTrue(final boolean cond, final String msg) {
        if (!cond) {
            failures.add("FAIL: " + msg);
            JCraft.LOGGER.error("[NONSTAND-DEBUG] FAIL: {}", msg);
        } else {
            checksPassed++;
            JCraft.LOGGER.info("[NONSTAND-DEBUG] PASS: {}", msg);
        }
    }

    private static ServerPlayer createDiagPlayer(final MinecraftServer server, final ServerLevel level, final String name) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        ServerPlayer sp = new ServerPlayer(server, level, profile, ClientInformation.createDefault());
        Connection conn = new Connection(PacketFlow.SERVERBOUND);
        sp.connection = new ServerGamePacketListenerImpl(server, conn, sp, CommonListenerCookie.createInitial(profile, false)) {
            @Override
            public void send(Packet<?> packet) {}

            @Override
            public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {}

            @Override
            public boolean hasClientLoaded() {
                return true;
            }
        };
        sp.setGameMode(GameType.SURVIVAL);
        sp.setPos(ARENA_X, ARENA_Y, ARENA_Z);
        sp.setYRot(0f);
        sp.setXRot(0f);
        sp.setOnGround(true);
        return sp;
    }

    private static void resetPlayerState() {
        player.getInventory().clearContent();
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.setAirSupply(player.getMaxAirSupply());
        player.clearFire();
        player.setShiftKeyDown(false);
        player.setOnGround(true);
        player.setPos(ARENA_X, ARENA_Y, ARENA_Z);
        player.setYRot(0f);
        player.setXRot(0f);
        player.setDeltaMovement(Vec3.ZERO);
        player.setGameMode(GameType.SURVIVAL);
        JComponentPlatformUtils.getCooldowns(player).clear();
        GunAiming.set(player, false);
    }

    private static void resetTarget() {
        if (target != null && !target.isRemoved()) {
            target.discard();
        }
        target = new Husk(EntityTypes.HUSK, world);
        target.setNoAi(true);
        target.setPos(ARENA_X, ARENA_Y, ARENA_Z + 1.5);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        target.setHealth(200.0f);
        world.addFreshEntity(target);
    }

    private static void clearItemCooldown(ItemStack stack) {
        player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
    }

    private static void tickSpecTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            JSpec<?, ?> spec = JUtils.getSpec(player);
            if (spec != null) {
                spec.tickSpec();
            }
            if (JComponentPlatformUtils.getCooldowns(player) instanceof net.arna.jcraft.common.component.impl.living.CommonCooldownsComponentImpl cdImpl) {
                cdImpl.tick();
            }
        }
    }

    private static void runPhase(final MinecraftServer server) throws Exception {
        switch (phase) {
            case 0 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 0: Setup Arena & Live ServerPlayer ===");
                world = server.getLevel(Level.OVERWORLD);
                assertTrue(world != null, "Overworld loaded");
                if (world == null) {
                    finish(server);
                    return;
                }
                ChunkPos cp = ChunkPos.containing(BlockPos.containing(ARENA_X, ARENA_Y, ARENA_Z));
                for (int cx = -1; cx <= 1; cx++) {
                    for (int cz = -1; cz <= 1; cz++) {
                        world.setChunkForced(cp.x() + cx, cp.z() + cz, true);
                    }
                }
                for (int dx = -8; dx <= 8; dx++) {
                    for (int dz = -8; dz <= 8; dz++) {
                        world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y - 1, ARENA_Z + dz), Blocks.STONE.defaultBlockState());
                        for (int dy = 0; dy <= 5; dy++) {
                            world.setBlockAndUpdate(BlockPos.containing(ARENA_X + dx, ARENA_Y + dy, ARENA_Z + dz), Blocks.AIR.defaultBlockState());
                        }
                    }
                }
                player = createDiagPlayer(server, world, "JCraftNonStandDiag");
                world.addFreshEntity(player);
                resetTarget();
                assertTrue(player.isAlive() && target.isAlive(), "Diag ServerPlayer and target spawned in Overworld");
                phase = 1;
                waitTicks = 2;
            }

            case 1 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 1: Menus & DiscCaseContainer ===");
                resetPlayerState();

                // MainMenu quickMoveStack must return EMPTY (never null)
                MainMenu mainMenu = new MainMenu(1, player);
                assertTrue(mainMenu.stillValid(player), "MainMenu.stillValid is true");
                ItemStack qmMain = mainMenu.quickMoveStack(player, 0);
                assertTrue(qmMain != null && qmMain.isEmpty(), "MainMenu.quickMoveStack returns ItemStack.EMPTY (no NPE)");

                // DiscCaseMenu + DiscCaseContainer
                ItemStack discCase = new ItemStack(JItemRegistry.DISC_CASE.get());
                ItemStack standDisc = StandDiscItem.createDiscStack(JStandTypeRegistry.STAR_PLATINUM.get(), 0);
                ItemStack specDisc = SpecDiscItem.createDiscStack(JSpecTypeRegistry.BRAWLER.get());
                ItemStack dirt = new ItemStack(Items.DIRT, 5);

                player.getInventory().setItem(0, discCase);
                player.getInventory().setSelectedSlot(0);
                player.getInventory().setItem(1, standDisc);
                player.getInventory().setItem(2, specDisc);
                player.getInventory().setItem(3, dirt);

                DiscCaseMenu caseMenu = new DiscCaseMenu(2, player);
                int caseSlots = caseMenu.getSlotCount();
                assertTrue(caseSlots > 0, "DiscCaseMenu opened with positive slot count (" + caseSlots + ")");
                assertTrue(caseMenu.stillValid(player), "DiscCaseMenu.stillValid true while holding case");
                assertTrue(caseMenu.getSlot(0).mayPlace(standDisc), "DiscCaseSlot accepts StandDisc");
                assertTrue(caseMenu.getSlot(0).mayPlace(specDisc), "DiscCaseSlot accepts SpecDisc");
                assertTrue(!caseMenu.getSlot(0).mayPlace(dirt), "DiscCaseSlot rejects non-disc items (Dirt)");

                // Out-of-bounds quickMoveStack must not throw
                assertTrue(caseMenu.quickMoveStack(player, -1).isEmpty(), "DiscCaseMenu.quickMoveStack(-1) safely returns EMPTY");
                assertTrue(caseMenu.quickMoveStack(player, 999).isEmpty(), "DiscCaseMenu.quickMoveStack(999) safely returns EMPTY");

                // Find hotbar slots in DiscCaseMenu (caseSlots + 27 + hotbarIdx)
                int hotbar0SlotId = caseSlots + 27;
                int hotbar1SlotId = caseSlots + 28;
                int hotbar2SlotId = caseSlots + 29;
                int hotbar3SlotId = caseSlots + 30;

                // Attempting to click/pick up the disc case itself must be blocked
                caseMenu.clicked(hotbar0SlotId, 0, ContainerInput.PICKUP, player);
                assertTrue(caseMenu.getCarried().isEmpty() && player.getInventory().getItem(0) == discCase,
                        "DiscCaseMenu blocks picking up the open Disc Case");

                // Shift-click stand disc and spec disc into case; dirt must stay out
                caseMenu.quickMoveStack(player, hotbar1SlotId);
                caseMenu.quickMoveStack(player, hotbar2SlotId);
                caseMenu.quickMoveStack(player, hotbar3SlotId);
                assertTrue(caseMenu.getSlot(0).getItem().is(JItemRegistry.STAND_DISC.get()), "Quick-moved StandDisc into DiscCase slot 0");
                assertTrue(caseMenu.getSlot(1).getItem().is(JItemRegistry.SPEC_DISC.get()), "Quick-moved SpecDisc into DiscCase slot 1");
                assertTrue(player.getInventory().getItem(3).is(Items.DIRT) && player.getInventory().getItem(3).getCount() == 5,
                        "Non-disc item refused by quickMoveStack into DiscCase");

                // Persistence in DataComponents.CONTAINER
                DiscCaseContainer reopened = DiscCaseContainer.of(discCase);
                assertTrue(reopened.getItem(0).is(JItemRegistry.STAND_DISC.get()) && reopened.getItem(1).is(JItemRegistry.SPEC_DISC.get()),
                        "DiscCaseContainer persists stored discs in DataComponents.CONTAINER");

                // Shift-click back out of case
                caseMenu.quickMoveStack(player, 0);
                assertTrue(caseMenu.getSlot(0).getItem().isEmpty(), "Quick-moved StandDisc back out of DiscCase");

                phase = 2;
                waitTicks = 1;
            }

            case 2 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 2: Cinderella's Kiss, Aya Tsuji, Anvil & Grindstone Mixins ===");
                resetPlayerState();

                Holder<Enchantment> kissHolder = world.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(CinderellasKissEnchantment.KEY);

                // CinderellaMask enchantable component
                ItemStack plainMask = new ItemStack(JItemRegistry.CINDERELLA_MASK.get());
                assertTrue(plainMask.has(DataComponents.ENCHANTABLE), "CinderellaMask has DataComponents.ENCHANTABLE");

                // AyaTsujiEntity trades carry real MC 26.3 DataComponents.ENCHANTMENTS
                AyaTsujiEntity aya = new AyaTsujiEntity(world);
                boolean foundLvl1 = false, foundLvl2 = false, foundLvl3 = false;
                for (MerchantOffer offer : aya.getOffers()) {
                    int lvl = CinderellasKissEnchantment.getCKLevel(offer.getResult());
                    if (lvl == 1) foundLvl1 = true;
                    if (lvl == 2) foundLvl2 = true;
                    if (lvl == 3) foundLvl3 = true;
                }
                assertTrue(foundLvl1 && foundLvl2 && foundLvl3,
                        "AyaTsujiEntity offers carry real MC 26.3 Cinderella's Kiss enchantment levels 1, 2, 3");
                aya.discard();

                // Enchanted Book creation & AnvilMenu: Mask + Book(2) -> Mask(2), then Mask(2) + Book(2) -> Mask(3)
                ItemStack kissBook2 = EnchantmentHelper.createBook(new EnchantmentInstance(kissHolder, 2));
                assertTrue(CinderellasKissEnchantment.getCKLevel(kissBook2) == 2, "EnchantedBook carries Cinderella's Kiss II");

                BlockPos anvilPos = BlockPos.containing(ARENA_X + 2, ARENA_Y, ARENA_Z);
                world.setBlockAndUpdate(anvilPos, Blocks.ANVIL.defaultBlockState());
                AnvilMenu anvil = new AnvilMenu(3, player.getInventory(), ContainerLevelAccess.create(world, anvilPos));
                anvil.getSlot(0).set(plainMask.copy());
                anvil.getSlot(1).set(kissBook2.copy());
                ItemStack maskLvl2 = anvil.getSlot(2).getItem();
                assertTrue(maskLvl2.is(JItemRegistry.CINDERELLA_MASK.get()) && CinderellasKissEnchantment.getCKLevel(maskLvl2) == 2 && anvil.getCost() > 0,
                        "AnvilMenu enchants CinderellaMask with Cinderella's Kiss II (cost=" + anvil.getCost() + ")");

                anvil.getSlot(0).set(maskLvl2.copy());
                anvil.getSlot(1).set(kissBook2.copy());
                ItemStack maskLvl3 = anvil.getSlot(2).getItem();
                assertTrue(CinderellasKissEnchantment.getCKLevel(maskLvl3) == 3,
                        "AnvilMenu combines Cinderella's Kiss II + II -> III");

                // AnvilMenu: StandDisc(SP skin 0) + CinderellaMask(2) -> SP skin 2
                ItemStack spDisc0 = StandDiscItem.createDiscStack(JStandTypeRegistry.STAR_PLATINUM.get(), 0);
                anvil.getSlot(0).set(spDisc0.copy());
                anvil.getSlot(1).set(maskLvl2.copy());
                ItemStack spDisc2 = anvil.getSlot(2).getItem();
                assertTrue(spDisc2.is(JItemRegistry.STAND_DISC.get())
                        && StandDiscItem.getStandType(spDisc2) == JStandTypeRegistry.STAR_PLATINUM.get()
                        && StandDiscItem.getSkin(spDisc2) == 2
                        && JCraft.id("stand_disc_star_platinum_2").equals(spDisc2.get(DataComponents.ITEM_MODEL)),
                        "AnvilMenu switches StandDisc skin 0 -> 2 with synced ITEM_MODEL");

                // Preserve OneUse fragile flag when switching skin back to 0 with unenchanted mask
                ItemStack fragileDisc2 = StandDiscItem.createDiscStack(JStandTypeRegistry.STAR_PLATINUM.get(), 2, true);
                anvil.getSlot(0).set(fragileDisc2);
                anvil.getSlot(1).set(plainMask.copy());
                ItemStack fragileDisc0 = anvil.getSlot(2).getItem();
                assertTrue(StandDiscItem.getSkin(fragileDisc0) == 0 && StandDiscItem.isFragile(fragileDisc0),
                        "AnvilMenu switches skin back to 0 with unenchanted mask and preserves OneUse flag");

                // GrindstoneMenu: wiping filled StandDisc back to empty StandDisc
                BlockPos grindPos = BlockPos.containing(ARENA_X + 3, ARENA_Y, ARENA_Z);
                world.setBlockAndUpdate(grindPos, Blocks.GRINDSTONE.defaultBlockState());
                GrindstoneMenu grind = new GrindstoneMenu(4, player.getInventory(), ContainerLevelAccess.create(world, grindPos));
                assertTrue(grind.getSlot(0).mayPlace(spDisc2), "Grindstone slot 0 accepts filled StandDisc");
                grind.getSlot(0).set(spDisc2.copy());
                assertTrue(!grind.getSlot(1).mayPlace(spDisc0), "Grindstone slot 1 blocks second StandDisc when slot 0 is occupied");
                ItemStack wiped = grind.getSlot(2).getItem();
                assertTrue(wiped.is(JItemRegistry.STAND_DISC.get())
                        && StandDiscItem.isEmptyDisc(wiped)
                        && JCraft.id("stand_disc").equals(wiped.get(DataComponents.ITEM_MODEL)),
                        "GrindstoneMenu erases filled StandDisc to empty StandDisc with synced model");

                world.setBlockAndUpdate(anvilPos, Blocks.AIR.defaultBlockState());
                world.setBlockAndUpdate(grindPos, Blocks.AIR.defaultBlockState());
                phase = 3;
                waitTicks = 1;
            }

            case 3 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 3: StandDiscItem & SpecDiscItem ===");
                resetPlayerState();

                CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
                standComp.setType(JStandTypeRegistry.NONE.get());
                CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);
                specComp.setType(JSpecTypeRegistry.NONE.get());

                // Empty StandDisc on Standless player must FAIL cleanly
                ItemStack emptyStandDisc = new ItemStack(JItemRegistry.STAND_DISC.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, emptyStandDisc);
                InteractionResult emptyRes = emptyStandDisc.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(emptyRes == InteractionResult.FAIL && StandTypeUtil.isNone(standComp.getType()),
                        "Empty StandDisc on Standless player returns FAIL");

                // Filled StandDisc -> equips stand & empties disc
                ItemStack twDisc = StandDiscItem.createDiscStack(JStandTypeRegistry.THE_WORLD.get(), 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, twDisc);
                InteractionResult useTw = twDisc.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(useTw.consumesAction()
                        && standComp.getType() == JStandTypeRegistry.THE_WORLD.get()
                        && standComp.getSkin() == 1
                        && StandDiscItem.isEmptyDisc(twDisc),
                        "StandDisc equips THE_WORLD skin 1 and empties disc");

                // Extract stand back into now-empty disc
                clearItemCooldown(twDisc);
                InteractionResult extractTw = twDisc.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(extractTw.consumesAction()
                        && StandTypeUtil.isNone(standComp.getType())
                        && StandDiscItem.getStandType(twDisc) == JStandTypeRegistry.THE_WORLD.get()
                        && StandDiscItem.getSkin(twDisc) == 1,
                        "Empty StandDisc extracts player's stand+skin back into disc and sets player to NONE");

                // Fragile OneUse disc consumed when Standless, refused when player has a stand
                ItemStack fragile = StandDiscItem.createDiscStack(JStandTypeRegistry.STAR_PLATINUM.get(), 0, true);
                player.setItemInHand(InteractionHand.MAIN_HAND, fragile);
                InteractionResult fragileUse = fragile.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(fragileUse.consumesAction() && fragile.isEmpty() && standComp.getType() == JStandTypeRegistry.STAR_PLATINUM.get(),
                        "Fragile OneUse StandDisc grants stand and consumes itself");

                ItemStack fragile2 = StandDiscItem.createDiscStack(JStandTypeRegistry.THE_WORLD.get(), 0, true);
                player.setItemInHand(InteractionHand.MAIN_HAND, fragile2);
                InteractionResult fragileBlocked = fragile2.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(fragileBlocked == InteractionResult.FAIL && !fragile2.isEmpty(),
                        "Fragile OneUse StandDisc fails when player already has a stand");

                // Desummon & clear stand cleanly
                if (standComp.getStand() != null) {
                    standComp.getStand().discard();
                    standComp.setStand(null);
                }
                standComp.setType(JStandTypeRegistry.NONE.get());

                // SpecDiscItem tests
                ItemStack emptySpecDisc = new ItemStack(JItemRegistry.SPEC_DISC.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, emptySpecDisc);
                assertTrue(emptySpecDisc.use(world, player, InteractionHand.MAIN_HAND) == InteractionResult.FAIL,
                        "Empty SpecDisc on Specless player returns FAIL");

                ItemStack brawlerDisc = SpecDiscItem.createDiscStack(JSpecTypeRegistry.BRAWLER.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, brawlerDisc);
                InteractionResult specUse = brawlerDisc.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(specUse.consumesAction()
                        && specComp.getType() == JSpecTypeRegistry.BRAWLER.get()
                        && SpecDiscItem.isEmptyDisc(brawlerDisc),
                        "SpecDisc equips BRAWLER and empties disc");

                InteractionResult specExtract = brawlerDisc.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(specExtract.consumesAction()
                        && SpecTypeUtil.isNone(specComp.getType())
                        && SpecDiscItem.getSpecType(brawlerDisc) == JSpecTypeRegistry.BRAWLER.get(),
                        "Empty SpecDisc extracts BRAWLER back into disc and sets player spec to NONE");

                phase = 4;
                waitTicks = 1;
            }

            case 4 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 4: Anubis Sheathe/Unsheathe & Spec Obtainment Items ===");
                resetPlayerState();
                CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);
                specComp.setType(JSpecTypeRegistry.NONE.get());

                // SheathedAnubis -> unsheathes into AnubisItem AND gives ANUBIS spec
                ItemStack sheathed = new ItemStack(JItemRegistry.ANUBIS_SHEATHED.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, sheathed);
                InteractionResult unsheatheRes = sheathed.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(unsheatheRes instanceof InteractionResult.Success s
                        && s.heldItemTransformedTo() != null
                        && s.heldItemTransformedTo().is(JItemRegistry.ANUBIS.get())
                        && player.getMainHandItem().is(JItemRegistry.ANUBIS.get())
                        && specComp.getType() == JSpecTypeRegistry.ANUBIS.get(),
                        "SheathedAnubis.use transforms held item to ANUBIS (MC 26.3 heldItemTransformedTo) and grants ANUBIS spec");

                // AnubisItem -> sheathes back into SheathedAnubisItem
                ItemStack unsheathed = player.getMainHandItem();
                InteractionResult sheatheRes = unsheathed.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(sheatheRes instanceof InteractionResult.Success s2
                        && s2.heldItemTransformedTo() != null
                        && s2.heldItemTransformedTo().is(JItemRegistry.ANUBIS_SHEATHED.get())
                        && player.getMainHandItem().is(JItemRegistry.ANUBIS_SHEATHED.get()),
                        "AnubisItem.use transforms held item back to ANUBIS_SHEATHED");

                // BoxingGloves -> Brawler (first click warns because player has Anubis, second click switches!)
                ItemStack gloves = new ItemStack(JItemRegistry.BOXING_GLOVES.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, gloves);
                gloves.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(specComp.getType() == JSpecTypeRegistry.ANUBIS.get(), "SpecObtainmentItem warns before overwriting existing spec");
                gloves.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(specComp.getType() == JSpecTypeRegistry.BRAWLER.get(), "BoxingGloves switches spec to BRAWLER on confirmation");

                // Holster -> Ranger
                specComp.setType(JSpecTypeRegistry.NONE.get());
                ItemStack holster = new ItemStack(JItemRegistry.HOLSTER.get());
                player.setItemInHand(InteractionHand.MAIN_HAND, holster);
                holster.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(specComp.getType() == JSpecTypeRegistry.RANGER.get(), "Holster grants RANGER spec");

                // StoneMask -> Vampire when recently attacked while worn on head
                specComp.setType(JSpecTypeRegistry.NONE.get());
                ItemStack mask = new ItemStack(JItemRegistry.STONE_MASK.get());
                player.setItemSlot(EquipmentSlot.HEAD, mask);
                player.hurtServer(world, world.damageSources().generic(), 1.0f);
                mask.inventoryTick(world, player, EquipmentSlot.HEAD);
                assertTrue(specComp.getType() == JSpecTypeRegistry.VAMPIRE.get(),
                        "StoneMask converts recently-damaged player into VAMPIRE without client dependency");
                player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);

                phase = 5;
                waitTicks = 1;
            }

            case 5 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 5: BloodBottle, VillagerMixin & Vampire FoodDataMixin ===");
                resetPlayerState();
                resetTarget();
                CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);
                specComp.setType(JSpecTypeRegistry.VAMPIRE.get());
                CommonVampireComponent vamp = JComponentPlatformUtils.getVampirism(player);
                vamp.setBlood(10f);

                // FoodDataMixin reflects Vampire blood level
                player.getFoodData().tick(player);
                assertTrue(player.getFoodData().getFoodLevel() == 10 && player.getFoodData().getSaturationLevel() == 0.0f,
                        "FoodDataMixin exposes vampire blood (10) as foodLevel and 0 saturation");

                // BloodBottle extracts blood from valid mob
                ItemStack bottle = JItemRegistry.BLOOD_BOTTLE.get().getDefaultInstance();
                player.setItemInHand(InteractionHand.MAIN_HAND, bottle);
                InteractionResult drawOk = bottle.getItem().interactLivingEntity(bottle, player, target, InteractionHand.MAIN_HAND);
                float bloodAfter = JItemData.getOrCreateTag(bottle).getFloatOr("Blood", 0f);
                assertTrue(drawOk.consumesAction() && bloodAfter > 0f,
                        "BloodBottle extracts blood from living Husk (" + bloodAfter + ")");

                // Invulnerable target must NOT give free blood
                clearItemCooldown(bottle);
                target.setPermanentlyInvulnerable(true);
                bottle.getItem().interactLivingEntity(bottle, player, target, InteractionHand.MAIN_HAND);
                float bloodAfterInvuln = JItemData.getOrCreateTag(bottle).getFloatOr("Blood", 0f);
                assertTrue(bloodAfterInvuln == bloodAfter,
                        "BloodBottle does NOT extract blood from invulnerable target (exploit fixed)");
                target.setPermanentlyInvulnerable(false);

                // VillagerEntityMixin passes through when holding BloodBottle
                Villager villager = new Villager(EntityTypes.VILLAGER, world);
                villager.setPos(ARENA_X + 1.5, ARENA_Y, ARENA_Z);
                world.addFreshEntity(villager);
                InteractionResult vilRes = villager.mobInteract(player, InteractionHand.MAIN_HAND);
                assertTrue(vilRes == InteractionResult.PASS,
                        "VillagerEntityMixin passes mobInteract through when holding BloodBottle");
                villager.discard();

                // Drinking BloodBottle increases Vampire blood & updates FoodData
                JItemData.edit(bottle, t -> t.putFloat("Blood", 8f));
                BloodBottleItem.syncBottleModel(bottle, 8f);
                clearItemCooldown(bottle);
                bottle.finishUsingItem(world, player);
                assertTrue(vamp.getBlood() == 12f && JItemData.getOrCreateTag(bottle).getFloatOr("Blood", 0f) == 7f,
                        "Drinking BloodBottle restores vampire blood (10 -> 12) and drains bottle (8 -> 7)");
                assertTrue(player.getFoodData().getFoodLevel() == 12,
                        "FoodDataMixin reflects updated vampire blood (12)");

                // Creative vampire must NOT ignite in sunlight
                JUtils.setDayTime(world, 6000L);
                player.setGameMode(GameType.CREATIVE);
                player.clearFire();
                ((net.arna.jcraft.common.component.impl.living.CommonVampireComponentImpl) vamp).tick();
                assertTrue(!player.isOnFire(), "Creative vampire does not ignite in sunlight");
                player.setGameMode(GameType.SURVIVAL);

                phase = 6;
                waitTicks = 1;
            }

            case 6 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 6: Peacemaker, FVRevolver, GunAiming & Start Live Reload ===");
                resetPlayerState();

                // Fresh Peacemaker defaults to 6 shots & dedicated-server-safe key tooltips
                ItemStack peacemaker = new ItemStack(JItemRegistry.PEACEMAKER.get());
                assertTrue(Peacemaker.getShots(peacemaker) == Peacemaker.MAX_ROUNDS,
                        "Fresh Peacemaker starts with 6 shots (was 0)");
                assertTrue(Peacemaker.getFireKey() != null && Peacemaker.getAimKey() != null && Peacemaker.getReloadKey() != null,
                        "Peacemaker keybind tooltip helpers safe on dedicated server");

                player.setItemInHand(InteractionHand.MAIN_HAND, peacemaker);
                // Aiming applies modifier; unequipping + server tick clears it automatically
                GunAiming.set(player, true);
                assertTrue(GunAiming.isAiming(player), "GunAiming activates while holding Peacemaker");

                // Cock then fire
                Peacemaker.handleLeftClick(player);
                assertTrue(JItemData.getOrCreateTag(peacemaker).getBooleanOr(Peacemaker.COCKED_ID, false), "Peacemaker cocked on 1st click");
                clearItemCooldown(peacemaker);
                int bulletsInWorldBefore = world.getEntitiesOfClass(BulletProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                Peacemaker.handleLeftClick(player);
                int bulletsInWorldAfter = world.getEntitiesOfClass(BulletProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(bulletsInWorldAfter == bulletsInWorldBefore + 1 && Peacemaker.getShots(peacemaker) == 5,
                        "Peacemaker fires BulletProjectile and decrements shots 6 -> 5");

                // Put BulletItem.getDefaultInstance() (has Caliber: 9 NBT!) in inventory & start live reload
                ItemStack ammo = JItemRegistry.BULLET.get().getDefaultInstance();
                ammo.setCount(4);
                player.getInventory().setItem(5, ammo);
                bulletsBeforeReload = ammo.getCount();
                assertTrue(Peacemaker.canLoadMore(peacemaker, player),
                        "Peacemaker.canLoadMore recognizes BulletItem with Caliber NBT in MC 26.3");

                clearItemCooldown(peacemaker);
                Peacemaker.handleReloadInput(player);
                assertTrue(Peacemaker.isReloading(peacemaker) && PeacemakerReload.isReloading(peacemaker),
                        "PeacemakerReload started live reload sequence");
                reloadingPeacemaker = peacemaker;

                // FVRevolverItem fresh default shots & fire
                ItemStack fv = new ItemStack(JItemRegistry.FV_REVOLVER.get());
                assertTrue(FVRevolverItem.getShots(fv) == 6, "Fresh FVRevolverItem has 6 shots");
                player.setItemInHand(InteractionHand.OFF_HAND, fv);
                int fvBulletsBefore = world.getEntitiesOfClass(BulletProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                InteractionResult fvRes = fv.use(world, player, InteractionHand.OFF_HAND);
                int fvBulletsAfter = world.getEntitiesOfClass(BulletProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(fvRes.consumesAction() && fvBulletsAfter == fvBulletsBefore + 1 && FVRevolverItem.getShots(fv) == 5,
                        "FVRevolverItem fires BulletProjectile and decrements shots 6 -> 5");
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);

                // Wait 45 real server ticks for 1-round reload (START 10 + EJECT 8 + FEED 2 + CYCLE 2 + END 18 = 40 ticks)
                phase = 7;
                waitTicks = 45;
            }

            case 7 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 7: Verify Live PeacemakerReload Completion & GunAiming Cleanup ===");
                assertTrue(!Peacemaker.isReloading(reloadingPeacemaker) && !PeacemakerReload.isReloading(reloadingPeacemaker),
                        "PeacemakerReload completed live reload cleanly");
                assertTrue(Peacemaker.getShots(reloadingPeacemaker) == 6,
                        "Peacemaker reloaded 5 -> 6 rounds on live server ticks");
                int bulletsNow = player.getInventory().getItem(5).getCount();
                assertTrue(bulletsNow == bulletsBeforeReload - 1,
                        "PeacemakerReload consumed exactly 1 BulletItem (" + bulletsBeforeReload + " -> " + bulletsNow + ")");

                // Switch off Peacemaker and let serverPostTick clear GunAiming automatically
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                phase = 8;
                waitTicks = 2;
            }

            case 8 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 8: Throwables, ItemUseMixin, Vehicle, Dummy, Coffin, Mock, Arrow ===");
                assertTrue(!GunAiming.isAiming(player), "Server tick automatically cleared GunAiming when Peacemaker unequipped");
                resetPlayerState();
                resetTarget();

                // KnifeItem, KnifeBundleItem, ScalpelItem, MatchboxItem
                ItemStack knife = new ItemStack(JItemRegistry.KNIFE.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, knife);
                player.startUsingItem(InteractionHand.MAIN_HAND);
                int kBefore = world.getEntitiesOfClass(KnifeProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                knife.releaseUsing(world, player, 7180);
                int kAfter = world.getEntitiesOfClass(KnifeProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(kAfter == kBefore + 1 && knife.getCount() == 1, "KnifeItem throws KnifeProjectile and consumes 1");

                ItemStack bundle = new ItemStack(JItemRegistry.KNIFEBUNDLE.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, bundle);
                player.startUsingItem(InteractionHand.MAIN_HAND);
                bundle.releaseUsing(world, player, 7170);
                int kBundleAfter = world.getEntitiesOfClass(KnifeProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(kBundleAfter == kAfter + 9 && bundle.getCount() == 1, "KnifeBundleItem throws 9 KnifeProjectiles");

                ItemStack scalpel = new ItemStack(JItemRegistry.SCALPEL.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, scalpel);
                player.startUsingItem(InteractionHand.MAIN_HAND);
                int sBefore = world.getEntitiesOfClass(ScalpelProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                scalpel.releaseUsing(world, player, 7180);
                int sAfter = world.getEntitiesOfClass(ScalpelProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(sAfter == sBefore + 1 && scalpel.getCount() == 1, "ScalpelItem throws ScalpelProjectile");

                ItemStack matchbox = new ItemStack(JItemRegistry.MATCHBOX.get(), 2);
                player.setItemInHand(InteractionHand.MAIN_HAND, matchbox);
                int mBefore = world.getEntitiesOfClass(MatchProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                matchbox.use(world, player, InteractionHand.MAIN_HAND);
                int mAfter = world.getEntitiesOfClass(MatchProjectile.class, AABB.ofSize(player.position(), 32, 32, 32)).size();
                assertTrue(mAfter == mBefore + 8 && matchbox.getCount() == 1, "MatchboxItem spawns 8 MatchProjectiles");

                // ItemUseMixin blocks item use when DAZED
                player.addEffect(new MobEffectInstance(JStatusRegistry.DAZED.asHolder(), 40, 1));
                clearItemCooldown(matchbox);
                InteractionResult dazedUse = matchbox.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(dazedUse == InteractionResult.FAIL, "ItemUseMixin blocks item use while DAZED");
                player.removeEffect(JStatusRegistry.DAZED.asHolder());

                // VehicleItem: sets YRot (yaw), preserves stack in creative, consumes in survival
                player.setYRot(135.0f);
                player.setXRot(45.0f);
                ItemStack rr = new ItemStack(JItemRegistry.ROAD_ROLLER.get(), 2);
                player.setGameMode(GameType.CREATIVE);
                player.setItemInHand(InteractionHand.MAIN_HAND, rr);
                rr.use(world, player, InteractionHand.MAIN_HAND);
                List<RoadRollerEntity> rollers = world.getEntitiesOfClass(RoadRollerEntity.class, AABB.ofSize(player.position(), 16, 16, 16));
                assertTrue(!rollers.isEmpty() && Math.abs(rollers.get(0).getYRot() - 135.0f) < 1.0f && rr.getCount() == 2,
                        "VehicleItem sets yaw (YRot=135) and does not shrink stack in creative");
                rollers.forEach(Entity::discard);

                player.setGameMode(GameType.SURVIVAL);
                clearItemCooldown(rr);
                rr.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(rr.getCount() == 1, "VehicleItem consumes 1 stack in survival");
                world.getEntitiesOfClass(RoadRollerEntity.class, AABB.ofSize(player.position(), 16, 16, 16)).forEach(Entity::discard);

                // TrainingDummy: survives 100-damage JCraft attack without losing HP or dying!
                TrainingDummyEntity dummy = new TrainingDummyEntity(world, ARENA_X + 2, ARENA_Y, ARENA_Z);
                world.addFreshEntity(dummy);
                Attacks.damageLogic(world, dummy, new AttackData(
                        Vec3.ZERO, 5, 1, false, 100.0f, false, 0,
                        world.damageSources().playerAttack(player), player,
                        CommonHitPropertyComponent.HitAnimation.MID, null, false, false, false));
                assertTrue(dummy.isAlive() && dummy.getHealth() == dummy.getMaxHealth(),
                        "TrainingDummyEntity handles 100-damage JCraft hit without losing health or dying");

                // Fill all 36 main inventory slots EXCEPT hand empty -> wait: pickup requires both hands empty,
                // so fill slots 1..35 + put player selected slot 0 empty, Wait: if slot 0 is empty, add() puts into slot 0!
                // Fill slots 1..35 AND verify inventory.add returns true into slot 0, then verify full-case via direct check
                // Actually: test exact full-inventory drop by keeping slot 0 empty? Wait: mobInteract checks mainHand.isEmpty() && offHand.isEmpty().
                // Fill all 36 slots with Cobblestone EXCEPT hold empty offhand? No, slot 0 IS mainhand! So mainhand empty means slot 0 is free.
                // Verify normal pickup adds to slot 0 and discards dummy:
                player.getInventory().clearContent();
                player.getInventory().setSelectedSlot(0);
                InteractionResult pickRes = dummy.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO);
                assertTrue(pickRes.consumesAction() && dummy.isRemoved()
                        && player.getInventory().contains(new ItemStack(JItemRegistry.TRAINING_DUMMY.get())),
                        "TrainingDummyEntity picked up cleanly into inventory with empty hands");

                // CoffinBlock drops: breaking 2 coffins in a row must yield 2 distinct count-1 drops even after 1st is shrunk to 0
                BlockState footState = JBlockRegistry.COFFIN_BLOCK.get().defaultBlockState().setValue(BedBlock.PART, BedPart.FOOT);
                LootParams.Builder lootBuilder = new LootParams.Builder(world)
                        .withParameter(LootContextParams.ORIGIN, player.position())
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);
                List<ItemStack> drop1 = footState.getDrops(lootBuilder);
                assertTrue(drop1.size() == 1 && drop1.get(0).is(JItemRegistry.COFFIN_BLOCK.get()) && drop1.get(0).getCount() == 1,
                        "CoffinBlock 1st break drops 1 Coffin");
                drop1.get(0).shrink(1); // Simulate player picking up the 1st dropped coffin!
                List<ItemStack> drop2 = footState.getDrops(lootBuilder);
                assertTrue(drop2.size() == 1 && drop2.get(0).is(JItemRegistry.COFFIN_BLOCK.get()) && drop2.get(0).getCount() == 1,
                        "CoffinBlock 2nd break STILL drops 1 Coffin after 1st drop was consumed");

                // MockItem & RewindMockItem safety
                ItemStack badMock = new ItemStack(JItemRegistry.AU_MOCK_ITEM.get());
                JItemData.edit(badMock, t -> t.putString("MockItem", "INVALID:NAMESPACE::BAD!!!"));
                assertTrue(MockItem.getMockedStack(badMock).is(Items.DIRT),
                        "MockItem.getMockedStack safely returns fallback on malformed Identifier");

                ItemStack badRewind = new ItemStack(JItemRegistry.REWIND_MOCK_ITEM.get());
                JItemData.edit(badRewind, t -> {
                    t.putInt("RewindRun", 0);
                });
                assertTrue(RewindMockItem.resolveMockStack(badRewind).isEmpty(),
                        "RewindMockItem.resolveMockStack safely handles malformed/zero-run tag");

                // StandArrowItem: survival player gets a regular stand safely
                CommonStandComponent sc = JComponentPlatformUtils.getStandComponent(player);
                sc.setType(JStandTypeRegistry.NONE.get());
                ItemStack arrow = new ItemStack(JItemRegistry.STAND_ARROW.get(), 1);
                player.setItemInHand(InteractionHand.MAIN_HAND, arrow);
                arrow.use(world, player, InteractionHand.MAIN_HAND);
                assertTrue(!StandTypeUtil.isNone(sc.getType()) && arrow.isEmpty(),
                        "StandArrowItem grants regular stand (" + sc.getType().getId() + ") and consumes arrow");
                if (sc.getStand() != null) {
                    sc.getStand().discard();
                    sc.setStand(null);
                }
                sc.setType(JStandTypeRegistry.NONE.get());

                // Cleanup projectiles
                world.getEntitiesOfClass(KnifeProjectile.class, AABB.ofSize(player.position(), 64, 64, 64)).forEach(Entity::discard);
                world.getEntitiesOfClass(ScalpelProjectile.class, AABB.ofSize(player.position(), 64, 64, 64)).forEach(Entity::discard);
                world.getEntitiesOfClass(MatchProjectile.class, AABB.ofSize(player.position(), 64, 64, 64)).forEach(Entity::discard);

                phase = 9;
                waitTicks = 1;
            }

            case 9 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 9: All 5 Fighting Styles / Specs Live Combat & Systems ===");
                resetPlayerState();
                resetTarget();
                CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);

                // 1. BrawlerSpec
                specComp.setType(JSpecTypeRegistry.BRAWLER.get());
                assertTrue(JUtils.getSpec(player) instanceof BrawlerSpec, "BrawlerSpec instantiated");
                float hp0 = target.getHealth();
                assertTrue(JUtils.getSpec(player).initMove(MoveClass.SPECIAL1), "BrawlerSpec SPECIAL1 (Low Kick) initiated");
                tickSpecTicks(14);
                assertTrue(target.getHealth() < hp0, "BrawlerSpec Low Kick landed live damage (" + hp0 + " -> " + target.getHealth() + ")");

                // 2. AnubisSpec
                resetTarget();
                specComp.setType(JSpecTypeRegistry.ANUBIS.get());
                AnubisSpec anubis = (AnubisSpec) JUtils.getSpec(player);
                assertTrue(anubis != null, "AnubisSpec instantiated");
                // Low kick while on cooldown must NOT apply slowness
                player.removeAllEffects();
                JComponentPlatformUtils.getCooldowns(player).setCooldown(CooldownType.SPECIAL3, 40);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JItemRegistry.ANUBIS_SHEATHED.get()));
                boolean blockedKick = anubis.initMove(MoveClass.SPECIAL3);
                assertTrue(!blockedKick && !player.hasEffect(MobEffects.SLOWNESS),
                        "AnubisSpec failed LOW_KICK does NOT apply SLOWNESS");
                JComponentPlatformUtils.getCooldowns(player).clear();
                // Unsheathing attack switches sheathed sword to unsheathed sword and hits target
                float hpAnubis = target.getHealth();
                assertTrue(anubis.initMove(MoveClass.SPECIAL1), "AnubisSpec SPECIAL1 (Unsheathing Attack) initiated");
                tickSpecTicks(15);
                assertTrue(player.getMainHandItem().is(JItemRegistry.ANUBIS.get()) && target.getHealth() < hpAnubis,
                        "AnubisSpec Unsheathing Attack unsheathed blade and damaged target");
                // Bloodlust clamp test: increment never exceeds 2.0f and decay never drops below 1.0f
                for (int i = 0; i < 10; i++) {
                    anubis.tryIncrementBloodlust(java.util.Set.of(target));
                }
                assertTrue(anubis.getAttackSpeedMult() <= AnubisSpec.BLOODLUST_MAX + 0.0001f,
                        "AnubisSpec Bloodlust capped at <= 2.0f (" + anubis.getAttackSpeedMult() + ")");
                // Force 1.1f scenario via use decrements and let decay run
                while (anubis.getAttackSpeedMult() > 1.15f) {
                    JComponentPlatformUtils.getCooldowns(player).clear();
                    anubis.setMoveStun(0);
                    anubis.initMove(MoveClass.HEAVY);
                }
                for (int t = 0; t < 100; t++) {
                    anubis.setMoveStun(0);
                    anubis.tickSpec();
                }
                assertTrue(anubis.getAttackSpeedMult() >= 1.0f,
                        "AnubisSpec Bloodlust decay clamped at >= 1.0f (" + anubis.getAttackSpeedMult() + ")");

                // 3. HamonSpec
                resetTarget();
                specComp.setType(JSpecTypeRegistry.HAMON.get());
                HamonSpec hamon = (HamonSpec) JUtils.getSpec(player);
                assertTrue(hamon != null, "HamonSpec instantiated");
                hamon.unlock();
                tickSpecTicks(25);
                assertTrue(hamon.getCharge() > 0f, "HamonSpec charges passively (" + hamon.getCharge() + ")");
                float hpHamon = target.getHealth();
                assertTrue(hamon.initMove(MoveClass.HEAVY), "HamonSpec HEAVY (Focus Strike) initiated");
                tickSpecTicks(16);
                assertTrue(target.getHealth() < hpHamon, "HamonSpec Focus Strike damaged target");
                hamon.setUseHamonNext(true);
                hamon.reset();
                assertTrue(hamon.getCharge() == 0f && !hamon.willUseHamonNext(),
                        "HamonSpec.reset() clears both internal charge and useHamonNext");

                // 4. RangerSpec + Enchanted Holstered Item NBT Round-Trip
                resetTarget();
                specComp.setType(JSpecTypeRegistry.RANGER.get());
                RangerSpec ranger = (RangerSpec) JUtils.getSpec(player);
                assertTrue(ranger != null, "RangerSpec instantiated");
                Holder<Enchantment> sharp = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
                ItemStack enchSword = new ItemStack(Items.DIAMOND_SWORD);
                enchSword.enchant(sharp, 5);
                player.setItemInHand(InteractionHand.MAIN_HAND, enchSword);
                assertTrue(ranger.initMove(MoveClass.SPECIAL3), "RangerSpec HOLSTER move initiated");
                CommonGunslingerComponent gunslinger = JComponentPlatformUtils.getGunslinger(player);
                assertTrue(gunslinger.getHolsteredItem().is(Items.DIAMOND_SWORD)
                        && EnchantmentHelper.getItemEnchantmentLevel(sharp, gunslinger.getHolsteredItem()) == 5,
                        "RangerSpec holstered enchanted Diamond Sword (Sharpness V)");

                // NBT round-trip test for CommonGunslingerComponentImpl
                CompoundTag gunTag = new CompoundTag();
                ((net.arna.jcraft.common.component.impl.living.CommonGunslingerComponentImpl) gunslinger).writeToNbt(gunTag);
                gunslinger.setHolsteredItem(ItemStack.EMPTY);
                ((net.arna.jcraft.common.component.impl.living.CommonGunslingerComponentImpl) gunslinger).readFromNbt(gunTag);
                assertTrue(gunslinger.getHolsteredItem().is(Items.DIAMOND_SWORD)
                        && EnchantmentHelper.getItemEnchantmentLevel(sharp, gunslinger.getHolsteredItem()) == 5,
                        "CommonGunslingerComponentImpl preserves enchanted holstered item across NBT save/load");

                // Focus active cleared when switching spec away from Ranger
                gunslinger.setFocusActive(true);
                specComp.setType(JSpecTypeRegistry.VAMPIRE.get());
                assertTrue(!gunslinger.isFocusActive(), "Switching away from RangerSpec automatically clears focusActive");

                // 5. VampireSpec
                resetTarget();
                VampireSpec vampireSpec = (VampireSpec) JUtils.getSpec(player);
                assertTrue(vampireSpec != null, "VampireSpec instantiated");
                float hpVamp = target.getHealth();
                assertTrue(vampireSpec.initMove(MoveClass.HEAVY), "VampireSpec HEAVY (Wheel Kick) initiated");
                tickSpecTicks(18);
                assertTrue(target.getHealth() < hpVamp, "VampireSpec Wheel Kick damaged target");

                phase = 10;
                waitTicks = 1;
            }

            case 10 -> {
                JCraft.LOGGER.info("[NONSTAND-DEBUG] === Phase 10: Tonpetty, Status Effects, Dash, Wager NBT & Commands ===");
                resetPlayerState();
                resetTarget();
                CommonSpecComponent specComp = JComponentPlatformUtils.getSpecData(player);

                // TonpettyEntity: Vampire taking Hamon initiation takes 200 damage (NOT 1!)
                TonpettyEntity tonpetty = new TonpettyEntity(world);
                tonpetty.setPos(ARENA_X + 2, ARENA_Y, ARENA_Z);
                world.addFreshEntity(tonpetty);

                ServerPlayer vampVictim = createDiagPlayer(server, world, "VampTonpettyTest");
                world.addFreshEntity(vampVictim);
                JComponentPlatformUtils.getSpecData(vampVictim).setType(JSpecTypeRegistry.VAMPIRE.get());
                // 1st interact warns, 2nd interact initiates & deals 200 dmg!
                tonpetty.interact(vampVictim, InteractionHand.MAIN_HAND, Vec3.ZERO);
                tonpetty.interact(vampVictim, InteractionHand.MAIN_HAND, Vec3.ZERO);
                assertTrue(!vampVictim.isAlive() && JComponentPlatformUtils.getSpecData(vampVictim).getType() == JSpecTypeRegistry.VAMPIRE.get(),
                        "Tonpetty kills Vampire with 200 damage (buggy RegistrySupplier== fixed) and does not grant Hamon to dead player");
                vampVictim.discard();

                // Non-vampire gets Hamon from Tonpetty
                specComp.setType(JSpecTypeRegistry.NONE.get());
                tonpetty.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO); // greeting
                tonpetty.interact(player, InteractionHand.MAIN_HAND, Vec3.ZERO); // initiation
                assertTrue(player.isAlive() && specComp.getType() == JSpecTypeRegistry.HAMON.get(),
                        "Tonpetty initiates non-vampire player into HAMON safely");
                tonpetty.discard();

                // Status Effects verification
                // KNOCKDOWN freezes movement speed to 0.0 (was 100% speed due to 0.0 multiplier bug!)
                target.removeAllEffects();
                double baseSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
                target.addEffect(new MobEffectInstance(JStatusRegistry.KNOCKDOWN.asHolder(), 40, 0));
                double kdSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
                assertTrue(baseSpeed > 0.05 && kdSpeed < 0.0001,
                        "KNOCKDOWN zeroes MOVEMENT_SPEED (" + baseSpeed + " -> " + kdSpeed + ")");
                target.removeEffect(JStatusRegistry.KNOCKDOWN.asHolder());

                // DAZED zeroes MOVEMENT_SPEED and cleanly restores on removal
                target.addEffect(new MobEffectInstance(JStatusRegistry.DAZED.asHolder(), 40, 1));
                double dazedSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
                assertTrue(dazedSpeed < 0.0001, "DAZED zeroes MOVEMENT_SPEED (" + dazedSpeed + ")");
                target.removeEffect(JStatusRegistry.DAZED.asHolder());
                assertTrue(Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 0.001,
                        "DAZED cleanly restores MOVEMENT_SPEED on removal");

                // HYPOXIA amp 2 applies real MC 26.3 attribute debuffs AND clamps airSupply >= -20
                target.addEffect(new MobEffectInstance(JStatusRegistry.HYPOXIA.asHolder(), 40, 2));
                double hypoxiaSpeed = target.getAttributeValue(Attributes.MOVEMENT_SPEED);
                assertTrue(hypoxiaSpeed < baseSpeed * 0.6,
                        "HYPOXIA amp 2 applies real MOVEMENT_SPEED debuff (" + baseSpeed + " -> " + hypoxiaSpeed + ")");
                target.removeEffect(JStatusRegistry.HYPOXIA.asHolder());
                assertTrue(Math.abs(target.getAttributeValue(Attributes.MOVEMENT_SPEED) - baseSpeed) < 0.001,
                        "HYPOXIA cleanly restores MOVEMENT_SPEED on removal");
                target.setAirSupply(0);
                JStatusRegistry.HYPOXIA.get().applyEffectTick(world, target, 2);
                JStatusRegistry.HYPOXIA.get().applyEffectTick(world, target, 2);
                JStatusRegistry.HYPOXIA.get().applyEffectTick(world, target, 2);
                assertTrue(target.getAirSupply() >= -20,
                        "HYPOXIA clamps airSupply >= -20 (actual=" + target.getAirSupply() + ")");

                // CombatTracker / wasRecentlyAttacked clears properly after leaving combat (no permanent disc lockout!)
                player.tickCount = 500;
                player.getCombatTracker().recordDamage(world.damageSources().mobAttack(target), 1.0f);
                assertTrue(JCraft.wasRecentlyAttacked(player.getCombatTracker()),
                        "wasRecentlyAttacked returns true immediately during combat");
                player.tickCount = 900; // > 300 ticks elapsed -> leaves combat
                assertTrue(!JCraft.wasRecentlyAttacked(player.getCombatTracker()),
                        "wasRecentlyAttacked returns false after leaving combat (no permanent Disc lockout!)");

                // SPECLESS blocks spec.canAttack()
                specComp.setType(JSpecTypeRegistry.BRAWLER.get());
                player.addEffect(new MobEffectInstance(JStatusRegistry.SPECLESS.asHolder(), 40, 0));
                assertTrue(!JUtils.getSpec(player).canAttack(), "SPECLESS blocks JSpec.canAttack()");
                player.removeEffect(JStatusRegistry.SPECLESS.asHolder());

                // STANDLESS desummons active stand
                CommonStandComponent standComp = JComponentPlatformUtils.getStandComponent(player);
                standComp.setType(JStandTypeRegistry.STAR_PLATINUM.get());
                JCraft.summon(world, player);
                assertTrue(standComp.getStand() != null, "Stand summoned before STANDLESS test");
                JStatusRegistry.STANDLESS.get().applyEffectTick(world, player, 0);
                assertTrue(standComp.getStand() == null || standComp.getStand().isRemoved(),
                        "STANDLESS desummons active Stand");
                standComp.setType(JStandTypeRegistry.NONE.get());

                // BLEEDING & PURPLE_INFECTION & FLAMMABLE ticks deal damage cleanly
                resetTarget();
                float preDot = target.getHealth();
                JStatusRegistry.BLEEDING.get().applyEffectTick(world, target, 0);
                target.invulnerableTime = 0;
                JStatusRegistry.PHPOISON.get().applyEffectTick(world, target, 0);
                target.invulnerableTime = 0;
                target.igniteForSeconds(5);
                JStatusRegistry.FLAMMABLE.get().applyEffectTick(world, target, 0);
                assertTrue(target.getHealth() < preDot, "BLEEDING/PURPLE_INFECTION/FLAMMABLE deal damage cleanly");

                // DashData
                player.setOnGround(true);
                JComponentPlatformUtils.getCooldowns(player).clear();
                DashData.tryDash(1, 0, player);
                assertTrue(DashData.isDashing(player)
                        && JComponentPlatformUtils.getCooldowns(player).getCooldown(CooldownType.DASH) > 0,
                        "DashData.tryDash starts dash and sets DASH cooldown");

                // TexasHoldEm Wager NBT with enchanted item (RegistryOps fix!)
                Holder<Enchantment> unbreaking = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
                ItemStack betItem = new ItemStack(Items.NETHERITE_SWORD);
                betItem.enchant(unbreaking, 3);
                Wager wagerOut = new Wager();
                wagerOut.increaseWager(betItem);
                CompoundTag wagerTag = new CompoundTag();
                var regOps = world.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                wagerOut.writeToNbt(wagerTag, regOps);
                Wager wagerIn = new Wager();
                wagerIn.readFromNbt(wagerTag, regOps);
                assertTrue(wagerOut.equals(wagerIn),
                        "TexasHoldEm Wager preserves enchanted Netherite Sword across RegistryOps NBT round-trip");

                // Commands via Brigadier dispatcher (console source with OP level)
                CommandSourceStack opSource = server.createCommandSourceStack();
                var disp = server.getCommands().getDispatcher();
                String targetSel = player.getStringUUID();

                int c1 = disp.execute("spec set " + targetSel + " hamon", opSource);
                assertTrue(c1 == 1 && specComp.getType() == JSpecTypeRegistry.HAMON.get(), "/spec set hamon succeeded");
                int c2 = disp.execute("spec unlock " + targetSel, opSource);
                assertTrue(c2 == 1, "/spec unlock succeeded");
                int c3 = disp.execute("spec reset " + targetSel, opSource);
                assertTrue(c3 == 1, "/spec reset succeeded");
                int c4 = disp.execute("spec clear " + targetSel, opSource);
                assertTrue(c4 == 1 && specComp.getType() == JSpecTypeRegistry.NONE.get(),
                        "/spec clear sets spec cleanly to NONE (not null)");

                // /stand set random with EXCLUSIVE_STANDS enabled must NOT NPE!
                boolean prevExcl = JServerConfig.EXCLUSIVE_STANDS.getValue();
                JServerConfig.EXCLUSIVE_STANDS.setValue(true);
                try {
                    int c5 = disp.execute("stand set " + targetSel + " random", opSource);
                    assertTrue(c5 == 1 && !StandTypeUtil.isNone(standComp.getType()),
                            "/stand set random succeeds with EXCLUSIVE_STANDS=true (no NPE)");
                } finally {
                    JServerConfig.EXCLUSIVE_STANDS.setValue(prevExcl);
                }

                // /stand skin works even while stand is UNSUMMONED
                if (standComp.getStand() != null) {
                    standComp.getStand().discard();
                    standComp.setStand(null);
                }
                standComp.setTypeAndSkin(JStandTypeRegistry.STAR_PLATINUM.get(), 0, false);
                int c6 = disp.execute("stand skin " + targetSel + " 2", opSource);
                assertTrue(c6 == 1 && standComp.getSkin() == 2,
                        "/stand skin updates skin even while Stand is unsummoned");

                int c7 = disp.execute("stand clear " + targetSel, opSource);
                assertTrue(c7 == 1 && StandTypeUtil.isNone(standComp.getType()),
                        "/stand clear resets stand cleanly to NONE");

                int c8 = disp.execute("jgravity set up " + targetSel, opSource);
                assertTrue(c8 == 1 && GravityChangerAPI.getGravityDirection(player) == Direction.UP,
                        "/jgravity set up works");
                int c9 = disp.execute("jgravity cleargravity " + targetSel, opSource);
                assertTrue(c9 == 1, "/jgravity cleargravity works with entities argument");
                GravityChangerAPI.setDefaultGravityDirection(player, Direction.DOWN);

                JComponentPlatformUtils.getCooldowns(player).setCooldown(CooldownType.DASH, 100);
                int c10 = disp.execute("cdc " + targetSel, opSource);
                assertTrue(c10 == 1 && JComponentPlatformUtils.getCooldowns(player).getCooldown(CooldownType.DASH) == 0,
                        "/cdc clears cooldowns");

                int c11 = disp.execute("jcraft help", opSource);
                assertTrue(c11 == 1, "/jcraft help executes cleanly");

                finish(server);
            }
        }
    }

    private static void finish(final MinecraftServer server) {
        finished = true;
        if (target != null && !target.isRemoved()) target.discard();
        if (player != null && !player.isRemoved()) player.discard();

        JCraft.LOGGER.info("========================================================");
        JCraft.LOGGER.info("[NONSTAND-DEBUG] SUMMARY: PASSED={} FAILED={}", checksPassed, failures.size());
        for (String f : failures) {
            JCraft.LOGGER.error("[NONSTAND-DEBUG] {}", f);
        }
        if (failures.isEmpty()) {
            JCraft.LOGGER.info("[NONSTAND-DEBUG] ALL_NONSTAND_CHECKS_PASSED");
        } else {
            JCraft.LOGGER.error("[NONSTAND-DEBUG] NONSTAND_CHECKS_HAD_FAILURES");
        }
        JCraft.LOGGER.info("========================================================");
        server.halt(false);
    }
}
