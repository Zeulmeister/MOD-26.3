package net.arna.jcraft.common.marker;

import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;

public interface Identifiers {

    String MINECRAFT = Identifier.DEFAULT_NAMESPACE;

    // Entity
    Identifier POSITION = Identifier.fromNamespaceAndPath(MINECRAFT, "pos");
    Identifier VELOCITY = Identifier.fromNamespaceAndPath(MINECRAFT, "vel");
    Identifier PITCH = Identifier.fromNamespaceAndPath(MINECRAFT, "xrot");
    Identifier YAW = Identifier.fromNamespaceAndPath(MINECRAFT, "yrot");
    Identifier YAW_HEAD = Identifier.fromNamespaceAndPath(MINECRAFT, "yheadrot");
    Identifier FALL_DISTANCE = Identifier.fromNamespaceAndPath(MINECRAFT, "fall_distance");
    Identifier FIRE = Identifier.fromNamespaceAndPath(MINECRAFT, "fire");
    Identifier AIR = Identifier.fromNamespaceAndPath(MINECRAFT, "air");
    Identifier GROUNDED = Identifier.fromNamespaceAndPath(MINECRAFT, "grounded");
    Identifier INVULNERABLE = Identifier.fromNamespaceAndPath(MINECRAFT, "invulnerable");
    Identifier PORTAL_COOLDOWN = Identifier.fromNamespaceAndPath(MINECRAFT, "portal_cooldown");
    Identifier UUID = Identifier.fromNamespaceAndPath(MINECRAFT, "uuid");
    Identifier CUSTOM_NAME = Identifier.fromNamespaceAndPath(MINECRAFT, "custom_name");
    Identifier CUSTOM_NAME_VISIBLE = Identifier.fromNamespaceAndPath(MINECRAFT, "custom_name_visible");
    Identifier SILENT = Identifier.fromNamespaceAndPath(MINECRAFT, "silent");
    Identifier NO_GRAVITY = Identifier.fromNamespaceAndPath(MINECRAFT, "no_gravity");
    Identifier GLOWING = Identifier.fromNamespaceAndPath(MINECRAFT, "glowing");
    Identifier TICKS_FROZEN = Identifier.fromNamespaceAndPath(MINECRAFT, "ticks_frozen");
    Identifier TAGS = Identifier.fromNamespaceAndPath(MINECRAFT, "tags");
    Identifier VEHICLE = Identifier.fromNamespaceAndPath(MINECRAFT, "vehicle");

    // Living Entity
    Identifier HEALTH = Identifier.fromNamespaceAndPath(MINECRAFT, "health");
    Identifier HURT_TIME = Identifier.fromNamespaceAndPath(MINECRAFT, "hurt_time");
    Identifier DEATH_TIME = Identifier.fromNamespaceAndPath(MINECRAFT, "death_time");
    Identifier ABSORPTION_AMOUNT = Identifier.fromNamespaceAndPath(MINECRAFT, "absorption_amount");
    Identifier ATTRIBUTES = Identifier.fromNamespaceAndPath(MINECRAFT, "attributes");
    Identifier ACTIVE_EFFECTS = Identifier.fromNamespaceAndPath(MINECRAFT, "active_effects");
    Identifier SLEEPING_POSITION = Identifier.fromNamespaceAndPath(MINECRAFT, "sleeping_pos");

    // Mobs
    Identifier CAN_PICKUP_LOOT = Identifier.fromNamespaceAndPath(MINECRAFT, "can_pickup_loot");
    Identifier PERSISTENCE_REQUIRED = Identifier.fromNamespaceAndPath(MINECRAFT, "can_pickup_loot");
    Identifier LEFT_HANDED_MOB = Identifier.fromNamespaceAndPath(MINECRAFT, "left_handed_mob");
    Identifier NO_AI = Identifier.fromNamespaceAndPath(MINECRAFT, "no_ai");

    // Ageable Mobs
    Identifier AGE = Identifier.fromNamespaceAndPath(MINECRAFT, "age");

    // Player
    Identifier INVENTORY = Identifier.fromNamespaceAndPath(MINECRAFT, "player_inventory");
    Identifier SELECTED_ITEM_SLOT = Identifier.fromNamespaceAndPath(MINECRAFT, "selected_item_slot");
    Identifier XP_PROGRESS = Identifier.fromNamespaceAndPath(MINECRAFT, "xp_progress");
    Identifier XP_LEVEL = Identifier.fromNamespaceAndPath(MINECRAFT, "xp_level");
    Identifier XP_TOTAL = Identifier.fromNamespaceAndPath(MINECRAFT, "xp_total");
    Identifier SCORE = Identifier.fromNamespaceAndPath(MINECRAFT, "score");
    Identifier FOOD_DATA = Identifier.fromNamespaceAndPath(MINECRAFT, "food_data");
    Identifier PLAYER_ABILITIES = Identifier.fromNamespaceAndPath(MINECRAFT, "player_abilities");
    Identifier ENDER_CHEST = Identifier.fromNamespaceAndPath(MINECRAFT, "ender_chest");

    // Vampires
    Identifier BLOOD_GAUGE = JCraft.id("blood_gauge");

    // Hamon Users
    Identifier HAMON_CHARGE = JCraft.id("hamon_charge");
}
