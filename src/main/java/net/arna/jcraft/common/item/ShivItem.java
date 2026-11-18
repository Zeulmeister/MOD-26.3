package net.arna.jcraft.common.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public class ShivItem extends Item {
    public ShivItem(ToolMaterial tier, Properties properties) {
        super(properties.sword(tier, 1.0f, -1.8f));
    }
}
