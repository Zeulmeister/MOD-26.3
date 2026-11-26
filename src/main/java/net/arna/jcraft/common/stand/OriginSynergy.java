package net.arna.jcraft.common.stand;

import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.moves.AbstractSimpleAttack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Passive synergy evaluator and move-copy modifier for the Origin Stand.
 * Purely common logic (server + client safe, zero client imports).
 */
public final class OriginSynergy {
    public enum SynergyType {
        CHRONO_SOVEREIGN(
                "chrono_sovereign", 3,
                "jcraft.origin.synergy.chrono_sovereign",
                "jcraft.origin.synergy.chrono_sovereign.desc"
        ),
        OBSIDIAN_JUGGERNAUT(
                "obsidian_juggernaut", 3,
                "jcraft.origin.synergy.obsidian_juggernaut",
                "jcraft.origin.synergy.obsidian_juggernaut.desc"
        ),
        PHANTOM_TEMPEST(
                "phantom_tempest", 2,
                "jcraft.origin.synergy.phantom_tempest",
                "jcraft.origin.synergy.phantom_tempest.desc"
        ),
        ASTRAL_ARTILLERY(
                "astral_artillery", 2,
                "jcraft.origin.synergy.astral_artillery",
                "jcraft.origin.synergy.astral_artillery.desc"
        ),
        OMNIPOTENT_HARMONY(
                "omnipotent_harmony", 4,
                "jcraft.origin.synergy.omnipotent_harmony",
                "jcraft.origin.synergy.omnipotent_harmony.desc"
        );

        private final String id;
        private final int requiredCount;
        private final String titleKey;
        private final String descKey;

        SynergyType(String id, int requiredCount, String titleKey, String descKey) {
            this.id = id;
            this.requiredCount = requiredCount;
            this.titleKey = titleKey;
            this.descKey = descKey;
        }

        public String getId() {
            return id;
        }

        public int getRequiredCount() {
            return requiredCount;
        }

        public String getTitleKey() {
            return titleKey;
        }

        public String getDescKey() {
            return descKey;
        }
    }

    public record SynergyStatus(SynergyType type, int count, int required, boolean active) {}

    public record SynergySummary(List<SynergyStatus> statuses, EnumSet<SynergyType> activeTypes) {
        public static final SynergySummary EMPTY = evaluateCategories(Collections.emptyList());

        public boolean isActive(SynergyType type) {
            return activeTypes.contains(type);
        }

        public int activeCount() {
            return activeTypes.size();
        }
    }

    private OriginSynergy() {}

    public static SynergySummary evaluate(Collection<OriginSkillCatalog.SkillEntry> skills) {
        if (skills == null || skills.isEmpty()) {
            return SynergySummary.EMPTY;
        }
        List<OriginSkillCatalog.SkillCategory> categories = new ArrayList<>(skills.size());
        for (OriginSkillCatalog.SkillEntry entry : skills) {
            if (entry != null && entry.category() != null) {
                categories.add(entry.category());
            }
        }
        return evaluateCategories(categories);
    }

    public static SynergySummary evaluateCategories(Collection<OriginSkillCatalog.SkillCategory> categories) {
        int timeSpace = 0;
        int heavy = 0;
        int tempest = 0;
        int ranged = 0;
        EnumSet<OriginSkillCatalog.SkillCategory> distinct = EnumSet.noneOf(OriginSkillCatalog.SkillCategory.class);

        if (categories != null) {
            for (OriginSkillCatalog.SkillCategory cat : categories) {
                if (cat == null || cat == OriginSkillCatalog.SkillCategory.ALL) {
                    continue;
                }
                distinct.add(cat);
                switch (cat) {
                    case TIME_SPACE -> timeSpace++;
                    case HEAVY -> heavy++;
                    case BARRAGE, MOBILITY -> tempest++;
                    case RANGED -> ranged++;
                    default -> {}
                }
            }
        }

        int distinctCount = distinct.size();
        List<SynergyStatus> statuses = new ArrayList<>(SynergyType.values().length);
        EnumSet<SynergyType> active = EnumSet.noneOf(SynergyType.class);

        addStatus(statuses, active, SynergyType.CHRONO_SOVEREIGN, timeSpace);
        addStatus(statuses, active, SynergyType.OBSIDIAN_JUGGERNAUT, heavy);
        addStatus(statuses, active, SynergyType.PHANTOM_TEMPEST, tempest);
        addStatus(statuses, active, SynergyType.ASTRAL_ARTILLERY, ranged);
        addStatus(statuses, active, SynergyType.OMNIPOTENT_HARMONY, distinctCount);

        return new SynergySummary(Collections.unmodifiableList(statuses), active);
    }

