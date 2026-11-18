package net.arna.jcraft.client.renderer.entity;

import lombok.NonNull;
import net.arna.jcraft.common.entity.PlayerCloneEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class PlayerCloneRenderer extends HumanoidMobRenderer<PlayerCloneEntity, AvatarRenderState, PlayerModel> {

    public PlayerCloneRenderer(final EntityRendererProvider.Context ctx, final boolean slim) {
        super(ctx, new PlayerModel(ctx.bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER), slim), 0.5f);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public boolean shouldRender(final PlayerCloneEntity clone, final @NonNull Frustum frustum, final double d, final double e, final double f, final float partialTick) {
        final boolean s = super.shouldRender(clone, frustum, d, e, f, partialTick);
        if (clone.shouldRenderForMaster()) {
            return s;
        }
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && clone.getMaster() == player) {
            return false;
        }
        return s;
    }

    @Override
    public @NonNull Identifier getTextureLocation(final @NonNull AvatarRenderState state) {
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
