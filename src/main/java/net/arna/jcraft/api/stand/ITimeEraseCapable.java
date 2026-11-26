package net.arna.jcraft.api.stand;

/**
 * Capability interface for Stands that support Time Erase state and visuals.
 */
public interface ITimeEraseCapable {
    int getTETime();

    void setTETime(int time);
}
