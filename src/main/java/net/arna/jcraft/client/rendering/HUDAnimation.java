package net.arna.jcraft.client.rendering;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class HUDAnimation {
    private final Identifier atlas;
    private final int atlasWidth;
    private final int atlasHeight;
    private final List<Frame> frames;

    public List<Frame> getFrames() {
        return frames;
    }

    public Identifier getAtlas() {
        return atlas;
    }

    public int getAtlasWidth() {
        return atlasWidth;
    }

    public int getAtlasHeight() {
        return atlasHeight;
    }

    public int getFrameCount() {
        return frames.size();
    }

    public FrameCounter createFrameCounter(final float framerate, final boolean looping) {
        return new FrameCounter(framerate, Math.max(1, frames.size()), looping);
    }

    public void preload(final TextureManager textureManager, final Executor executor) {
        Minecraft.getInstance().execute(() -> textureManager.getTexture(atlas));
    }

    public Frame getFrame(final int index) {
        if (frames.isEmpty()) {
            return new Frame(this, 0, 0, atlasWidth, atlasHeight);
        }
        return frames.get(Math.floorMod(index, frames.size()));
    }

    public static HUDAnimation create(final Identifier atlas, final Identifier atlasData) {
        HUDAnimation dummy = new HUDAnimation(atlas, 256, 256, new ArrayList<>());
        final Optional<Resource> dataRes = Minecraft.getInstance().getResourceManager().getResource(atlasData);
        if (dataRes.isEmpty()) {
            dummy.frames.add(new Frame(dummy, 0, 0, 256, 256));
            return dummy;
        }

        final JsonObject data;
        try (BufferedReader reader = dataRes.get().openAsReader()) {
            data = new Gson().fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            dummy.frames.add(new Frame(dummy, 0, 0, 256, 256));
            return dummy;
        }

        int metaW = 256;
        int metaH = 256;
        if (data.has("meta") && data.getAsJsonObject("meta").has("size")) {
            JsonObject sz = data.getAsJsonObject("meta").getAsJsonObject("size");
            metaW = sz.get("w").getAsInt();
            metaH = sz.get("h").getAsInt();
        }

        List<Frame> parsedFrames = new ArrayList<>();
        HUDAnimation anim = new HUDAnimation(atlas, metaW, metaH, parsedFrames);
        if (data.has("frames")) {
            JsonElement fe = data.get("frames");
            if (fe.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : fe.getAsJsonObject().entrySet()) {
                    JsonObject fObj = entry.getValue().getAsJsonObject().getAsJsonObject("frame");
                    parsedFrames.add(new Frame(anim, fObj.get("x").getAsInt(), fObj.get("y").getAsInt(), fObj.get("w").getAsInt(), fObj.get("h").getAsInt()));
                }
            } else if (fe.isJsonArray()) {
                for (JsonElement el : fe.getAsJsonArray()) {
                    JsonObject fObj = el.getAsJsonObject().getAsJsonObject("frame");
                    parsedFrames.add(new Frame(anim, fObj.get("x").getAsInt(), fObj.get("y").getAsInt(), fObj.get("w").getAsInt(), fObj.get("h").getAsInt()));
                }
            }
        }
        if (parsedFrames.isEmpty()) {
            parsedFrames.add(new Frame(anim, 0, 0, metaW, metaH));
        }
        return anim;
    }

    public void render(final GuiGraphicsExtractor graphics, final int frameIdx, final int x, final int y, final int width, final int height, final float alpha) {
        if (frames.isEmpty()) return;
        Frame f = frames.get(Math.floorMod(frameIdx, frames.size()));
        int color = ((int) (Math.max(0f, Math.min(1f, alpha)) * 255f) << 24) | 0xFFFFFF;
        graphics.blit(RenderPipelines.GUI_TEXTURED, atlas, x, y, (float) f.x, (float) f.y, width, height, f.w, f.h, atlasWidth, atlasHeight, color);
    }

    public void render(final int frameIdx, final int x, final int y, final int width, final int height, final float alpha) {
    }

    @Getter
    @RequiredArgsConstructor
    public static class Frame {
        private final HUDAnimation parent;
        private final int x, y, w, h;

        public void render(final GuiGraphicsExtractor graphics) {
            int sw = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int sh = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            graphics.blit(RenderPipelines.GUI_TEXTURED, parent.atlas, 0, 0, (float) x, (float) y, sw, sh, w, h, parent.atlasWidth, parent.atlasHeight, -1);
        }

        public void render() {
            // Handled via GuiGraphicsExtractor overload in 26.3 HUD pipeline
        }
    }
}
