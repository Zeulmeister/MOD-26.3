package net.arna.jcraft.common.entity.ai.goal;

import net.arna.jcraft.common.config.JServerConfig;
import net.arna.jcraft.common.entity.SheerHeartAttackEntity;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

public class SHAAttackGoal extends Goal {
    private final SheerHeartAttackEntity sha;
    private final LookControl shaLookControl;
    private final PathNavigation shaNavigation;
    private final double speed;
    private int cooldown;
    private LivingEntity target;

    public SHAAttackGoal(final SheerHeartAttackEntity mob, final double speed) {
        sha = mob;
        shaLookControl = sha.getLookControl();
        shaNavigation = sha.getNavigation();
        this.speed = speed;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        target = sha.getTarget();
        return target != null;
    }

    public boolean canContinueToUse() {
        if (!target.isAlive() || target.isRemoved() || target.isPermanentlyInvulnerable()) {
            return false;
        }

        final var main = JUtils.getUserIfStand(target);
        if (main instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        final double maxTrackDist = Math.max(16.0D, JServerConfig.SHA_SEARCH_RADIUS.getValue() * 2.0D);
        if (sha.distanceToSqr(target) > maxTrackDist * maxTrackDist) {
            return false;
        } else {
            return !sha.getNavigation().isDone() || canUse();
        }
    }

    public void stop() {
        target = null;
        sha.getNavigation().stop();
    }

    public boolean requiresUpdateEveryTick() {
        return true;
    }

    public void tick() {
        shaLookControl.setLookAt(target, 30.0F, 30.0F);
        shaNavigation.moveTo(target, speed);

        double d = 3.0; // SHA_width^2 * 4
        double e = sha.distanceToSqr(target);

        if (cooldown-- <= 0 && e <= d) {
            cooldown = 100;
            sha.explode();
        }
    }
}
