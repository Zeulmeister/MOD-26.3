package net.arna.jcraft.client.rendering.handler;

import dev.architectury.event.events.client.ClientTickEvent;
import org.joml.Matrix4f;

public abstract class StandShaderHandler implements ClientTickEvent.Client {
    public int ticks = 0;
    public boolean shouldRender = false;
    public boolean renderingEffect = false;
    public final Matrix4f projectionMatrix = new Matrix4f();

    public void init() {
        ClientTickEvent.CLIENT_POST.register(this);
    }
}
