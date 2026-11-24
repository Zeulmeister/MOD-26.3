package net.arna.jcraft.client.renderer.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arna.jcraft.api.component.living.CommonHamonComponent;
import net.arna.jcraft.api.registry.JParticleTypeRegistry;
import net.arna.jcraft.client.rendering.IJCraftEntityRenderStateHolder;
import net.arna.jcraft.platform.JComponentPlatformUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Emits golden Hamon sparks around entities holding an active Hamon charge or Hamonize ready state.
 */
public class HamonParticlesFeatureRenderer<S extends HumanoidRenderState, M extends HumanoidModel<S>> extends RenderLayer<S, M> {
    private static final Map<LivingEntity, Integer> LAST_EMIT_TICK = new WeakHashMap<>();

    public HamonParticlesFeatureRenderer(final RenderLayerParent<S, M> parent) {
        super(parent);
    }

    @Override
    public void submit(final PoseStack poseStack, final SubmitNodeCollector collector, final int packedLight,
                       final S state, final float yRot, final float xRot) {
        final LivingEntity entity = ((IJCraftEntityRenderStateHolder) state).jcraft$getLivingEntity();
        if (entity == null || entity.isInvisible() || Minecraft.getInstance().isPaused()) {
            return;
        }
        final CommonHamonComponent hamon = JComponentPlatformUtils.getHamon(entity);
        if (hamon == null) {
            return;
        }
        final float charge = hamon.getHamonCharge();
        final boolean ready = hamon.isHamonizeReady();
        if (charge < 5.0f && !ready) {
            return;
        }
        final int interval = ready ? 2 : 5;
        final int last = LAST_EMIT_TICK.getOrDefault(entity, -999);
        if (entity.tickCount - last < interval && entity.tickCount >= last) {
            return;
        }
        LAST_EMIT_TICK.put(entity, entity.tickCount);

        final RandomSource rand = entity.getRandom();
        final double lx = (rand.nextDouble() - 0.5) * entity.getBbWidth() * 1.4;
        final double ly = rand.nextDouble() * entity.getBbHeight();
        final double lz = (rand.nextDouble() - 0.5) * entity.getBbWidth() * 1.4;
        final var dir = net.arna.jcraft.common.gravity.api.GravityChangerAPI.getGravityDirection(entity);
        final var offset = net.arna.jcraft.common.gravity.util.RotationUtil.vecPlayerToWorld(lx, ly, lz, dir);
        final var vel = net.arna.jcraft.common.gravity.util.RotationUtil.vecPlayerToWorld(0.0, 0.03, 0.0, dir);
        entity.level().addParticle(
                JParticleTypeRegistry.HAMON_SPARK.get(),
                entity.getX() + offset.x, entity.getY() + offset.y, entity.getZ() + offset.z,
                vel.x, vel.y, vel.z
        );
    }

    public static void prepareHamonAura() {}
    public static void prepareHamonAura(LivingEntity entity) {}
}
