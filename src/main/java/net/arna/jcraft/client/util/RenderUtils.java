package net.arna.jcraft.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

@UtilityClass
public class RenderUtils {

    public static final int FULL_BRIGHT = 15728880;

    public static void vertexPos(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z) {
        vertexConsumer.addVertex(last, x, y, z);
    }

    public static void vertexPosUV(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z, final float u, final float v) {
        vertexConsumer.addVertex(last, x, y, z).setUv(u, v);
    }

    public static void vertexPosUVLight(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z, final float u, final float v, final int light) {
        vertexConsumer.addVertex(last, x, y, z).setUv(u, v).setLight(light);
    }

    public static void vertexPosColor(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z, final float r, final float g, final float b, final float a) {
        vertexConsumer.addVertex(last, x, y, z).setColor(r, g, b, a);
    }

    public static void vertexPosColorUV(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z, final float r, final float g, final float b, final float a, final float u, final float v) {
        vertexConsumer.addVertex(last, x, y, z).setColor(r, g, b, a).setUv(u, v);
    }

    public static void vertexPosColorUVLight(final VertexConsumer vertexConsumer, final Matrix4f last, final float x, final float y, final float z, final float r, final float g, final float b, final float a, final float u, final float v, final int light) {
        vertexConsumer.addVertex(last, x, y, z).setColor(r, g, b, a).setUv(u, v).setLight(light);
    }

    public static float distSqr(final float... a) {
        float d = 0.0F;
        for (float f : a) {
            d += f * f;
        }
        return d;
    }

    public static float distance(final float... a) {
        return Mth.sqrt(distSqr(a));
    }

    public static void renderBlock(final PoseStack matrixStack, final VertexConsumer vertexConsumer) {
        final Matrix4f matrix4f = matrixStack.last().pose();
        renderSide(matrix4f, vertexConsumer, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F);
        renderSide(matrix4f, vertexConsumer, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        renderSide(matrix4f, vertexConsumer, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        renderSide(matrix4f, vertexConsumer, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        renderSide(matrix4f, vertexConsumer, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F);
    }

    private static void renderSide(final Matrix4f model, final VertexConsumer vertices, final float x1, final float x2, final float y1, final float y2, final float z1, final float z2, final float z3, final float z4) {
        vertices.addVertex(model, x1, y1, z1);
        vertices.addVertex(model, x2, y1, z2);
        vertices.addVertex(model, x2, y2, z3);
        vertices.addVertex(model, x1, y2, z4);
    }

    public static void startOverlayRender() {}
    public static void endOverlayRender() {}
}
