package net.arna.jcraft.common.stand;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.JRegistries;
import net.arna.jcraft.api.attack.MoveMap;
import net.arna.jcraft.api.attack.MoveSet;
import net.arna.jcraft.api.attack.MoveSetManager;
import net.arna.jcraft.api.attack.core.MoveAction;
import net.arna.jcraft.api.attack.core.MoveCondition;
import net.arna.jcraft.api.attack.enums.MoveClass;
import net.arna.jcraft.api.attack.moves.AbstractBarrageAttack;
import net.arna.jcraft.api.attack.moves.AbstractCounterAttack;
import net.arna.jcraft.api.attack.moves.AbstractGrabAttack;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.attack.moves.AbstractTimeStopMove;
import net.arna.jcraft.api.attack.moves.AbstractUppercutAttack;
import net.arna.jcraft.api.registry.JStandTypeRegistry;
import net.arna.jcraft.api.stand.StandType;
import net.arna.jcraft.api.stand.StandTypeUtil;
import net.arna.jcraft.common.attack.moves.kingcrimson.TimeEraseMove;
import net.arna.jcraft.common.attack.moves.madeinheaven.TimeAccelerationMove;
import net.arna.jcraft.common.attack.moves.shared.TimeSkipMove;
import net.arna.jcraft.common.attack.moves.shared.TossChargeMove;
import net.arna.jcraft.common.attack.moves.shared.TossMove;
import net.arna.jcraft.common.attack.moves.theworld.overheaven.ChargeOverwriteMove;
import net.arna.jcraft.common.attack.moves.theworld.overheaven.OverwriteAttack;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Runtime-discovered catalog of Stand skills compatible with {@link OriginEntity}.
 * Purely common logic (server + client safe, zero client imports).
 */
public final class OriginSkillCatalog {
    public enum SkillCategory {
        ALL("all"),
        STRIKE("strike"),
        BARRAGE("barrage"),
        HEAVY("heavy"),
        COUNTER("counter"),
        MOBILITY("mobility"),
        TIME_SPACE("time_space"),
        RANGED("ranged"),
        ULTIMATE("ultimate");

        private final String id;

        SkillCategory(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getTranslationKey() {
            return "jcraft.origin.cat." + id;
        }
    }

    public record SkillEntry(
            String id,
            StandType standType,
            Identifier standId,
            MoveClass sourceSlot,
            AbstractMove<?, ?> templateMove,
            MoveMap.Entry<?, ?> sourceEntry,
            SkillCategory category
    ) {
        public String getIconOwner() {
            return standId.getPath();
        }

        public String getIconName() {
            return switch (sourceSlot) {
                case LIGHT -> "light";
                case HEAVY -> "heavy";
                case BARRAGE -> "barrage";
                case SPECIAL1 -> "special1";
                case SPECIAL2 -> "special2";
                case SPECIAL3 -> "special3";
                case ULTIMATE -> "ult";
                case UTILITY, TOSS -> "utility";
            };
        }
    }

    public record LoadoutPreset(
            String id,
            String titleKey,
            Map<MoveClass, String> slots
    ) {}

    private static volatile List<SkillEntry> cachedCatalog = null;
    private static volatile Map<String, SkillEntry> cachedById = null;

