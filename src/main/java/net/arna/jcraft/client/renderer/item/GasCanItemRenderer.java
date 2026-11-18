package net.arna.jcraft.client.renderer.item;

import mod.azure.azurelib.render.item.AzItemRenderer;
import mod.azure.azurelib.render.item.AzItemRendererConfig;
import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;

public class GasCanItemRenderer extends AzItemRenderer {
    private static final Identifier model = JCraft.id("geo/canister.geo.json");
    private static final Identifier texture = JCraft.id("textures/item/gascan.png");

    public GasCanItemRenderer() {
        super(AzItemRendererConfig.builder(model, texture).useNewOffset(true).build());
    }
}
