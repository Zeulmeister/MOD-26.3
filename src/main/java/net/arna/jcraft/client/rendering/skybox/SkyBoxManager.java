package net.arna.jcraft.client.rendering.skybox;

public class SkyBoxManager {
    private static SkyBoxManager instance;

    public static SkyBoxManager getInstance() {
        if (instance == null) {
            instance = new SkyBoxManager();
        }
        return instance;
    }

    public void setEnabled(boolean enabled) {}
    public boolean isEnabled() { return false; }
}