    private static void addStatus(List<SynergyStatus> statuses, EnumSet<SynergyType> active, SynergyType type, int count) {
        boolean isActive = count >= type.getRequiredCount();
        statuses.add(new SynergyStatus(type, count, type.getRequiredCount(), isActive));
        if (isActive) {
            active.add(type);
        }
    }

    /**
     * Applies active synergy bonuses exclusively to a freshly cloned move tree (never templates).
     */
    public static void applyToMoveCopy(
            AbstractMove<?, ?> copied,
            OriginSkillCatalog.SkillCategory category,
            SynergySummary summary) {
        if (copied == null || summary == null || summary.activeCount() == 0) {
            return;
        }
        Set<AbstractMove<?, ?>> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        applyRecursive(copied, category, summary, visited);
    }

    private static void applyRecursive(
            AbstractMove<?, ?> move,
            OriginSkillCatalog.SkillCategory category,
            SynergySummary summary,
            Set<AbstractMove<?, ?>> visited) {
        if (move == null || !visited.add(move)) {
            return;
        }

        float cdMult = 1.0f;
        int minCd = 6;
        if (summary.isActive(SynergyType.OMNIPOTENT_HARMONY)) {
            cdMult *= 0.92f;
        }
        if (category == OriginSkillCatalog.SkillCategory.TIME_SPACE && summary.isActive(SynergyType.CHRONO_SOVEREIGN)) {
            cdMult *= 0.85f;
            minCd = Math.max(minCd, 10);
        }
        if ((category == OriginSkillCatalog.SkillCategory.BARRAGE || category == OriginSkillCatalog.SkillCategory.MOBILITY)
                && summary.isActive(SynergyType.PHANTOM_TEMPEST)) {
            cdMult *= 0.88f;
        }
        if (category == OriginSkillCatalog.SkillCategory.RANGED && summary.isActive(SynergyType.ASTRAL_ARTILLERY)) {
            cdMult *= 0.88f;
        }
        if (cdMult < 0.999f && move.getCooldown() > 0) {
            int updatedCd = Math.max(minCd, Math.round(move.getCooldown() * cdMult));
            move.withCooldown(Math.min(move.getCooldown(), updatedCd));
        }

        if (move instanceof AbstractSimpleAttack<?, ?> atk && atk.getDamage() > 0.0f) {
            float dmgMult = 1.0f;
            if (summary.isActive(SynergyType.OMNIPOTENT_HARMONY)) {
                dmgMult *= 1.08f;
            }
            if (category == OriginSkillCatalog.SkillCategory.RANGED && summary.isActive(SynergyType.ASTRAL_ARTILLERY)) {
                dmgMult *= 1.12f;
            }
            if (dmgMult > 1.001f) {
                atk.withDamage(atk.getDamage() * dmgMult);
            }
        }

        if (summary.isActive(SynergyType.OBSIDIAN_JUGGERNAUT)
                && (category == OriginSkillCatalog.SkillCategory.HEAVY || category == OriginSkillCatalog.SkillCategory.STRIKE)
                && move.getArmor() < Integer.MAX_VALUE - 2) {
            move.withArmor(move.getArmor() + 1);
        }

        applyRecursive(move.getFollowup(), category, summary, visited);
        applyRecursive(move.getCrouchingVariant(), category, summary, visited);
        applyRecursive(move.getAerialVariant(), category, summary, visited);
    }
}
