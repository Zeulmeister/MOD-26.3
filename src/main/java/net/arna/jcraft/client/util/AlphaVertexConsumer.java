package net.arna.jcraft.client.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.util.Mth;

public class AlphaVertexConsumer implements VertexConsumer {
    private final VertexConsumer delegate;
    private final float alpha;

    public AlphaVertexConsumer(final VertexConsumer delegate, final float alpha) {
        this.delegate = delegate;
        this.alpha = Mth.clamp(alpha, 0f, 1f);
    }

    @Override
    public VertexConsumer addVertex(final float x, final float y, final float z) {
        delegate.addVertex(x, y, z);
        return this;
    }

    @Override
    public VertexConsumer setColor(final int red, final int green, final int blue, final int a) {
        delegate.setColor(red, green, blue, (int) (a * this.alpha));
        return this;
    }

    @Override
    public VertexConsumer setColor(final int argb) {
        int a = (int) (((argb >>> 24) & 0xFF) * this.alpha);
        delegate.setColor((argb & 0x00FFFFFF) | (Mth.clamp(a, 0, 255) << 24));
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