    private static final List<LoadoutPreset> PRESETS = List.of(
            new LoadoutPreset("supreme_time_lord", "jcraft.origin.preset.supreme_time_lord", createSlotMap(
                    "jcraft:star_platinum#light",
                    "jcraft:the_world_over_heaven#heavy",
                    "jcraft:star_platinum#barrage",
                    "jcraft:the_world_over_heaven#special1",
                    "jcraft:the_world_over_heaven#special3",
                    "jcraft:king_crimson#ultimate",
                    "jcraft:the_world#ultimate",
                    "jcraft:king_crimson#utility",
                    "jcraft:the_world#toss"
            )),
            new LoadoutPreset("void_executioner", "jcraft.origin.preset.void_executioner", createSlotMap(
                    "jcraft:king_crimson#light",
                    "jcraft:the_world_over_heaven#heavy",
                    "jcraft:gold_experience_requiem#barrage",
                    "jcraft:star_platinum#special1",
                    "jcraft:king_crimson#special3",
                    "jcraft:gold_experience_requiem#special3",
                    "jcraft:killer_queen_bites_the_dust#ultimate",
                    "jcraft:king_crimson#utility",
                    "jcraft:star_platinum_the_world#toss"
            )),
            new LoadoutPreset("requiem_warden", "jcraft.origin.preset.requiem_warden", createSlotMap(
                    "jcraft:gold_experience_requiem#light",
                    "jcraft:star_platinum_the_world#heavy",
                    "jcraft:gold_experience_requiem#barrage",
                    "jcraft:gold_experience_requiem#special1",
                    "jcraft:gold_experience_requiem#special2",
                    "jcraft:gold_experience_requiem#special3",
                    "jcraft:gold_experience_requiem#ultimate",
                    "jcraft:gold_experience_requiem#utility",
                    "jcraft:gold_experience_requiem#toss"
            )),
            new LoadoutPreset("abyss_sovereign", "jcraft.origin.preset.abyss_sovereign", createSlotMap(
                    "jcraft:origin#light",
                    "jcraft:origin#heavy",
                    "jcraft:origin#barrage",
                    "jcraft:origin#special1",
                    "jcraft:origin#special2",
                    "jcraft:origin#special3",
                    "jcraft:origin#ultimate",
                    "jcraft:origin#utility",
                    "jcraft:origin#toss"
            ))
    );

    private OriginSkillCatalog() {}

    private static Map<MoveClass, String> createSlotMap(
            String light, String heavy, String barrage,
            String sp1, String sp2, String sp3,
            String ult, String utility, String toss) {
        EnumMap<MoveClass, String> map = new EnumMap<>(MoveClass.class);
        map.put(MoveClass.LIGHT, light);
        map.put(MoveClass.HEAVY, heavy);
        map.put(MoveClass.BARRAGE, barrage);
        map.put(MoveClass.SPECIAL1, sp1);
        map.put(MoveClass.SPECIAL2, sp2);
        map.put(MoveClass.SPECIAL3, sp3);
        map.put(MoveClass.ULTIMATE, ult);
        map.put(MoveClass.UTILITY, utility);
        map.put(MoveClass.TOSS, toss);
        return Collections.unmodifiableMap(map);
    }

    public static void invalidateCache() {
        cachedCatalog = null;
        cachedById = null;
    }

    public static List<LoadoutPreset> getPresets() {
        return PRESETS;
    }

    public static @Nullable LoadoutPreset getPresetById(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        for (LoadoutPreset preset : PRESETS) {
            if (preset.id().equals(id)) {
                return preset;
            }
        }
        return null;
    }

    public static String getDefaultId(MoveClass slot) {
        return "jcraft:origin#" + slot.getName();
    }

    public static synchronized List<SkillEntry> getAllSkills() {
        if (cachedCatalog != null) {
            return cachedCatalog;
        }
        buildCatalog();
        return cachedCatalog;
    }

    public static synchronized @Nullable SkillEntry getById(@Nullable String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (cachedById == null) {
            buildCatalog();
        }
        return cachedById.get(id);
    }

