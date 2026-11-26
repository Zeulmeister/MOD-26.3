package net.arna.jcraft.api.stand;

/**
 * Capability interface for Stands that support Time Acceleration and speed buildup.
 */
public interface ITimeAccelCapable {
    int getAccelTime();

    void setAccelTime(int time);

    int getSpeedometer();

    void setSpeedometer(int speedometer);

    boolean isAfterimage();

    void setAfterimage(boolean afterimage);
}
