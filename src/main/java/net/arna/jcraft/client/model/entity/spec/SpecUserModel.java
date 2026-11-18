package net.arna.jcraft.client.model.entity.spec;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.api.spec.SpecUserMob;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public abstract class SpecUserModel<T extends SpecUserMob> {
    protected static final Identifier GENERIC_ANIMATIONS = JCraft.id("animations/spec/spec_user.animation.json");

    /*@Override
    public Identifier getAnimationResource(final T animatable) {
        return GENERIC_ANIMATIONS;
    }

    @Override
    public void setCustomAnimations(final @NonNull T animatable, final long instanceId, final AnimationState<T> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        this.getBone("head").ifPresent(head -> {
            head.setRotX(-animatable.getXRot() * Mth.DEG_TO_RAD);
            head.setRotY((animatable.getYRot() - animatable.getViewYRot(animationState.getPartialTick())) * Mth.DEG_TO_RAD);
        });
    }*/
}
