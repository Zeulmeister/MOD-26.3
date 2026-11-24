package net.arna.jcraft.common.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.api.spec.SpecType;
import net.arna.jcraft.api.spec.JSpec;
import net.arna.jcraft.common.util.JUtils;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.arna.jcraft.api.registry.JStatRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class SpecObtainmentItem extends Item {
    protected final Set<Player> warned = Collections.newSetFromMap(new WeakHashMap<>());
    protected final RegistrySupplier<SpecType> switchTo;

    public SpecObtainmentItem(Properties settings, RegistrySupplier<SpecType> switchTo) {
        super(settings);
        this.switchTo = switchTo;
    }

    protected boolean isWarned(Player player) {
        return player != null && warned.contains(player);
    }

    private boolean setSpec(Player player) {
        if (player == null) {
            return false;
        }

        JComponentPlatformUtils.getSpecData(player).setType(switchTo.get());
        if (!player.level().isClientSide()) {
            player.awardStat(JStatRegistry.SPECS_CHANGED.get());
        }
        warned.remove(player);
        return true;
    }

    /**
     * Applies this spec directly when thrown at a player who currently has no spec.
     */
    public boolean tryApplyOnToss(Player target) {
        if (target == null || target.level().isClientSide()) {
            return false;
        }
        if (JUtils.getSpec(target) != null) {
            return false;
        }
        return setSpec(target);
    }

    protected boolean tryGetSpec(Player player) {
        JSpec<?, ?> spec = JUtils.getSpec(player);
        if (spec != null) { // If the player already has a spec
            if (spec.getType() != switchTo.get()) { // And it isn't the one that will be switched to
                if (!warned.contains(player)) {
                    player.sendSystemMessage(Component.translatable("warning.jcraft.spec.change"));
                    warned.add(player);
                    return false;
                }
                return setSpec(player);
            } else {
                warned.remove(player);
                return false;
            }
        }
        return setSpec(player);
    }
}
