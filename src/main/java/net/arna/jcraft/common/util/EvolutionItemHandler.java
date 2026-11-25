package net.arna.jcraft.common.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.InteractionEvent;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.component.living.CommonStandComponent;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.saveddata.ExclusiveStandsData;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.arna.jcraft.api.registry.JStatRegistry;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class EvolutionItemHandler {
    private static final List<String> BUNDLED_EVOLUTIONS = List.of(
            "c_moon_to_made_in_heaven",
            "gold_experience_to_gold_experience_requiem",
            "killer_queen_to_killer_queen_bites_the_dust",
            "shadow_the_world_to_the_world",
            "silver_chariot_to_chariot_requiem",
            "star_platinum_to_star_platinum_the_world",
            "the_world_to_the_world_over_heaven",
            "white_snake_to_c_moon"
    );

    private static final Multimap<Item, Evolution> evolutions = MultimapBuilder.hashKeys().arrayListValues().build();
    private static boolean serverLoaded = false;

    static {
        InteractionEvent.RIGHT_CLICK_ITEM.register(EvolutionItemHandler::onItemUse);
    }

    public static void init() {}

    private static synchronized void ensureBootstrapLoaded() {
        if (serverLoaded || !evolutions.isEmpty()) {
            return;
        }
        Gson gson = new Gson();
        for (String name : BUNDLED_EVOLUTIONS) {
            String path = "/data/jcraft/evolutions/" + name + ".json";
            try (var stream = EvolutionItemHandler.class.getResourceAsStream(path)) {
                if (stream == null) {
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    JsonObject obj = gson.fromJson(reader, JsonObject.class);
                    Evolution.CODEC.parse(JsonOps.INSTANCE, obj)
                            .result()
                            .ifPresent(evo -> evolutions.put(evo.item(), evo));
                }
            } catch (Exception e) {
                JCraft.LOGGER.warn("Failed to bootstrap evolution {}: {}", name, e.getMessage());
            }
        }
    }

    public static List<Evolution> getEvolutions() {
        ensureBootstrapLoaded();
        return ImmutableList.copyOf(evolutions.values());
    }

    public static List<Evolution> getEvolutionsFor(@Nullable StandType current) {
        return getEvolutions().stream()
                .filter(e -> e.stand() == null || e.stand() == current)
                .toList();
    }


    private static EventResult onItemUse(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) return EventResult.pass();

        ItemStack handStack = player.getItemInHand(hand);
        Item item = handStack.getItem();
        final ExclusiveStandsData exclusiveStands = JCraft.getExclusiveStandsData();

        if (!evolutions.containsKey(item)) {
            return EventResult.pass();
        }

        Collection<Evolution> evolutions = EvolutionItemHandler.evolutions.get(item);
        CommonStandComponent standData = JComponentPlatformUtils.getStandComponent(player);
        StandType current = standData.getType();

        // If there's a more specific evolution that matches, use that.
        // Otherwise, if there's one with a null input, use that.
        Evolution fallback = evolutions.stream()
                .filter(e -> e.predicate().test(handStack) && e.stand() == null)
                .findFirst()
                .orElse(null);

        Evolution evolution = fallback;
        if (current != null) evolution = evolutions.stream()
                .filter(e -> e.predicate().test(handStack) && e.stand() == current)
                .findFirst()
                .orElse(fallback);

        if (evolution == null || StandTypeUtil.isIn(evolution.target(), JTagRegistry.PLAYER_STAND_BLACKLIST, player.level().registryAccess())) {
            return EventResult.pass();
        }

        // Check if target stand is already in use.
        if (exclusiveStands.isStandUsed(evolution.target())) {
            return EventResult.pass();
        }

        if (!player.isCreative()) handStack.shrink(1);

        standData.setTypeAndSkin(evolution.target(), standData.getSkin(), false);
        player.awardStat(JStatRegistry.STAND_EVOLVED.get());

        // End any active Time Stop cleanly before desummoning/swapping stand
        if (net.arna.jcraft.common.tickable.Timestops.isUserOfActiveTimestop(player)) {
            JCraft.stopTimestop(player);
        }

        // Re-summon users stand
        StandEntity<?, ?> stand = standData.getStand();
        if (stand != null) {
            stand.setTsTime(0);
            stand.desummon();
        }
        JCraft.summon(player.level(), player);

        return EventResult.interruptTrue();
    }

    public static CompletableFuture<Void> onReload(PreparableReloadListener.PreparationBarrier preparationBarrier,
                                                   ResourceManager resourceManager, ProfilerFiller preparationsProfiler,
                                                   ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> readEvolutions(resourceManager), backgroundExecutor)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync(evs -> {
                    serverLoaded = true;
                    evolutions.clear();
                    evs.forEach(e -> evolutions.put(e.item(), e));
                }, gameExecutor);
    }

    private static Collection<Evolution> readEvolutions(ResourceManager resourceManager) {
        Map<Identifier, Resource> evolutionFiles = resourceManager.listResources("evolutions",
                rl -> rl.getPath().endsWith(".json"));

        Gson gson = new Gson();
        List<Evolution> evolutions = new ArrayList<>();
        for (final Map.Entry<Identifier, Resource> entry : evolutionFiles.entrySet()) {
            JsonObject obj;
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                obj = gson.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                JCraft.LOGGER.error("Failed to read evolutions from {}", entry.getKey(), e);
                continue;
            }

            Evolution.CODEC.parse(JsonOps.INSTANCE, obj).resultOrPartial(err -> JCraft.LOGGER.error("Failed to parse evolution {}: {}", entry.getKey(), err)).ifPresent(evolutions::add);
        }

        return evolutions;
    }

    public record Evolution(Item item, ItemPredicate predicate, @Nullable StandType stand, StandType target) {
        public static final Codec<Evolution> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Evolution::item),
                JCodecUtils.ITEM_PREDICATE_CODEC.optionalFieldOf("predicate", ItemPredicate.Builder.item().build()).forGetter(Evolution::predicate),
                JRegistries.STAND_TYPE_CODEC.optionalFieldOf("stand").forGetter(e -> Optional.ofNullable(e.stand())),
                JRegistries.STAND_TYPE_CODEC.fieldOf("target").forGetter(Evolution::target)
        ).apply(instance, Evolution::new));

        public Evolution(Item item, @Nullable StandType stand, StandType target) {
            this(item, ItemPredicate.Builder.item().build(), stand, target);
        }

        private Evolution(Item item, ItemPredicate predicate, Optional<StandType> stand, StandType target) {
            this(item, predicate, stand.orElse(null), target);
        }
    }
}
