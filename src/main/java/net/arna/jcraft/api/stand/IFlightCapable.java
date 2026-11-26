package net.arna.jcraft.api.stand;

/**
 * Capability interface for Stands that grant temporary Stand flight.
 */
public interface IFlightCapable {
    int getFlightTime();

    void setFlightTime(int time);
}
