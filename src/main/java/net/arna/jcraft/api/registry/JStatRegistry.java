package net.arna.jcraft.api.registry;

import dev.architectury.registry.registries.RegistrySupplier;
import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.stats.StatFormatter;
import net.minecraft.stats.Stats;

public interface JStatRegistry {

    // alphabetic order
    RegistrySupplier<Identifier> BLOOD_SUCKED = registerCustomStat("blood_sucked");
    RegistrySupplier<Identifier> DASHES = registerCustomStat("dashes");
    RegistrySupplier<Identifier> RAW_DAMAGE = registerCustomStat("raw_damage");
    RegistrySupplier<Identifier> SPECS_CHANGED = registerCustomStat("specs_changed");
    RegistrySupplier<Identifier> STAND_EVOLVED = registerCustomStat("stands_evolved");
    RegistrySupplier<Identifier> STAND_SUMMONED = registerCustomStat("stands_summoned");
    RegistrySupplier<Identifier> STAND_USERS_KILLED = registerCustomStat("stand_users_killed");
    RegistrySupplier<Identifier> TIME_STOPPED = registerCustomStat("time_stopped");
    RegistrySupplier<Identifier> VAMPIRE_LASER = registerCustomStat("vampire_laser");
    RegistrySupplier<Identifier> VAMPIRE_REVIVES = registerCustomStat("vampire_revives");

    private static RegistrySupplier<Identifier> registerCustomStat(final String id) {
        final Identifier jcraftId = JCraft.id(id);
        return JCraft.STATS.register(id, () -> jcraftId);
    }

    static void init() {
        // left empty on purpose
    }

    static void initFormatters() {
        Stats.CUSTOM.get(BLOOD_SUCKED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(DASHES.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(RAW_DAMAGE.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(SPECS_CHANGED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(STAND_EVOLVED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(STAND_SUMMONED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(STAND_USERS_KILLED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(TIME_STOPPED.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(VAMPIRE_LASER.get(), StatFormatter.DEFAULT);
        Stats.CUSTOM.get(VAMPIRE_REVIVES.get(), StatFormatter.DEFAULT);
    }
}
