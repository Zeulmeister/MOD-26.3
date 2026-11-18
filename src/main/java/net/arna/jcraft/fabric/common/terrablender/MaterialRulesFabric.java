package net.arna.jcraft.fabric.common.terrablender;

import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.material.rule.SequenceRule;

import java.util.List;

public class MaterialRulesFabric {
    public static MaterialRule makeRules() {
        return new SequenceRule(List.of());
    }
}
