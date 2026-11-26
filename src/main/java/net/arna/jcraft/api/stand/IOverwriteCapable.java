package net.arna.jcraft.api.stand;

/**
 * Capability interface for Stands that support Reality Overwrite type selection.
 */
public interface IOverwriteCapable {
    int getOverwriteType();

    void setOverwriteType(int type);
}
