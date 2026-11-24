package net.arna.jcraft.platform;

import net.arna.jcraft.api.component.entity.CommonGrabComponent;
import net.arna.jcraft.api.component.entity.CommonGravityComponent;
import net.arna.jcraft.api.component.entity.CommonTimeStopComponent;
import net.arna.jcraft.api.component.living.*;
import net.arna.jcraft.api.component.player.CommonPhComponent;
import net.arna.jcraft.api.component.player.CommonSpecComponent;
import net.arna.jcraft.api.component.world.CommonShockwaveHandlerComponent;
import net.arna.jcraft.api.component.world.CommonTexasHoldEmComponent;
import net.arna.jcraft.fabric.common.component.JComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;

public class JComponentPlatformUtils {

    public static CommonStandComponent getStandComponent(LivingEntity entity) {
        return JComponents.STAND.get(entity);
    }

    public static CommonSpecComponent getSpecData(LivingEntity livingEntity) {
        return JComponents.SPEC.get(livingEntity);
    }

    public static CommonPhComponent getPhData(Player player) {
        return JComponents.PH.get(player);
    }

    public static CommonCooldownsComponent getCooldowns(LivingEntity entity) {
        return JComponents.COOLDOWNS.get(entity);
    }

    @SuppressWarnings("unchecked")
    public static Optional<CommonTimeStopComponent> getTimeStopData(Entity entity) {
        return (Optional<CommonTimeStopComponent>) (Optional<?>) JComponents.TIME_STOP.maybeGet(entity);
    }

    public static CommonMiscComponent getMiscData(LivingEntity entity) {
        return JComponents.MISC.get(entity);
    }

    public static CommonBombTrackerComponent getBombTracker(LivingEntity entity) {
        return JComponents.BOMB_TRACKER.get(entity);
    }

    public static CommonGrabComponent getGrab(LivingEntity entity) {
        return JComponents.GRAB.get(entity);
    }

    public static CommonHitPropertyComponent getHitProperties(LivingEntity livingEntity) {
        return JComponents.HIT_PROPERTY.get(livingEntity);
    }

    @SuppressWarnings("unchecked")
    public static Optional<CommonGravityComponent> getGravity(Entity entity) {
        if (!(entity instanceof org.ladysnake.cca.api.v3.component.ComponentProvider provider)
                || provider.getComponentContainer() == null) {
            return Optional.empty();
        }
        return (Optional<CommonGravityComponent>) (Optional<?>) JComponents.GRAVITY_MODIFIER.maybeGet(entity);
    }

    public static CommonGravityShiftComponent getGravityShift(LivingEntity entity) {
        return JComponents.GRAVITY_SHIFT.get(entity);
    }

    public static CommonShockwaveHandlerComponent getShockwaveHandler(Level world) {
        return JComponents.SHOCKWAVE_HANDLER.get(world);
    }

    public static CommonTexasHoldEmComponent getTexasHoldEmComponent(Level world) {
        return JComponents.TEXAS_HOLD_EM.get(world);
    }

    public static CommonHamonComponent getHamon(LivingEntity living) {
        return JComponents.HAMON.get(living);
    }

    public static CommonVampireComponent getVampirism(LivingEntity living) {
        return JComponents.VAMPIRE.get(living);
    }

    public static CommonGunslingerComponent getGunslinger(LivingEntity living) {
        return JComponents.GUNSLINGER.get(living);
    }
}
