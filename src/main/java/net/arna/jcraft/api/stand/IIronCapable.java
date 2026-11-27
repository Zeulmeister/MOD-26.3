package net.arna.jcraft.api.stand;

import net.minecraft.core.BlockPos;

import java.util.Optional;

/**
 * Capability interface for Stands that wield Metallica's Iron gauge, Bisect charge, cloaking, and siphon state.
 */
public interface IIronCapable {
    float getIron();

    void setIron(float iron);

    void addIron(float add);

    boolean drainIron(float cost);

    int getBisectChargeTime();

    void setBisectChargeTime(int time);

    boolean isMetallicaInvisible();

    void setMetallicaInvisible(boolean invisible);

    Optional<BlockPos> getSiphonPos();

    void setSiphonPos(Optional<BlockPos> pos);

    default float getMaxIron() {
        return 80.0f;
    }
}