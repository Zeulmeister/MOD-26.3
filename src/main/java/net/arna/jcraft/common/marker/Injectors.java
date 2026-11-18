package net.arna.jcraft.common.marker;

import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.common.util.JValueIO;
import net.arna.jcraft.common.util.NbtUtils;
import net.arna.jcraft.common.util.TriConsumer;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

import static net.arna.jcraft.common.marker.Identifiers.*;

public interface Injectors {

    TriConsumer<Identifier, Entity, CompoundTag> ENTITY = (id, entity, compoundTag) -> {
        if (id == null) return;
        if (id.equals(POSITION) && compoundTag.contains(POSITION.toString()) && compoundTag.contains(PITCH.toString())
                && compoundTag.contains(YAW.toString()) && compoundTag.contains(YAW_HEAD.toString())) {
            final Vec3 pos = NbtUtils.getVec3(compoundTag, POSITION.toString());
            final float pitch = compoundTag.getFloatOr(PITCH.toString(), 0f);
            final float yaw = compoundTag.getFloatOr(YAW.toString(), 0f);
            final float yawHead = compoundTag.getFloatOr(YAW_HEAD.toString(), 0f);
            if (entity instanceof final ServerPlayer serverPlayer) {
                serverPlayer.teleportTo((ServerLevel) serverPlayer.level(), pos.x(), pos.y(), pos.z(),
                        EnumSet.noneOf(Relative.class), yaw, pitch, true);
                serverPlayer.setYHeadRot(yawHead);
                serverPlayer.connection.send(new ClientboundRotateHeadPacket(serverPlayer, (byte) ((int) (yaw * 256.0F / 360.0F))));
            } else {
                entity.teleportTo(pos.x(), pos.y(), pos.z());
                entity.setYRot(yaw);
                entity.setXRot(pitch);
                entity.setYHeadRot(yawHead);
                entity.setYBodyRot(yaw);
                entity.yRotO = yaw;
                entity.xRotO = pitch;
                if (entity instanceof final LivingEntity livingEntity) {
                    livingEntity.yHeadRotO = yawHead;
                    livingEntity.yBodyRotO = yaw;
                }
            }
            if (compoundTag.contains(VEHICLE.toString())) {
                final var vehicleUuid = JValueIO.getUUID(compoundTag, VEHICLE.toString());
                if (vehicleUuid != null && entity.level() instanceof ServerLevel sl) {
                    final Entity vehicle = sl.getEntity(vehicleUuid);
                    if (vehicle != null) {
                        entity.startRiding(vehicle);
                    }
                }
            }
        } else if (id.equals(VELOCITY)) {
            if (compoundTag.contains(VELOCITY.toString())) {
                entity.setDeltaMovement(NbtUtils.getVec3(compoundTag, VELOCITY.toString()));
            } else {
                entity.setDeltaMovement(0d, 0d, 0d);
            }
            if (entity instanceof final ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new ClientboundSetEntityMotionPacket(serverPlayer));
            }
        } else if (id.equals(FALL_DISTANCE)) {
            entity.fallDistance = compoundTag.getDoubleOr(FALL_DISTANCE.toString(), 0.0);
        } else if (id.equals(FIRE)) {
            entity.setRemainingFireTicks(compoundTag.getIntOr(FIRE.toString(), 0));
        } else if (id.equals(AIR)) {
            entity.setAirSupply(compoundTag.getIntOr(AIR.toString(), 20));
        } else if (id.equals(GROUNDED)) {
            entity.setOnGround(compoundTag.getBooleanOr(GROUNDED.toString(), true));
        } else if (id.equals(INVULNERABLE)) {
            entity.setPermanentlyInvulnerable(compoundTag.getBooleanOr(INVULNERABLE.toString(), false));
        } else if (id.equals(PORTAL_COOLDOWN)) {
            entity.setPortalCooldown(compoundTag.getIntOr(PORTAL_COOLDOWN.toString(), 0));
        } else if (id.equals(UUID) && compoundTag.contains(UUID.toString())) {
            var u = JValueIO.getUUID(compoundTag, UUID.toString());
            if (u != null) entity.setUUID(u);
        } else if (id.equals(CUSTOM_NAME)) {
            Tag nameTag = compoundTag.get(CUSTOM_NAME.toString());
            if (nameTag == null) {
                entity.setCustomName(null);
            } else {
                ComponentSerialization.CODEC.parse(entity.registryAccess().createSerializationContext(NbtOps.INSTANCE), nameTag)
                        .result().ifPresent(entity::setCustomName);
            }
        } else if (id.equals(CUSTOM_NAME_VISIBLE)) {
            entity.setCustomNameVisible(compoundTag.getBooleanOr(CUSTOM_NAME_VISIBLE.toString(), true));
        } else if (id.equals(SILENT)) {
            entity.setSilent(compoundTag.getBooleanOr(SILENT.toString(), false));
        } else if (id.equals(NO_GRAVITY)) {
            entity.setNoGravity(compoundTag.getBooleanOr(NO_GRAVITY.toString(), false));
        } else if (id.equals(GLOWING)) {
            entity.setGlowingTag(compoundTag.getBooleanOr(GLOWING.toString(), false));
        } else if (id.equals(TICKS_FROZEN)) {
            entity.setTicksFrozen(compoundTag.getIntOr(TICKS_FROZEN.toString(), 0));
        } else if (id.equals(TAGS)) {
            entity.entityTags().clear();
            if (compoundTag.contains(TAGS.toString())) {
                final ListTag list = compoundTag.getListOrEmpty(TAGS.toString());
                for (int i = 0; i < list.size(); i++) {
                    list.getString(i).ifPresent(entity::addTag);
                }
            }
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> LIVING_ENTITY = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        if (id.equals(HEALTH)) {
            livingEntity.setHealth(compoundTag.getFloatOr(HEALTH.toString(), livingEntity.getMaxHealth()));
        } else if (id.equals(HURT_TIME) && compoundTag.contains(HURT_TIME.toString())) {
            livingEntity.hurtTime = compoundTag.getIntOr(HURT_TIME.toString(), 0);
        } else if (id.equals(DEATH_TIME) && compoundTag.contains(DEATH_TIME.toString())) {
            livingEntity.deathTime = compoundTag.getIntOr(DEATH_TIME.toString(), 0);
        } else if (id.equals(ABSORPTION_AMOUNT)) {
            livingEntity.setAbsorptionAmount(compoundTag.getFloatOr(ABSORPTION_AMOUNT.toString(), 0f));
        } else if (id.equals(ACTIVE_EFFECTS)) {
            livingEntity.getActiveEffectsMap().clear();
            if (compoundTag.contains(ACTIVE_EFFECTS.toString())) {
                var ops = livingEntity.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                final ListTag list = compoundTag.getListOrEmpty(ACTIVE_EFFECTS.toString());
                for (int i = 0; i < list.size(); ++i) {
                    Tag elem = list.get(i);
                    MobEffectInstance.CODEC.parse(ops, elem).result().ifPresent(inst ->
                            livingEntity.getActiveEffectsMap().put(inst.getEffect(), inst));
                }
            }
        } else if (id.equals(SLEEPING_POSITION) && compoundTag.contains(SLEEPING_POSITION.toString())) {
            livingEntity.setSleepingPos(new BlockPos(NbtUtils.getVec3i(compoundTag, SLEEPING_POSITION.toString()).getX(), NbtUtils.getVec3i(compoundTag, SLEEPING_POSITION.toString()).getY(), NbtUtils.getVec3i(compoundTag, SLEEPING_POSITION.toString()).getZ()));
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> PLAYER = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final Player player)) return;
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (id.equals(INVENTORY)) {
            player.getInventory().clearContent();
            if (compoundTag.contains(INVENTORY.toString())) {
                final ListTag list = compoundTag.getListOrEmpty(INVENTORY.toString());
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof CompoundTag ct) {
                        int slot = ct.getIntOr("Slot", -1);
                        if (slot >= 0 && slot < player.getInventory().getContainerSize()) {
                            ItemStack.CODEC.parse(ops, ct).result().ifPresent(st -> player.getInventory().setItem(slot, st));
                        }
                    }
                }
            }
        } else if (id.equals(SELECTED_ITEM_SLOT)) {
            player.getInventory().setSelectedSlot(compoundTag.getIntOr(SELECTED_ITEM_SLOT.toString(), 0));
        } else if (id.equals(XP_PROGRESS)) {
            player.experienceProgress = compoundTag.getFloatOr(XP_PROGRESS.toString(), 0f);
        } else if (id.equals(XP_LEVEL)) {
            player.experienceLevel = compoundTag.getIntOr(XP_LEVEL.toString(), 0);
        } else if (id.equals(XP_TOTAL)) {
            player.totalExperience = compoundTag.getIntOr(XP_TOTAL.toString(), 0);
        } else if (id.equals(SCORE)) {
            player.setScore(compoundTag.getIntOr(SCORE.toString(), 0));
        } else if (id.equals(FOOD_DATA) && compoundTag.contains(FOOD_DATA.toString())) {
            CompoundTag fd = compoundTag.getCompoundOrEmpty(FOOD_DATA.toString());
            player.getFoodData().setFoodLevel(fd.getIntOr("foodLevel", 20));
            player.getFoodData().setSaturation(fd.getFloatOr("saturationLevel", 5f));
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> MOB = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final Mob mob)) return;
        if (id.equals(CAN_PICKUP_LOOT)) {
            mob.setCanPickUpLoot(compoundTag.getBooleanOr(CAN_PICKUP_LOOT.toString(), false));
        } else if (id.equals(PERSISTENCE_REQUIRED)) {
            if (compoundTag.getBooleanOr(PERSISTENCE_REQUIRED.toString(), false)) {
                mob.setPersistenceRequired();
            }
        } else if (id.equals(LEFT_HANDED_MOB) && compoundTag.contains(LEFT_HANDED_MOB.toString())) {
            mob.setLeftHanded(compoundTag.getBooleanOr(LEFT_HANDED_MOB.toString(), false));
        } else if (id.equals(NO_AI)) {
            mob.setNoAi(compoundTag.getBooleanOr(NO_AI.toString(), false));
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> AGABLE_MOB = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final AgeableMob ageableMob)) return;
        if (id.equals(AGE)) {
            ageableMob.setAge(compoundTag.getIntOr(AGE.toString(), 0));
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> VAMPIRE = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        final CommonVampireComponent vampire = JComponentPlatformUtils.getVampirism(livingEntity);
        if (vampire != null && vampire.isVampire() && id.equals(BLOOD_GAUGE)) {
            if (compoundTag.contains(BLOOD_GAUGE.toString())) {
                vampire.setBlood(compoundTag.getFloatOr(BLOOD_GAUGE.toString(), 0f));
            } else {
                vampire.resetBlood();
            }
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> HAMON = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        final CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(livingEntity);
        if (hamon != null && id.equals(HAMON_CHARGE)) {
            if (compoundTag.contains(HAMON_CHARGE.toString())) {
                hamon.setHamonCharge(compoundTag.getFloatOr(HAMON_CHARGE.toString(), 0f));
            } else {
                hamon.resetHamonCharge();
            }
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> ALL = (id, entity, compoundTag) -> {
        ENTITY.accept(id, entity, compoundTag);
        LIVING_ENTITY.accept(id, entity, compoundTag);
        PLAYER.accept(id, entity, compoundTag);
        MOB.accept(id, entity, compoundTag);
        AGABLE_MOB.accept(id, entity, compoundTag);
        VAMPIRE.accept(id, entity, compoundTag);
        HAMON.accept(id, entity, compoundTag);
    };
}
