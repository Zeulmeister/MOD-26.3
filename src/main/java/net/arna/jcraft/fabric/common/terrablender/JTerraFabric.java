package net.arna.jcraft.fabric.common.terrablender;

import net.arna.jcraft.JCraft;
import terrablender.api.Regions;
import terrablender.api.TerraBlenderApi;

public class JTerraFabric implements TerraBlenderApi {
    @Override
    public void onTerraBlenderInitialized() {
        Regions.register(new OverworldRegionFabric(JCraft.id("overworld"), 4));
    }

    public static void onModInitialized() {}
}
