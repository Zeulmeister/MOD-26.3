package net.arna.jcraft.api.stand;

import net.arna.jcraft.common.entity.projectile.LargeIcicleProjectile;
import org.jetbrains.annotations.Nullable;

/**
 * Capability interface for Stands that spawn and detonate Horus large icicles.
 */
public interface IIceLanceCapable {
    @Nullable
    LargeIcicleProjectile getLastLargeIcicle();

    void setLastLargeIcicle(@Nullable LargeIcicleProjectile icicle);
}