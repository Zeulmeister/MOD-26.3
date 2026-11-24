package net.arna.jcraft.mixin.client;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.client.util.JClientUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private ClientLevel level;

    @Shadow
    public abstract boolean isEntityVisible(Entity entity, Frustum frustum, double x, double y, double z,
                                            float partialTick, long sectionFadeTime);

    @Invoker("extractEntity")
    protected abstract EntityRenderState jcraft$extractEntity(Entity entity, float partialTick);

    @Inject(method = "isEntityVisible", at = @At("HEAD"), cancellable = true)
    private void jcraft$hideStealthEntities(
            Entity entity,
            Frustum frustum,
            double x,
            double y,
            double z,
            float partialTick,
            long sectionFadeTime,
            CallbackInfoReturnable<Boolean> cir) {
        if (JClientUtils.shouldNotRender(entity)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * Vanilla MC 26.3 skips extracting LocalPlayer whenever {@code camera.entity() != player}.
     * During Stand Pilot Mode ({@link StandRemoteControlHandler}), the camera attaches to the piloted Stand,
     * which caused the player's own body to turn completely invisible. Re-extract the player here if visible.
     */
    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void jcraft$extractLocalPlayerDuringPilotMode(
            Camera camera,
            Frustum frustum,
            DeltaTracker deltaTracker,
            LevelRenderState levelRenderState,
            CallbackInfo ci) {
        LocalPlayer player = this.minecraft.player;
        if (player == null || this.level == null || camera.entity() == player) {
            return;
        }
        if (!StandRemoteControlHandler.isControlling(this.minecraft)
                && !(camera.entity() instanceof StandEntity<?, ?> stand && stand.getUser() == player)) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(
                !this.level.tickRateManager().isEntityFrozen(player)
        );
        Vec3 camPos = camera.position();
        long sectionFadeTime = Util.toMillis(this.minecraft.options.chunkSectionFadeInTime().get());
        if (!this.isEntityVisible(player, frustum, camPos.x, camPos.y, camPos.z, partialTick, sectionFadeTime)) {
            return;
        }

        if (player.tickCount == 0) {
            player.xOld = player.getX();
            player.yOld = player.getY();
            player.zOld = player.getZ();
        }
        EntityRenderState state = this.jcraft$extractEntity(player, partialTick);
        levelRenderState.entityRenderStates.add(state);
        levelRenderState.lastEntityRenderStateCount = levelRenderState.entityRenderStates.size();
    }
}
