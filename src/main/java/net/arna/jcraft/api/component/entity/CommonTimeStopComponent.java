package net.arna.jcraft.api.component.entity;

import net.arna.jcraft.api.component.JComponent;
import net.minecraft.world.phys.Vec3;

public interface CommonTimeStopComponent extends JComponent {

    int getTicks();

    //TODO: specialized sync packet
    void setTicks(final int ticks);

    /**
     * Client-local tick update used by client-side Time Stop prediction without triggering server sync packets.
     */
    default void setClientTicks(final int ticks) {
        setTicks(ticks);
    }

    /**
     * Immediately releases this entity from Time Stop, applying any accumulated launch velocity and syncing once.
     */
    default void releaseNow() {
        setTicks(0);
    }

    void addTotalVelocity(final Vec3 vel);

    /**
     * @return <code>true</code> if further tick handling shall be interrupted
     */
    boolean tick();

}
