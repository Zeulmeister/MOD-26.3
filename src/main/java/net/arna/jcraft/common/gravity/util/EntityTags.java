package net.arna.jcraft.common.gravity.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class EntityTags {
    public static final TagKey<EntityType<?>> FORBIDDEN_ENTITIES = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("gravitychanger", "forbidden_entities"));
    public static final TagKey<EntityType<?>> FORBIDDEN_ENTITY_RENDERING = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("gravitychanger", "forbidden_entity_rendering"));

    public static boolean canChangeGravity(Entity entity) {
        if (entity == null) {
            return false;
        }
        return !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(entity.getType()).is(FORBIDDEN_ENTITIES);
    }
}
