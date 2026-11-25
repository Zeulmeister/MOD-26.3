package net.arna.jcraft.client.renderer.entity.npc;

import lombok.NonNull;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.model.AzBakedModel;
import mod.azure.azurelib.model.AzBone;
import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.layer.AzArmorLayer;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.spec.SpecUserMob;
import net.arna.jcraft.client.renderer.entity.AbstractEntityRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SpecUserRenderer<T extends SpecUserMob> extends AbstractEntityRenderer<T> {

    protected static final Identifier GENERIC_ANIMATIONS = JCraft.id("animations/spec/spec_user.animation.json");

    public SpecUserRenderer(final @NonNull EntityRendererProvider.Context context, final @NonNull Identifier model, final @NonNull Identifier texture) {
        super(context, () -> new SpecUserAnimator<>(GENERIC_ANIMATIONS),
                b -> b
                        .addRenderLayer(new HandItemsRenderLayer<>())
                        .addRenderLayer(new SpecUserArmorLayer<>(context))
                        .setRenderEntry((pc) -> {
                            pc.animatable().updateAnimations();
                            return pc;
                        }),
                model,
                texture
        );
    }

    /**
     * Renders equipped vanilla and AzureLib armor (helmet/masks, chestplate, leggings, boots)
     * directly onto SpecUserMob geo bones.
     */
    public static class SpecUserArmorLayer<T extends SpecUserMob> extends AzArmorLayer<T> {
        public SpecUserArmorLayer(final EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        protected @Nullable ItemStack getArmorItemForBone(final AzRendererPipelineContext<UUID, T> context, final AzBone bone) {
            if (bone == null || bone.getCubes().isEmpty()) {
                return null;
            }
            final ItemStack candidate = switch (bone.getName()) {
                case "head" -> helmetStack;
                case "body", "rightArm", "leftArm" -> chestplateStack;
                case "rightLeg", "leftLeg" -> leggingsStack;
                case "lower_legRight", "lower_legLeft" -> bootsStack;
                default -> null;
            };
            return (candidate == null || candidate.isEmpty()) ? null : candidate;
        }

        @Override
        protected EquipmentSlot getEquipmentSlotForBone(final AzRendererPipelineContext<UUID, T> context,
                                                        final AzBone bone, final ItemStack stack) {
            return switch (bone.getName()) {
                case "head" -> EquipmentSlot.HEAD;
                case "body", "rightArm", "leftArm" -> EquipmentSlot.CHEST;
                case "rightLeg", "leftLeg" -> EquipmentSlot.LEGS;
                case "lower_legRight", "lower_legLeft" -> EquipmentSlot.FEET;
                default -> super.getEquipmentSlotForBone(context, bone, stack);
            };
        }

        @Override
        protected ModelPart getModelPartForBone(final AzRendererPipelineContext<UUID, T> context,
                                                final AzBone bone, final HumanoidModel<?> baseModel) {
            return switch (bone.getName()) {
                case "head" -> baseModel.head;
                case "body" -> baseModel.body;
                case "rightArm" -> baseModel.rightArm;
                case "leftArm" -> baseModel.leftArm;
                case "rightLeg", "lower_legRight" -> baseModel.rightLeg;
                case "leftLeg", "lower_legLeft" -> baseModel.leftLeg;
                default -> super.getModelPartForBone(context, bone, baseModel);
            };
        }
    }

    private static class SpecUserAnimator<T extends SpecUserMob> extends EntityAnimator<T> {
        public SpecUserAnimator(@NonNull Identifier animation) {
            super(animation);
        }

        @Override
        public void registerControllers(@NonNull AzAnimationControllerContainer<T> animationControllerContainer) {
            animationControllerContainer.add(AzAnimationController.builder(this, SpecUserMob.MOVEMENT_CONTROLLER).setTransitionLength(0).build());
            animationControllerContainer.add(AzAnimationController.builder(this, JCraft.BASE_CONTROLLER).setTransitionLength(0).build());
        }

        @Override
        public void setCustomAnimations(final T animatable, final float partialTicks) {
            final AzBakedModel model = context().boneCache().getBakedModel();
            final AzBone head = model.getBoneOrNull("head");

            if (head != null) {
                head.setRotX(-animatable.getXRot() * Mth.DEG_TO_RAD);
                head.setRotY((animatable.getYRot() - animatable.getViewYRot(partialTicks)) * Mth.DEG_TO_RAD);
            }
        }
    }
}
