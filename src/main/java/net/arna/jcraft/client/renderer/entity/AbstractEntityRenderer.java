package net.arna.jcraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.NonNull;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.render.layer.AzBlockAndItemLayer;
import net.arna.jcraft.JCraft;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public abstract class AbstractEntityRenderer<T extends Entity> extends AzEntityRenderer<T> {

    public static final String MODEL_STR_TEMPLATE = "geo/%s.geo.json";
    public static final String TEXTURE_STR_TEMPLATE = "textures/entity/%s.png";
    public static final String ANIMATION_STR_TEMPLATE = "animations/%s.animation.json";

    protected AbstractEntityRenderer(final @NonNull AzEntityRendererConfig<T> config, final @NonNull EntityRendererProvider.Context context) {
        super(config, context);
    }

    protected AbstractEntityRenderer(final @NonNull EntityRendererProvider.Context context, final @NonNull Supplier<AzAnimator<UUID, T>> animatorSupplier,
                                     final @NonNull Identifier model, final @NonNull Identifier texture) {
        this(AzEntityRendererConfig.<T>builder(model, texture).setAnimatorProvider(animatorSupplier).build(), context);
    }

    protected AbstractEntityRenderer(final @NonNull EntityRendererProvider.Context context, final @NonNull Supplier<AzAnimator<UUID, T>> animatorSupplier, final @NonNull String id) {
        this(context, animatorSupplier, JCraft.id(MODEL_STR_TEMPLATE.formatted(id)), JCraft.id(TEXTURE_STR_TEMPLATE.formatted(id)));
    }

    protected AbstractEntityRenderer(final @NonNull EntityRendererProvider.Context context,
                                     final @NonNull Supplier<AzAnimator<UUID, T>> animatorSupplier,
                                     final @NonNull Function<AzEntityRendererConfig.Builder<T>, AzEntityRendererConfig.Builder<T>> additionalConfigs,
                                     final @NonNull Identifier model, final @NonNull Identifier texture) {
        this(additionalConfigs.apply(AzEntityRendererConfig.<T>builder(model, texture).setAnimatorProvider(animatorSupplier)).build(), context);
    }

    protected AbstractEntityRenderer(final @NonNull EntityRendererProvider.Context context,
                                     final @NonNull Supplier<AzAnimator<UUID, T>> animatorSupplier,
                                     final @NonNull Function<AzEntityRendererConfig.Builder<T>, AzEntityRendererConfig.Builder<T>> additionalConfigs,
                                     final @NonNull String id) {
        this(additionalConfigs.apply(AzEntityRendererConfig.<T>builder(JCraft.id(MODEL_STR_TEMPLATE.formatted(id)),
                        JCraft.id(TEXTURE_STR_TEMPLATE.formatted(id))).setAnimatorProvider(animatorSupplier)).build(),
                context);
    }

    public static class EntityAnimator<T extends Entity> extends AzEntityAnimator<T> {
        protected final @NonNull Identifier animation;

        public EntityAnimator(final @NonNull Identifier animation) {
            this.animation = animation;
        }

        public EntityAnimator(final @NonNull String id) {
            this(JCraft.id(ANIMATION_STR_TEMPLATE.formatted(id)));
        }

        @Override
        public void registerControllers(final @NonNull AzAnimationControllerContainer<T> animationControllerContainer) {
            animationControllerContainer.add(AzAnimationController.builder(this, JCraft.BASE_CONTROLLER).setTransitionLength(0).build());
        }

        @Override
        public @NotNull Identifier getAnimationLocation(@Nullable final T animatable) {
            return animation;
        }
    }

    protected static final String LEFT_HAND = "bipedHandLeft";
    protected static final String RIGHT_HAND = "bipedHandRight";

    public static class HandItemsRenderLayer<T extends Mob> extends AzBlockAndItemLayer<UUID, T> {
        protected ItemStack mainHandItem = ItemStack.EMPTY;
        protected ItemStack offHandItem = ItemStack.EMPTY;

        @Override
        public void preRender(final AzRendererPipelineContext<UUID, T> context) {
            mainHandItem = context.animatable().getMainHandItem();
            offHandItem = context.animatable().getOffhandItem();
        }

        @Override
        public ItemStack itemStackForBone(final AzBone bone, final T animatable) {
            return switch (bone.getName()) {
                case LEFT_HAND -> animatable.isLeftHanded() ? mainHandItem : offHandItem;
                case RIGHT_HAND -> animatable.isLeftHanded() ? offHandItem : mainHandItem;
                default -> null;
            };
        }

        @Override
        protected ItemDisplayContext getTransformTypeForStack(final AzBone bone, final ItemStack stack, final T animatable) {
            return switch (bone.getName()) {
                case LEFT_HAND -> ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
                case RIGHT_HAND -> ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
                default -> ItemDisplayContext.NONE;
            };
        }

        @Override
        protected void renderItemForBone(final AzRendererPipelineContext<UUID, T> context, final AzBone bone, final ItemStack itemStack, final T animatable) {
            final PoseStack poseStack = context.poseStack();
            if (itemStack == mainHandItem) {
                poseStack.rotate(Axis.XP.rotationDegrees(-90f));
                if (itemStack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, -0.25);
                }
            } else if (itemStack == offHandItem) {
                poseStack.rotate(Axis.XP.rotationDegrees(-90f));
                if (itemStack.getItem() instanceof ShieldItem) {
                    poseStack.translate(0, 0.125, 0.25);
                    poseStack.rotate(Axis.YP.rotationDegrees(180));
                }
            }
            super.renderItemForBone(context, bone, itemStack, animatable);
        }

        protected void superRenderItemForBone(final AzRendererPipelineContext<UUID, T> context, final AzBone bone, final ItemStack itemStack, final T animatable) {
            super.renderItemForBone(context, bone, itemStack, animatable);
        }

        protected float getItemAlpha(final AzRendererPipelineContext<UUID, T> context, final T animatable) {
            return 1f;
        }
    }
}