    private static void buildCatalog() {
        List<SkillEntry> list = new ArrayList<>();
        Map<String, SkillEntry> byId = new LinkedHashMap<>();

        List<StandType> stands = StandTypeUtil.streamAll()
                .filter(st -> !StandTypeUtil.isNone(st))
                .sorted(Comparator
                        .comparing((StandType st) -> st == JStandTypeRegistry.ORIGIN.get())
                        .thenComparing(st -> {
                            Identifier id = JRegistries.STAND_TYPE_REGISTRY.getId(st);
                            return id != null ? id.toString() : "";
                        }))
                .toList();

        for (StandType standType : stands) {
            Identifier standId = JRegistries.STAND_TYPE_REGISTRY.getId(standType);
            if (standId == null) {
                continue;
            }
            MoveSet<?, ?> moveSet = MoveSetManager.get(standType, "default");
            if (moveSet == null || moveSet.getMoveMap() == null) {
                continue;
            }
            MoveMap<?, ?> moveMap = moveSet.getMoveMap();
            for (MoveClass slot : MoveClass.values()) {
                Collection<? extends MoveMap.Entry<?, ?>> entries = moveMap.getEntries(slot);
                if (entries == null || entries.isEmpty()) {
                    continue;
                }
                MoveMap.Entry<?, ?> chosen = null;
                for (MoveMap.Entry<?, ?> candidate : entries) {
                    if (isMoveCompatibleWithOrigin(candidate.getMove())) {
                        chosen = candidate;
                        break;
                    }
                }
                if (chosen == null) {
                    continue;
                }
                String entryId = standId.toString() + "#" + slot.getName();
                SkillCategory category = categorize(slot, chosen.getMove());
                SkillEntry entry = new SkillEntry(entryId, standType, standId, slot, chosen.getMove(), chosen, category);
                list.add(entry);
                byId.put(entryId, entry);
            }
        }

        if (!list.isEmpty()) {
            cachedCatalog = Collections.unmodifiableList(list);
            cachedById = Collections.unmodifiableMap(byId);
        } else {
            // Do not permanently cache an empty catalog before movesets finish loading.
            cachedCatalog = Collections.emptyList();
            cachedById = Collections.emptyMap();
        }
    }

