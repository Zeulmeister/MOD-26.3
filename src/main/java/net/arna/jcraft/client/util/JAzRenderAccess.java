package net.arna.jcraft.client.util;

import mod.azure.azurelib.render.AzRendererPipelineContext;
import mod.azure.azurelib.render.entity.AzEntityRendererPipeline;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.lang.reflect.Field;

public final class JAzRenderAccess {
    private static final Field MODEL_RENDER_TRANSLATIONS;

    static {
        Field f = null;
        try {
            f = AzEntityRendererPipeline.class.getDeclaredField("modelRenderTranslations");
            f.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
        MODEL_RENDER_TRANSLATIONS = f;
    }

    private JAzRenderAccess() {}

    public static void setModelRenderTranslations(AzEntityRendererPipeline<?> pipeline, Matrix4fc matrix) {
        if (MODEL_RENDER_TRANSLATIONS == null || pipeline == null || matrix == null) return;
        try {
            Matrix4f target = (Matrix4f) MODEL_RENDER_TRANSLATIONS.get(pipeline);
            if (target != null) {
                target.set(matrix);
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    public static float getAlpha(AzRendererPipelineContext<?, ?> ctx) {
        return ((ctx.renderColor() >>> 24) & 0xFF) / 255.0f;
    }

    public static void setAlpha(AzRendererPipelineContext<?, ?> ctx, float alpha) {
        int a = (int) (Mth.clamp(alpha, 0.0f, 1.0f) * 255.0f);
        ctx.setRenderColor((ctx.renderColor() & 0x00FFFFFF) | (a << 24));
    }

    public static void setRenderColorRgb(final AzRendererPipelineContext<?, ?> pc, final float r, final float g, final float b) {
        int cur = pc.renderColor();
        int a = (cur >>> 24) & 0xFF;
        int ri = Math.clamp((int) (r * 255.0f), 0, 255);
        int gi = Math.clamp((int) (g * 255.0f), 0, 255);
        int bi = Math.clamp((int) (b * 255.0f), 0, 255);
        pc.setRenderColor((a << 24) | (ri << 16) | (gi << 8) | bi);
    }
}
