package net.arna.jcraft.client.renderer.entity.projectiles;

import com.mojang.math.Axis;
import mod.azure.azurelib.render.entity.AzEntityRenderer;
import mod.azure.azurelib.render.entity.AzEntityRendererConfig;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.projectile.GasCanProjectile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class GasCanRenderer extends AzEntityRenderer<GasCanProjectile> {
    private static final Identifier MODEL = JCraft.id("geo/canister.geo.json");
    private static final Identifier TEXTURE = JCraft.id("textures/item/gascan.png");

    public GasCanRenderer(EntityRendererProvider.Context context) {
        super(AzEntityRendererConfig.<GasCanProjectile>builder(MODEL, TEXTURE)
                .setPrerenderEntry(ctx -> {
                    GasCanProjectile entity = ctx.animatable();
                    if (entity != null) {
                        float partialTick = ctx.partialTick();
                        Vec3 prevVelocity = entity.getPrevDeltaMovement();
                        Vec3 curVelocity = entity.getDeltaMovement();
                        Vec3 velocity = new Vec3(
                                Mth.lerp(partialTick, prevVelocity.x, curVelocity.x),
                                Mth.lerp(partialTick, prevVelocity.y, curVelocity.y),
                                Mth.lerp(partialTick, prevVelocity.z, curVelocity.z)
                        );
                        if (velocity.lengthSqr() > 1e-5) {
                            double horizontalDist = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
                            float pitch = (float) Math.toDegrees(Math.atan2(-velocity.y, horizontalDist)) + 90f;
                            float yaw = (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
                            ctx.poseStack().rotate(Axis.YN.rotationDegrees(yaw));
                            ctx.poseStack().rotate(Axis.XP.rotationDegrees(pitch));
                            ctx.poseStack().rotate(Axis.YN.rotationDegrees(yaw));
                        }
                    }
                    return ctx;
                })
                .build(), context);
    }
}
