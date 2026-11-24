package net.arna.jcraft.client.util;

import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.modifier.SpeedModifier;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.arna.jcraft.JCraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class JPlayerAnimHelper {
    public static final Identifier LAYER_ID = JCraft.id("animation");

    private JPlayerAnimHelper() {}

    public static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(
                LAYER_ID,
                42,
                avatar -> new PlayerAnimationController(avatar, (controller, state, animSetter) -> PlayState.STOP)
        );
    }

    public static void play(Player player, String animPath, float speed) {
        if (player == null) return;
        try {
            var layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER_ID);
            if (layer instanceof PlayerAnimationController controller) {
                controller.removeModifierIf(m -> m instanceof SpeedModifier);
                if (animPath == null || animPath.isEmpty()) {
                    controller.stopTriggeredAnimation();
                } else {
                    float clampedSpeed = speed > 0.05f ? speed : 1.0f;
                    if (Math.abs(clampedSpeed - 1.0f) > 0.01f) {
                        controller.addModifierBefore(new SpeedModifier(clampedSpeed));
                    }
                    Identifier animId = animPath.contains(":") ? Identifier.parse(animPath) : JCraft.id(animPath);
                    controller.triggerAnimation(animId);
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void stop(Player player) {
        play(player, "", 1.0f);
    }
}
