package net.arna.jcraft.api.stand;

import net.arna.jcraft.common.entity.stand.AbstractPurpleHazeEntity;

/**
 * Capability interface for Stands that fire Purple Haze virus capsules and toggle PoisonType.
 */
public interface IPoisonCapsuleCapable {
    AbstractPurpleHazeEntity.PoisonType getPoisonType();

    default void nextPoisonType() {}
}