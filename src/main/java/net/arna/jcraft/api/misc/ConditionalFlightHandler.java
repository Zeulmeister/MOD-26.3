package net.arna.jcraft.api.misc;

import dev.architectury.event.events.common.TickEvent;
import net.arna.jcraft.common.entity.stand.CreamEntity;
import net.arna.jcraft.common.entity.stand.GEREntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Allows for safely setting a player's flight status without fear of it lingering upon state change.
 * E.g., if a stand has a flight move that constantly sets the player's flight status,
 * abruptly cancelling this move might inadvertently leave the player in a state of allowed flight as the move will
 * no longer be ticked after that and can thus not set the player's flight state back. This class seeks to resolve that
 * by always running the check.
 */
// TODO ideally, we'd use Flight API, but it's for 1.21+ only: https://www.curseforge.com/minecraft/mc-mods/flight-api
public class ConditionalFlightHandler {
    private static final List<FlightTracker> TRACKERS = new ArrayList<>();
    private static volatile Predicate<Player> clientTimestopChecker = player -> false;

    public static void setClientTimestopChecker(Predicate<Player> checker) {
        clientTimestopChecker = checker != null ? checker : player -> false;
    }

    public static void tickClientPlayer(@Nullable Player player) {
        if (player != null) {
            onTick(List.of(player));
        }
    }

    @ApiStatus.Internal
    public static void init() {
        TickEvent.SERVER_POST.register(server -> onTick(server.getPlayerList().getPlayers()));
        registerJCraftConditions();
    }

    private static void registerJCraftConditions() {
        addCondition(
                p -> JUtils.getStand(p) instanceof GEREntity,
                p -> JUtils.getStand(p) instanceof GEREntity ger && ger.getFlightTime() > 0
        );
        addCondition(
                ConditionalFlightHandler::isActiveTimestopUser,
                ConditionalFlightHandler::isActiveTimestopUser
        );
    }

    private static boolean isActiveTimestopUser(Player player) {
        return player instanceof ServerPlayer
                ? net.arna.jcraft.common.tickable.Timestops.isUserOfActiveTimestop(player)
                : clientTimestopChecker.test(player);
    }

    public static boolean isAllowedJCraftFlight(Player player) {
        return isActiveTimestopUser(player)
                || (JUtils.getStand(player) instanceof GEREntity ger && ger.getFlightTime() > 0)
                || CreamEntity.isCreaming(player);
    }

    /**
     * Immediately revokes temporary JCraft flight (e.g. when Time Stop ends or when loading a save
     * that still had lingering survival flight enabled). Never strips Creative or Spectator flight.
     */
    public static void revokeStaleFlightIfNotAllowed(Player player) {
        if (player == null || player.isCreative() || player.isSpectator() || isAllowedJCraftFlight(player)) {
            return;
        }
        for (FlightTracker tracker : TRACKERS) {
            tracker.markInactive(player);
        }
        if (player.getAbilities().mayfly || player.getAbilities().flying) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.resetFallDistance();
            if (player instanceof ServerPlayer sp) {
                sp.onUpdateAbilities();
            }
        }
    }

    /**
     * Adds a flight condition to the list.
     * This method takes two predicates. One determines whether it should even be taken into account.
     * This predicate is used to prevent overriding the player's flight ability
     * @param shouldCheck Whether this condition should be checked
     * @param flightCondition Whether the player may fly
     */
    public static void addCondition(Predicate<Player> shouldCheck, Predicate<Player> flightCondition) {
        TRACKERS.add(new FlightTracker(shouldCheck, flightCondition));
    }

    private static void onTick(Collection<? extends Player> players) {
        for (Player player : players) {
            boolean foundPositiveTracker = false;
            boolean anyChangedToInactive = false;
            boolean forceHoverNow = isActiveTimestopUser(player)
                    || (JUtils.getStand(player) instanceof GEREntity ger && ger.getFlightTime() > 0);

            for (FlightTracker tracker : TRACKERS) {
                Boolean flightState = tracker.determineFlightState(player);

                if (flightState == null) continue;

                if (!flightState) {
                    tracker.markInactive(player);
                    anyChangedToInactive = true;
                } else {
                    foundPositiveTracker = true;
                    tracker.markActive(player);
                }
            }

            if (foundPositiveTracker) {
                boolean wasAllowed = player.getAbilities().mayfly;
                boolean wasFlying = player.getAbilities().flying;
                player.getAbilities().mayfly = true;
                if (forceHoverNow && !player.onGround()) {
                    player.getAbilities().flying = true;
                }
                player.resetFallDistance();
                if ((!wasAllowed || player.getAbilities().flying != wasFlying) && player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            } else if (anyChangedToInactive && !player.isCreative() && !player.isSpectator() && !CreamEntity.isCreaming(player)) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.resetFallDistance();
                if (player instanceof ServerPlayer sp) {
                    sp.onUpdateAbilities();
                }
            }
        }
    }

    private static class FlightTracker {
        // We track server and local players separately because the LocalPlayer equivalent of a ServerPlayer
        // has the same hashcode and will clash if kept in the same set.
        private final Set<ServerPlayer> activeServerPlayers = Collections.newSetFromMap(new WeakHashMap<>());
        private final Set<Player> activeLocalPlayers = Collections.newSetFromMap(new WeakHashMap<>());
        private final Predicate<Player> shouldCheck, flightCondition;

        private FlightTracker(Predicate<Player> shouldCheck, Predicate<Player> flightCondition) {
            this.shouldCheck = shouldCheck;
            this.flightCondition = flightCondition;
        }

        /**
         * Determines whether this player may fly according to this tracker.
         * @param player The player to check for
         * @return False if the player may not fly, true if they may, null if this tracker doesn't apply to them.
         */
        public Boolean determineFlightState(Player player) {
            if (player.isCreative() || player.isSpectator()) return null;

            if (!shouldCheck.test(player)) {
                if ((player instanceof ServerPlayer ? activeServerPlayers : activeLocalPlayers).contains(player)) {
                    // Player was flying because of this tracker, but this tracker no longer applies to them
                    // so we disable their flight.
                    return false;
                }

                return null;
            }

            return flightCondition.test(player);
        }

        public void markInactive(Player player) {
            if (player instanceof ServerPlayer)
                activeServerPlayers.remove(player);
            else
                activeLocalPlayers.remove(player);
        }

        public void markActive(Player player) {
            if (player instanceof ServerPlayer sp)
                activeServerPlayers.add(sp);
            else
                activeLocalPlayers.add(player);
        }
    }
}
