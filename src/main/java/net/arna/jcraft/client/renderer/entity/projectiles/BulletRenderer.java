package net.arna.jcraft.client.renderer.entity.projectiles;

import lombok.NonNull;
import net.arna.jcraft.JCraft;
import net.arna.jcraft.common.entity.projectile.BulletProjectile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class BulletRenderer extends ProjectileRenderer<BulletProjectile> {

    public BulletRenderer(final @NonNull EntityRendererProvider.Context context) {
        super(context, () -> new EntityAnimator<>("bullet"), b -> b.setScale(a -> a.getCaliber() * 0.016f),
                JCraft.id(MODEL_STR_TEMPLATE.formatted("bullet")),
                JCraft.id("textures/entity/projectiles/bullet.png"));
    }
}