    public static List<SkillEntry> getSkillsForSlot(MoveClass slot, boolean freeBinds) {
        List<SkillEntry> all = getAllSkills();
        if (freeBinds) {
            return all;
        }
        List<SkillEntry> filtered = new ArrayList<>();
        for (SkillEntry entry : all) {
            if (entry.sourceSlot() == slot) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    public static @Nullable SkillEntry resolveForSlot(MoveClass slot, @Nullable String skillId, boolean freeBinds) {
        SkillEntry requested = getById(skillId);
        if (requested != null && (freeBinds || requested.sourceSlot() == slot)) {
            return requested;
        }
        SkillEntry fallback = getById(getDefaultId(slot));
        if (fallback != null) {
            return fallback;
        }
        List<SkillEntry> forSlot = getSkillsForSlot(slot, freeBinds);
        if (!forSlot.isEmpty()) {
            return forSlot.get(0);
        }
        List<SkillEntry> all = getAllSkills();
        return all.isEmpty() ? null : all.get(0);
    }

    public static boolean isUltimateSkill(@Nullable SkillEntry entry) {
        return entry != null && entry.sourceSlot() == MoveClass.ULTIMATE;
    }

    public static @Nullable MoveClass findSlotContaining(
            @Nullable Map<MoveClass, String> loadout,
            @Nullable MoveClass excludedSlot,
            @Nullable String candidateId) {
        if (loadout == null || candidateId == null || candidateId.isEmpty()) {
            return null;
        }
        for (MoveClass slot : MoveClass.values()) {
            if (slot == excludedSlot) {
                continue;
            }
            String existing = loadout.getOrDefault(slot, getDefaultId(slot));
            if (candidateId.equals(existing)) {
                return slot;
            }
        }
        return null;
    }

    public static boolean isDuplicateInOtherSlot(
            @Nullable Map<MoveClass, String> loadout,
            @Nullable MoveClass targetSlot,
            @Nullable String candidateId) {
        return findSlotContaining(loadout, targetSlot, candidateId) != null;
    }

    public static int countUltimatesExcluding(
            @Nullable Map<MoveClass, String> loadout,
            @Nullable MoveClass excludedSlot,
            boolean freeBinds) {
        int count = 0;
        for (MoveClass slot : MoveClass.values()) {
            if (slot == excludedSlot) {
                continue;
            }
            String id = loadout != null ? loadout.getOrDefault(slot, getDefaultId(slot)) : getDefaultId(slot);
            SkillEntry resolved = resolveForSlot(slot, id, freeBinds);
            if (isUltimateSkill(resolved)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Resolves and sanitizes a full 9-slot Origin loadout, deduplicating cross-slot repeats
     * and enforcing the configurable maximum Ultimate count (dedicated ULTIMATE slot has priority).
     */
    public static EnumMap<MoveClass, SkillEntry> sanitizeLoadout(
            @Nullable Map<MoveClass, String> rawSlots,
            boolean freeBinds,
            int maxUltimates) {
        EnumMap<MoveClass, SkillEntry> result = new EnumMap<>(MoveClass.class);
        Set<String> usedIds = new HashSet<>();
        int ultCount = 0;

        // Prioritize the dedicated ULTIMATE bind first so it is never crowded out by earlier slots.
        List<MoveClass> ordered = new ArrayList<>(MoveClass.values().length);
        ordered.add(MoveClass.ULTIMATE);
        for (MoveClass mc : MoveClass.values()) {
            if (mc != MoveClass.ULTIMATE) {
                ordered.add(mc);
            }
        }

        for (MoveClass slot : ordered) {
            String rawId = rawSlots != null ? rawSlots.get(slot) : null;
            SkillEntry candidate = resolveForSlot(slot, rawId, freeBinds);
            if (candidate == null) {
                continue;
            }
            boolean duplicate = usedIds.contains(candidate.id());
            boolean exceedsUlts = maxUltimates > 0 && isUltimateSkill(candidate) && ultCount >= maxUltimates;
            if (duplicate || exceedsUlts) {
                SkillEntry defEntry = resolveForSlot(slot, getDefaultId(slot), freeBinds);
                if (defEntry != null && !usedIds.contains(defEntry.id())
                        && !(maxUltimates > 0 && isUltimateSkill(defEntry) && ultCount >= maxUltimates)) {
                    candidate = defEntry;
                } else {
                    SkillEntry replacement = findFirstSafeFallback(slot, freeBinds, usedIds, maxUltimates, ultCount);
                    if (replacement != null) {
                        candidate = replacement;
                    }
                }
            }
            usedIds.add(candidate.id());
            if (isUltimateSkill(candidate)) {
                ultCount++;
            }
            result.put(slot, candidate);
        }
        return result;
    }

    private static @Nullable SkillEntry findFirstSafeFallback(
            MoveClass slot,
            boolean freeBinds,
            Set<String> usedIds,
            int maxUltimates,
            int currentUltCount) {
        for (SkillEntry entry : getSkillsForSlot(slot, freeBinds)) {
            if (usedIds.contains(entry.id())) {
                continue;
            }
            if (maxUltimates > 0 && isUltimateSkill(entry) && currentUltCount >= maxUltimates) {
                continue;
            }
            return entry;
        }
        return null;
    }

    public static boolean isMoveCompatibleWithOrigin(@Nullable AbstractMove<?, ?> move) {
        if (move == null) {
            return false;
        }
        if (move.getAttackerClass() != null && !move.getAttackerClass().isAssignableFrom(OriginEntity.class)) {
            return false;
        }
        for (MoveAction<?, ?> action : move.getActions()) {
            if (action.getAttackerClass() != null && !action.getAttackerClass().isAssignableFrom(OriginEntity.class)) {
                return false;
            }
        }
        for (MoveCondition<?, ?> condition : move.getConditions()) {
            if (condition.getAttackerClass() != null && !condition.getAttackerClass().isAssignableFrom(OriginEntity.class)) {
                return false;
            }
        }
        if (move.getFollowup() != null && !isMoveCompatibleWithOrigin(move.getFollowup())) {
            return false;
        }
        if (move.getAerialVariant() != null && !isMoveCompatibleWithOrigin(move.getAerialVariant())) {
            return false;
        }
        if (move.getCrouchingVariant() != null && !isMoveCompatibleWithOrigin(move.getCrouchingVariant())) {
            return false;
        }
        if (move.getFinisher() != null && !isMoveCompatibleWithOrigin(move.getFinisher().right())) {
            return false;
        }
        if (move instanceof AbstractGrabAttack<?, ?, ?> grab && grab.getHitMove() != null
                && !isMoveCompatibleWithOrigin(grab.getHitMove())) {
            return false;
        }
        return true;
    }

    private static SkillCategory categorize(MoveClass slot, AbstractMove<?, ?> move) {
        if (move instanceof AbstractTimeStopMove || move instanceof TimeEraseMove
                || move instanceof TimeAccelerationMove || move instanceof TimeSkipMove) {
            return SkillCategory.TIME_SPACE;
        }
        if (move instanceof AbstractCounterAttack) {
            return SkillCategory.COUNTER;
        }
        if (move instanceof AbstractBarrageAttack) {
            return SkillCategory.BARRAGE;
        }
        if (move.isRanged()) {
            return SkillCategory.RANGED;
        }
        if (slot == MoveClass.ULTIMATE) {
            return SkillCategory.ULTIMATE;
        }
        if (slot == MoveClass.UTILITY) {
            return SkillCategory.MOBILITY;
        }
        if (slot == MoveClass.HEAVY || move instanceof AbstractUppercutAttack
                || move instanceof OverwriteAttack || move instanceof ChargeOverwriteMove) {
            return SkillCategory.HEAVY;
        }
        return SkillCategory.STRIKE;
    }

    public static OriginEntity.State pickAnimState(MoveClass targetSlot, @Nullable AbstractMove<?, ?> move) {
        if (move instanceof AbstractTimeStopMove) {
            return OriginEntity.State.TIME_STOP;
        }
        if (move instanceof ChargeOverwriteMove || move instanceof OverwriteAttack) {
            return OriginEntity.State.OVERWRITE;
        }
        if (move instanceof AbstractCounterAttack) {
            return OriginEntity.State.COUNTER;
        }
        if (move instanceof AbstractGrabAttack) {
            return OriginEntity.State.GRAB;
        }
        if (move instanceof AbstractBarrageAttack) {
            return OriginEntity.State.BARRAGE;
        }
        if (move instanceof AbstractUppercutAttack) {
            return OriginEntity.State.UPPERCUT;
        }
        if (move instanceof TossChargeMove) {
            return OriginEntity.State.ITEM_TOSS_CHARGE;
        }
        if (move instanceof TossMove) {
            return OriginEntity.State.ITEM_TOSS;
        }
        if (move != null && move.isFollowup() && targetSlot == MoveClass.LIGHT) {
            return OriginEntity.State.LIGHT_FOLLOWUP;
        }
        if (move != null && move.isCrouchingVariant() && targetSlot == MoveClass.HEAVY) {
            return OriginEntity.State.GRAB_HIT;
        }
        return defaultStateForSlot(targetSlot);
    }

    public static OriginEntity.State defaultStateForSlot(MoveClass slot) {
        return switch (slot) {
            case LIGHT -> OriginEntity.State.LIGHT;
            case HEAVY -> OriginEntity.State.HEAVY;
            case BARRAGE -> OriginEntity.State.BARRAGE;
            case SPECIAL1 -> OriginEntity.State.SPECIAL1;
            case SPECIAL2 -> OriginEntity.State.SPECIAL2;
            case SPECIAL3 -> OriginEntity.State.SPECIAL3;
            case ULTIMATE -> OriginEntity.State.ULTIMATE;
            case UTILITY -> OriginEntity.State.UTILITY;
            case TOSS -> OriginEntity.State.ITEM_TOSS_CHARGE;
        };
    }

    public static void warmUpIfNeeded() {
        List<SkillEntry> all = getAllSkills();
        if (all.isEmpty()) {
            JCraft.LOGGER.debug("OriginSkillCatalog warmUp deferred until movesets finish loading");
        }
    }
}
