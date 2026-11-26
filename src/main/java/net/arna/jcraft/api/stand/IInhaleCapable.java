package net.arna.jcraft.api.stand;

/**
 * Capability interface for Stands that support vacuum Inhale duration tracking.
 */
public interface IInhaleCapable {
    int getInhaleTime();

    void setInhaleTime(int time);
}
