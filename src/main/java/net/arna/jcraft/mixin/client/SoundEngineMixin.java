package net.arna.jcraft.mixin.client;

import net.arna.jcraft.client.util.JClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void jcraft$muteExternalSoundsInTimeStop(
            SoundInstance sound,
            CallbackInfoReturnable<SoundEngine.PlayResult> cir
    ) {
        if (sound == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Vec3 soundPos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
        if (JClientUtils.isUserOfActiveTimestop(client.player) || JClientUtils.isInTSRange(soundPos)) {
            Identifier id = sound.getIdentifier();
            if (id != null && "jcraft".equals(id.getNamespace())) {
                return;
            }

            SoundSource source = sound.getSource();
            if (source == SoundSource.MUSIC || source == SoundSource.RECORDS || source == SoundSource.UI) {
                return;
            }

            if (source == SoundSource.WEATHER
                    || source == SoundSource.BLOCKS
                    || source == SoundSource.HOSTILE
                    || source == SoundSource.NEUTRAL
                    || source == SoundSource.AMBIENT) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
                return;
            }

            if (client.player.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) > 4.0D) {
                cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
            }
        }
    }
}
