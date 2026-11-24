package net.arna.jcraft.mixin.client;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.remote.StandRemoteControlHandler;
import net.arna.jcraft.client.util.JClientUtils;
import net.arna.jcraft.common.gravity.api.GravityChangerAPI;
import net.arna.jcraft.common.gravity.util.RotationUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

    /**
     * Rotates the 8 eye-level block-overlay checks with the player's gravity direction so
     * standing on walls or ceilings does not trigger a false full-screen suffocation overlay.
     */
    @Inject(
            method = "getViewBlockingState(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/client/renderer/culling/Frustum;)Lnet/minecraft/world/level/block/state/BlockState;",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void jcraft$getViewBlockingStateWithGravity(
            LocalPlayer player,
            Frustum frustum,
            CallbackInfoReturnable<BlockState> cir) {
        Direction dir = GravityChangerAPI.getGravityDirection(player);
        if (dir == Direction.DOWN) {
            return;
        }
        if (player.noPhysics) {
            cir.setReturnValue(null);
            return;
        }
        Vec3 eyePos = player.getEyePosition();
        AABB eyeNear = frustum.getNearPlaneBounds().move(eyePos);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < 8; i++) {
            double lx = ((i >> 0) % 2 - 0.5F) * player.getBbWidth() * 0.8F;
            double ly = ((i >> 1) % 2 - 0.5F) * 0.1F * player.getScale();
            double lz = ((i >> 2) % 2 - 0.5F) * player.getBbWidth() * 0.8F;
            Vec3 worldOffset = RotationUtil.vecPlayerToWorld(lx, ly, lz, dir);
            pos.set(eyePos.x + worldOffset.x, eyePos.y + worldOffset.y, eyePos.z + worldOffset.z);
            BlockState blockState = player.level().getBlockState(pos);
            if (blockState.getRenderShape() != RenderShape.INVISIBLE
                    && blockState.isViewBlocking(player.level(), pos, eyeNear)) {
                cir.setReturnValue(blockState);
                return;
            }
        }
        cir.setReturnValue(null);
    }
}
