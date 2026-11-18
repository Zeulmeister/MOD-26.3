package net.arna.jcraft.api.registry;

import net.arna.jcraft.common.advancements.*;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public interface JAdvancementTriggerRegistry {
    ObtainedStandTrigger OBTAINED_STAND = register(ObtainedStandTrigger.ID, new ObtainedStandTrigger());
    ObtainedSpecTrigger OBTAINED_SPEC = register(ObtainedSpecTrigger.ID, new ObtainedSpecTrigger());
    Hamon1Trigger HAMON1 = register(Hamon1Trigger.ID, new Hamon1Trigger());
    Hamon2Trigger HAMON2 = register(Hamon2Trigger.ID, new Hamon2Trigger());
    Hamon3Trigger HAMON3 = register(Hamon3Trigger.ID, new Hamon3Trigger());
    Hamon4Trigger HAMON4 = register(Hamon4Trigger.ID, new Hamon4Trigger());
    Hamon5Trigger HAMON5 = register(Hamon5Trigger.ID, new Hamon5Trigger());
    Hamon6Trigger HAMON6 = register(Hamon6Trigger.ID, new Hamon6Trigger());

    private static <T extends CriterionTrigger<?>> T register(Identifier id, T trigger) {
        return Registry.register(BuiltInRegistries.TRIGGER_TYPES, id, trigger);
    }

    static void init() {}
}
