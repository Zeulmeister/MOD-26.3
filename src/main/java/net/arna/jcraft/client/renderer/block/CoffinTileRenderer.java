package net.arna.jcraft.client.renderer.block;

import mod.azure.azurelib.animation.AzAnimatorConfig;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzBlockAnimator;
import mod.azure.azurelib.render.block.AzBlockEntityRenderer;
import mod.azure.azurelib.render.block.AzBlockEntityRendererConfig;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.block.tile.CoffinTileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class CoffinTileRenderer extends AzBlockEntityRenderer<CoffinTileEntity> {

    public static final Identifier GEO = JCraft.id("geo/coffin.geo.json");
    public static final Identifier TEXTURE = JCraft.id("textures/block/coffin.png");
    public static final Identifier ANIMATION = JCraft.id("animations/coffin.animation.json");

    public CoffinTileRenderer() {
        super(AzBlockEntityRendererConfig.<CoffinTileEntity>builder(GEO, TEXTURE)
                .setAnimatorProvider(CoffinAnimator::new)
                .setScale(tile -> BedPart.HEAD == tile.getBlockState().getValue(BedBlock.PART) ? 0f : 1f)
                .build());
    }

    public static class CoffinAnimator extends AzBlockAnimator<CoffinTileEntity> {

        public CoffinAnimator() {
            super(AzAnimatorConfig.defaultConfig());
        }

        @Override
        public void registerControllers(AzAnimationControllerContainer<CoffinTileEntity> animationControllerContainer) {
            animationControllerContainer.add(AzAnimationController.builder(this, JCraft.BASE_CONTROLLER).setTransitionLength(30).build());
        }

        @Override
        public @NotNull Identifier getAnimationLocation(CoffinTileEntity animatable) {
            return ANIMATION;
        }
    }
}
