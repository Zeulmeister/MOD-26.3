package net.arna.jcraft.client.renderer.item;
import net.arna.jcraft.common.util.JItemData;
import net.arna.jcraft.common.util.JValueIO;

import mod.azure.azurelib.AzureLib;
import mod.azure.azurelib.animation.controller.AzAnimationController;
import mod.azure.azurelib.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.animation.dispatch.AzDispatchSide;
import mod.azure.azurelib.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.animation.impl.AzItemAnimator;
import mod.azure.azurelib.animation.play_behavior.AzPlayBehaviors;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.item.Peacemaker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class PeacemakerAnimator extends AzItemAnimator {
    private static final Identifier ANIMATION = JCraft.id("animations/peacemaker.animation.json");

    private AzAnimationController<ItemStack> baseController, fireController;
    private UUID lastStackId;
    private long lastSequence;
    private boolean sequenceInitialized;

    @Override
    public void registerControllers(AzAnimationControllerContainer<ItemStack> container) {
        baseController = AzAnimationController.builder(this, JCraft.BASE_CONTROLLER).build();
        container.add(baseController);

        fireController = AzAnimationController.builder(this, JCraft.FIRE_CONTROLLER).build();
        container.add(fireController);
    }

    @Override
    public Identifier getAnimationLocation(ItemStack stack) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(ItemStack stack, float partialTicks) {
        if (baseController == null) {
            return;
        }

        // Read-only: this runs every frame, so it must not attach a tag to a freshly crafted gun.
        final CompoundTag data = JItemData.getTag(stack);
        if (data == null) {
            return;
        }

        final UUID stackId = stackId(data);
        final long sequence = data.getLongOr(Peacemaker.ANIMATION_SEQUENCE_ID, 0L);

        // Adopt the sequence without playing anything the first time a stack is seen, otherwise
        // swapping to an already-fired revolver replays whatever it did last.
        if (!sequenceInitialized || (stackId != null && !stackId.equals(lastStackId))) {
            sequenceInitialized = true;
            lastStackId = stackId;
            lastSequence = sequence;
            return;
        }

        if (sequence == lastSequence) {
            return;
        }

        lastSequence = sequence;

        final String animation = data.getStringOr(Peacemaker.ANIMATION_ID, "");
        if (animation.isEmpty()) {
            return;
        }

        dispatch(AzCommand.create(JCraft.BASE_CONTROLLER, animation, AzPlayBehaviors.HOLD_ON_LAST_FRAME));
    }

    private void dispatch(AzCommand command) {
        command.actions().forEach(action -> action.handle(AzDispatchSide.CLIENT, this));
    }

    public boolean isFireEffectVisible() {
        final var queuedAnim = fireController.currentAnimation();
        if (queuedAnim == null) return false;
        final var anim = queuedAnim.animation();
        if (!fireController.stateMachine().isPlaying()) return false;
        return "fire".equals(anim.name());
    }

    private static UUID stackId(CompoundTag data) {
        return JValueIO.hasUUID(data, "AzItemUuid") ? JValueIO.getUUID(data, "AzItemUuid") : null;
    }
}
