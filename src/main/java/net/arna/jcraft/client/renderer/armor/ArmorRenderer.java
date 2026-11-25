package net.arna.jcraft.client.renderer.armor;

import lombok.NonNull;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.impl.AzItemAnimator;
import mod.azure.azurelib.render.armor.AzArmorRenderer;
import mod.azure.azurelib.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.render.armor.AzArmorRendererPipelineContext;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneContext;
import mod.azure.azurelib.render.armor.bone.AzArmorBoneProvider;
import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

// NOTE: Retained as lightweight wrapper over AzArmorRenderer for Stone Mask and Red Hat.
public class ArmorRenderer extends AzArmorRenderer {

    protected static final String MODEL_STR_TEMPLATE = "geo/%s.geo.json";
    protected static final String TEXTURE_STR_TEMPLATE = "textures/armor/%s.png";
    protected static final String ANIMATION_STR_TEMPLATE = "animations/%s.animation.json";
    private static final AzArmorBoneProvider BONE_PROVIDER = new ArmorBoneProvider();

    /**
     * Path to the model to be used for this armor.
     */
    protected final @NonNull Identifier model;
    /**
     * Path to the texture to be used for this armor.
     */
    protected final @NonNull Identifier texture;

    /**
     * Constructs a renderer with a fully customizable config and the given model/texture paths.
     */
    protected ArmorRenderer(final @NonNull AzArmorRendererConfig config, final @NonNull Identifier model, final @NonNull Identifier texture) {
        super(config);
        this.model = model;
        this.texture = texture;
    }

    /**
     * Constructs a renderer with a simple config based on the {@link AzAnimator} {@link Supplier} and the given model/texture paths.
     */
    protected ArmorRenderer(final @NonNull Supplier<AzAnimator<UUID, ItemStack>> animatorSupplier, final @NonNull Identifier model, final @NonNull Identifier texture) {
        this(animatorSupplier, UnaryOperator.identity(), model, texture);
    }

    /**
     * Constructs a renderer with a simple config based on the {@link AzAnimator} {@link Supplier} and model/texture paths based on the specified ID.
     *
     * <ul>
     * <li>Resulting model path will be equivalent to <code>JCraft.id("geo/" + id + ".geo.json")</code></li>
     * <li>Resulting texture path will be equivalent to <code>JCraft.id("textures/armor/" + id + ".png")</code></li>
     * </ul>
     */
    protected ArmorRenderer(final @NonNull Supplier<AzAnimator<UUID, ItemStack>> animatorSupplier, final @NonNull String id) {
        this(animatorSupplier, JCraft.id(MODEL_STR_TEMPLATE.formatted(id)), JCraft.id(TEXTURE_STR_TEMPLATE.formatted(id)));
    }

    /**
     * Constructs a renderer with a config based on the {@link AzAnimator} {@link Supplier} and the {@link mod.azure.azurelib.render.entity.AzEntityRendererConfig.Builder} {@link Function}, and the given model/texture paths.
     */
    protected ArmorRenderer(final @NonNull Supplier<AzAnimator<UUID, ItemStack>> animatorSupplier, final @NonNull Function<AzArmorRendererConfig.Builder, AzArmorRendererConfig.Builder> additionalConfigs, final @NonNull Identifier model, final @NonNull Identifier texture) {
        this(animatorSupplier, new AzArmorBoneContext(), additionalConfigs, model, texture);
    }

    /**
     * Constructs a renderer with a config based on the {@link AzAnimator} {@link Supplier}, given bone context and the {@link mod.azure.azurelib.render.entity.AzEntityRendererConfig.Builder} {@link Function}, and the given model/texture paths.
     */
    protected ArmorRenderer(final @NonNull Supplier<AzAnimator<UUID, ItemStack>> animatorSupplier, final @NonNull AzArmorBoneContext boneContext, final @NonNull Function<AzArmorRendererConfig.Builder, AzArmorRendererConfig.Builder> additionalConfigs, final @NonNull Identifier model, final @NonNull Identifier texture) {
        this(additionalConfigs.apply(
                AzArmorRendererConfig.builder(model, texture)
                        .setAnimatorProvider(animatorSupplier)
                        .setBoneProvider(BONE_PROVIDER)
                        .setPipelineContext(pipeline -> new AzArmorRendererPipelineContext(pipeline) {
                            private final AzArmorBoneContext ourContext = boneContext;

                            @Override
                            public AzArmorBoneContext boneContext() {
                                return ourContext;
                            }
                        })
                )
                .build(), model, texture);
    }

    /**
     * Returns a supplier for a simple {@link ArmorRenderer}, only identified by its ID.
     */
    public static Supplier<AzArmorRenderer> simple(final String id) {
        return () -> new ArmorRenderer(() -> new ArmorAnimator(id), id);
    }

    /**
     * Basic {@link AzItemAnimator} implementation that can be used or extended for all kinds of {@link net.minecraft.world.item.ArmorItem} animators.
     */
    public static class ArmorAnimator extends AzItemAnimator {

        /**
         * Path to the animation to be used for this entity.
         */
        protected final @NonNull Identifier animation;

        /**
         * Constructs an animator with the given animation path.
         */
        public ArmorAnimator(final @NonNull Identifier animation) {
            this.animation = animation;
        }

        /**
         * Constructs an animator with the given ID.
         * <p>
         * Resulting animation path will be equivalent to <code>JCraft.id("animations/" + id + ".animation.json")</code>
         */
        public ArmorAnimator(final @NonNull String id) {
            this(JCraft.id(ANIMATION_STR_TEMPLATE.formatted(id)));
        }

        @Override
        public void registerControllers(final @NonNull AzAnimationControllerContainer<ItemStack> animationControllerContainer) {
            animationControllerContainer.add(AzAnimationController.builder(this, JCraft.BASE_CONTROLLER).build());
        }

        @Override
        public @NotNull Identifier getAnimationLocation(final @NonNull ItemStack animatable) {
            return animation;
        }
    }

}
