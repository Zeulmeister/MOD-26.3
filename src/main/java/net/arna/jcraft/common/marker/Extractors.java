package net.arna.jcraft.common.marker;

import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.common.spec.HamonSpec;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.common.util.JValueIO;
import net.arna.jcraft.common.util.NbtUtils;
import net.arna.jcraft.common.util.TriConsumer;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static net.arna.jcraft.common.marker.Identifiers.*;

public interface Extractors {
    TriConsumer<Identifier, Entity, CompoundTag> ENTITY = (id, entity, compoundTag) -> {
        if (id == null) return;
        if (id.equals(POSITION)) {
            final Entity vehicle = entity.getVehicle();
            if (vehicle != null) {
                NbtUtils.put(compoundTag, POSITION.toString(), new Vec3(vehicle.getX(), vehicle.getY(), vehicle.getZ()));
            } else {
                NbtUtils.put(compoundTag, POSITION.toString(), new Vec3(entity.getX(), entity.getY(), entity.getZ()));
            }
        } else if (id.equals(VELOCITY)) {
            NbtUtils.put(compoundTag, VELOCITY.toString(), entity.getDeltaMovement());
        } else if (id.equals(PITCH)) {
            compoundTag.putFloat(PITCH.toString(), entity.getXRot());
        } else if (id.equals(YAW)) {
            compoundTag.putFloat(YAW.toString(), entity.getYRot());
        } else if (id.equals(YAW_HEAD)) {
            compoundTag.putFloat(YAW_HEAD.toString(), entity.getYHeadRot());
        } else if (id.equals(FALL_DISTANCE)) {
            compoundTag.putDouble(FALL_DISTANCE.toString(), entity.fallDistance);
        } else if (id.equals(FIRE)) {
            compoundTag.putInt(FIRE.toString(), entity.getRemainingFireTicks());
        } else if (id.equals(AIR)) {
            compoundTag.putInt(AIR.toString(), entity.getAirSupply());
        } else if (id.equals(GROUNDED)) {
            compoundTag.putBoolean(GROUNDED.toString(), entity.onGround());
        } else if (id.equals(INVULNERABLE)) {
            compoundTag.putBoolean(INVULNERABLE.toString(), entity.isInvulnerable());
        } else if (id.equals(PORTAL_COOLDOWN)) {
            compoundTag.putInt(PORTAL_COOLDOWN.toString(), entity.getPortalCooldown());
        } else if (id.equals(UUID)) {
            JValueIO.putUUID(compoundTag, UUID.toString(), entity.getUUID());
        } else if (id.equals(CUSTOM_NAME)) {
            final Component component = entity.getCustomName();
            if (component != null) {
                ComponentSerialization.CODEC.encodeStart(entity.registryAccess().createSerializationContext(NbtOps.INSTANCE), component)
                        .result().ifPresent(t -> compoundTag.put(CUSTOM_NAME.toString(), t));
            }
        } else if (id.equals(CUSTOM_NAME_VISIBLE)) {
            compoundTag.putBoolean(CUSTOM_NAME_VISIBLE.toString(), entity.isCustomNameVisible());
        } else if (id.equals(SILENT)) {
            compoundTag.putBoolean(SILENT.toString(), entity.isSilent());
        } else if (id.equals(NO_GRAVITY)) {
            compoundTag.putBoolean(NO_GRAVITY.toString(), entity.isNoGravity());
        } else if (id.equals(GLOWING)) {
            compoundTag.putBoolean(GLOWING.toString(), entity.hasGlowingTag());
        } else if (id.equals(TICKS_FROZEN)) {
            compoundTag.putInt(TICKS_FROZEN.toString(), entity.getTicksFrozen());
        } else if (id.equals(TAGS)) {
            final ListTag listTag = new ListTag();
            for (final String string : entity.entityTags()) {
                listTag.add(StringTag.valueOf(string));
            }
            compoundTag.put(TAGS.toString(), listTag);
        } else if (id.equals(VEHICLE) && entity.getVehicle() != null) {
            JValueIO.putUUID(compoundTag, VEHICLE.toString(), entity.getVehicle().getUUID());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> LIVING_ENTITY = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        if (id.equals(HEALTH)) {
            compoundTag.putFloat(HEALTH.toString(), livingEntity.getHealth());
        } else if (id.equals(HURT_TIME)) {
            compoundTag.putInt(HURT_TIME.toString(), livingEntity.hurtTime);
        } else if (id.equals(DEATH_TIME)) {
            compoundTag.putInt(DEATH_TIME.toString(), livingEntity.deathTime);
        } else if (id.equals(ABSORPTION_AMOUNT)) {
            compoundTag.putFloat(ABSORPTION_AMOUNT.toString(), livingEntity.getAbsorptionAmount());
        } else if (id.equals(ACTIVE_EFFECTS)) {
            final ListTag listTag = new ListTag();
            var ops = livingEntity.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            for (final MobEffectInstance mobEffectInstance : livingEntity.getActiveEffects()) {
                MobEffectInstance.CODEC.encodeStart(ops, mobEffectInstance).result().ifPresent(listTag::add);
            }
            compoundTag.put(ACTIVE_EFFECTS.toString(), listTag);
        } else if (id.equals(SLEEPING_POSITION) && livingEntity.getSleepingPos().isPresent()) {
            NbtUtils.put(compoundTag, SLEEPING_POSITION.toString(), livingEntity.getSleepingPos().get());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> PLAYER = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final Player player)) return;
        var ops = player.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (id.equals(INVENTORY)) {
            ListTag invList = new ListTag();
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack st = player.getInventory().getItem(i);
                if (!st.isEmpty()) {
                    int slotIdx = i;
                    ItemStack.CODEC.encodeStart(ops, st).result().ifPresent(t -> {
                        if (t instanceof CompoundTag ct) {
                            ct.putInt("Slot", slotIdx);
                            invList.add(ct);
                        }
                    });
                }
            }
            compoundTag.put(INVENTORY.toString(), invList);
        } else if (id.equals(SELECTED_ITEM_SLOT)) {
            compoundTag.putInt(SELECTED_ITEM_SLOT.toString(), player.getInventory().getSelectedSlot());
        } else if (id.equals(XP_PROGRESS)) {
            compoundTag.putFloat(XP_PROGRESS.toString(), player.experienceProgress);
        } else if (id.equals(XP_LEVEL)) {
            compoundTag.putInt(XP_LEVEL.toString(), player.experienceLevel);
        } else if (id.equals(XP_TOTAL)) {
            compoundTag.putInt(XP_TOTAL.toString(), player.totalExperience);
        } else if (id.equals(SCORE)) {
            compoundTag.putInt(SCORE.toString(), player.getScore());
        } else if (id.equals(FOOD_DATA)) {
            CompoundTag fd = new CompoundTag();
            fd.putInt("foodLevel", player.getFoodData().getFoodLevel());
            fd.putFloat("saturationLevel", player.getFoodData().getSaturationLevel());
            compoundTag.put(FOOD_DATA.toString(), fd);
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> MOB = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final Mob mob)) return;
        if (id.equals(CAN_PICKUP_LOOT)) {
            compoundTag.putBoolean(CAN_PICKUP_LOOT.toString(), mob.canPickUpLoot());
        } else if (id.equals(PERSISTENCE_REQUIRED)) {
            compoundTag.putBoolean(PERSISTENCE_REQUIRED.toString(), mob.isPersistenceRequired());
        } else if (id.equals(LEFT_HANDED_MOB)) {
            compoundTag.putBoolean(LEFT_HANDED_MOB.toString(), mob.isLeftHanded());
        } else if (id.equals(NO_AI)) {
            compoundTag.putBoolean(NO_AI.toString(), mob.isNoAi());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> AGEABLE_MOB = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final AgeableMob ageableMob)) return;
        if (id.equals(AGE)) {
            compoundTag.putInt(AGE.toString(), ageableMob.getAge());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> VAMPIRE = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        final CommonVampireComponent vampire = JComponentPlatformUtils.getVampirism(livingEntity);
        if (vampire != null && vampire.isVampire() && id.equals(BLOOD_GAUGE)) {
            compoundTag.putFloat(BLOOD_GAUGE.toString(), vampire.getBlood());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> HAMON = (id, entity, compoundTag) -> {
        if (id == null || !(entity instanceof final LivingEntity livingEntity)) return;
        final CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(livingEntity);
        if (hamon != null && JUtils.getSpec(livingEntity) instanceof HamonSpec && id.equals(HAMON_CHARGE)) {
            compoundTag.putFloat(HAMON_CHARGE.toString(), hamon.getHamonCharge());
        }
    };

    TriConsumer<Identifier, Entity, CompoundTag> ALL = (id, entity, compoundTag) -> {
        ENTITY.accept(id, entity, compoundTag);
        LIVING_ENTITY.accept(id, entity, compoundTag);
        PLAYER.accept(id, entity, compoundTag);
        MOB.accept(id, entity, compoundTag);
        AGEABLE_MOB.accept(id, entity, compoundTag);
        VAMPIRE.accept(id, entity, compoundTag);
        HAMON.accept(id, entity, compoundTag);
    };
}
