package net.arna.jcraft.client.renderer.entity;

import lombok.NonNull;
import net.arna.jcraft.client.rendering.CloneSkinTracker;
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
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.player.PlayerSkin;

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
    public void extractRenderState(final @NonNull PlayerCloneEntity clone, final @NonNull AvatarRenderState state, final float partialTick) {
        super.extractRenderState(clone, state, partialTick);
        PlayerSkin skin = CloneSkinTracker.getPlayerSkinFor(clone);
        if (skin == null) {
            skin = clone.getMasterId() != null
                    ? DefaultPlayerSkin.get(clone.getMasterId())
                    : DefaultPlayerSkin.getDefaultSkin();
        }
        state.skin = skin;
        state.id = clone.getId();
        state.arrowCount = clone.getArrowCount();
        state.stingerCount = clone.getStingerCount();
        state.showCape = false;
        if (clone.getMasterId() != null) {
            byte mask = clone.getPartMask();
            state.showHat = (mask & PlayerModelPart.HAT.getMask()) != 0;
            state.showJacket = (mask & PlayerModelPart.JACKET.getMask()) != 0;
            state.showLeftPants = (mask & PlayerModelPart.LEFT_PANTS_LEG.getMask()) != 0;
            state.showRightPants = (mask & PlayerModelPart.RIGHT_PANTS_LEG.getMask()) != 0;
            state.showLeftSleeve = (mask & PlayerModelPart.LEFT_SLEEVE.getMask()) != 0;
            state.showRightSleeve = (mask & PlayerModelPart.RIGHT_SLEEVE.getMask()) != 0;
        }
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
        return state.skin != null ? state.skin.body().texturePath() : DefaultPlayerSkin.getDefaultTexture();
    }
}
