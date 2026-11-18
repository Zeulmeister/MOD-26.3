package net.arna.jcraft.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.arna.jcraft.mixin_logic.StillDepthHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements StillDepthHolder {

    @Unique
    private int jcraft$depthTextureId = -1;

    @Override
    public int jcraft$getDepthTexture() {
        return jcraft$depthTextureId;
    }

    @Override
    public void jcraft$freezeDepth() {
        // RenderPearl 26.3 manages depth targets automatically via PostChain targets.
    }
}
