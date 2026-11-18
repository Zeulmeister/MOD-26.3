package net.arna.jcraft.client.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

public class AlphaForcingVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final int alpha;

    public AlphaForcingVertexConsumer(final VertexConsumer delegate, final int alpha) {
        this.delegate = delegate;
        this.alpha = Mth.clamp(alpha, 0, 255);
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int red, final int green, final int blue, final int a) {
        delegate.setColor(red, green, blue, alpha);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int argb) {
        delegate.setColor((argb & 0x00FFFFFF) | (alpha << 24));
        return this;
    }

    @Override
    public VertexConsumer setUv(final float u, final float v) {
        delegate.setUv(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv1(final int u, final int v) {
        delegate.setUv1(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv2(final int u, final int v) {
        delegate.setUv2(u, v);
        return this;
    }

    @Override
    public VertexConsumer setUv3(final float u, final float v) {
        delegate.setUv3(u, v);
        return this;
    }

    @Override
    public VertexConsumer setNormal(final float x, final float y, final float z) {
        delegate.setNormal(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setLineWidth(final float width) {
        delegate.setLineWidth(width);
        return this;
    }
}
