package net.arna.jcraft.mixin.client;

import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.component.living.CommonVampireComponent;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.gui.hud.EpitaphOverlay;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public class GuiMixin {
    @Unique
    private static final Identifier JCRAFT_BLOOD_EMPTY = JCraft.id("textures/gui/blood_empty.png");
    @Unique
    private static final Identifier JCRAFT_BLOOD_HALF = JCraft.id("textures/gui/blood_half.png");
    @Unique
    private static final Identifier JCRAFT_BLOOD_FULL = JCraft.id("textures/gui/blood_full.png");

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void jcraft$renderEpitaphOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (JClientConfig.getInstance().isEpitaphOverlay()) {
            EpitaphOverlay.render(graphics);
        }
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void jcraft$renderVampireBloodIcons(GuiGraphicsExtractor graphics, Player player, int y, int x, CallbackInfo ci) {
        if (player == null) {
            return;
        }
        CommonVampireComponent vamp = JComponentPlatformUtils.getVampirism(player);
        if (vamp == null || !vamp.isVampire()) {
            return;
        }
        int blood = Math.max(0, Math.min(20, (int) Math.floor(vamp.getBlood())));
        for (int i = 0; i < 10; i++) {
            int iconX = x - i * 8 - 9;
            int iconY = y;
            graphics.blit(RenderPipelines.GUI_TEXTURED, JCRAFT_BLOOD_EMPTY, iconX, iconY, 0.0F, 0.0F, 9, 9, 9, 9);
            int threshold = i * 2 + 1;
            if (threshold < blood) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, JCRAFT_BLOOD_FULL, iconX, iconY, 0.0F, 0.0F, 9, 9, 9, 9);
            } else if (threshold == blood) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, JCRAFT_BLOOD_HALF, iconX, iconY, 0.0F, 0.0F, 9, 9, 9, 9);
            }
        }
        ci.cancel();
    }
}
