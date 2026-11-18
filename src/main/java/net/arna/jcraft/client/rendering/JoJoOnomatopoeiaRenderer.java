package net.arna.jcraft.client.rendering;

import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.client.JClientConfig;
import net.arna.jcraft.client.util.JClientUtils;
import net.arna.jcraft.common.util.JUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Dynamic Manga Onomatopoeia Overlay & Burst Renderer (ゴゴゴ / ドドド) for Minecraft 26.3.
 */
public final class JoJoOnomatopoeiaRenderer {
    private static final List<FloatingGlyph> ACTIVE_GLYPHS = new ArrayList<>();
    private static final RandomSource RNG = RandomSource.create();
    private static int burstTicks = 0;
    private static boolean burstMenacing = true;

    private JoJoOnomatopoeiaRenderer() {}

    private static final class FloatingGlyph {
        final String symbol;
        float normX;
        float normY;
        float velY;
        float driftPhase;
        float scale;
        int age;
        final int maxAge;
        final int baseRgb;

        FloatingGlyph(String symbol, float normX, float normY, float scale, int maxAge, int baseRgb) {
            this.symbol = symbol;
            this.normX = normX;
            this.normY = normY;
            this.velY = -0.0045F - RNG.nextFloat() * 0.003F;
            this.driftPhase = RNG.nextFloat() * Mth.TWO_PI;
            this.scale = scale;
            this.maxAge = maxAge;
            this.baseRgb = baseRgb;
        }
    }

    public static void triggerBurst(boolean menacingGo, int durationTicks) {
        burstMenacing = menacingGo;
        burstTicks = Math.max(burstTicks, durationTicks);
    }

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !player.isAlive() || !JClientConfig.getInstance().isOnomatopoeiaEnabled()) {
            ACTIVE_GLYPHS.clear();
            burstTicks = 0;
            return;
        }

        if (burstTicks > 0) {
            burstTicks--;
        }

        StandEntity<?, ?> stand = JUtils.getStand(player);
        boolean inTimeStop = JClientUtils.isInTSRange(player) || (stand != null && stand.getTsTime() > 0);
        boolean barraging = stand != null && stand.getMoveStun() > 4 && stand.isHolding();
        boolean lowHp = player.getHealth() <= player.getMaxHealth() * 0.25F;

        if ((burstTicks > 0 || inTimeStop || barraging || lowHp) && ACTIVE_GLYPHS.size() < 12) {
            int interval = barraging ? 3 : (burstTicks > 0 ? 4 : 8);
            if (player.tickCount % interval == 0) {
                boolean leftEdge = RNG.nextBoolean();
                float nx = leftEdge ? (0.04F + RNG.nextFloat() * 0.12F) : (0.84F + RNG.nextFloat() * 0.12F);
                float ny = 0.25F + RNG.nextFloat() * 0.55F;
                String sym = barraging ? "ド" : (inTimeStop ? "ゴ" : (burstMenacing ? "ゴ" : "ド"));
                int color = inTimeStop ? 0xD8B4FE : (barraging ? 0xFBBF24 : 0xC084FC);
                ACTIVE_GLYPHS.add(new FloatingGlyph(sym, nx, ny, 1.45F + RNG.nextFloat() * 0.65F, 34, color));
            }
        }

        Iterator<FloatingGlyph> it = ACTIVE_GLYPHS.iterator();
        while (it.hasNext()) {
            FloatingGlyph g = it.next();
            g.age++;
            g.normY += g.velY;
            if (g.age >= g.maxAge) {
                it.remove();
            }
        }
    }

    public static void render(GuiGraphicsExtractor ctx, float partialTick) {
        if (ACTIVE_GLYPHS.isEmpty() || !JClientConfig.getInstance().isOnomatopoeiaEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Font font = client.font;
        int sw = client.getWindow().getGuiScaledWidth();
        int sh = client.getWindow().getGuiScaledHeight();

        for (FloatingGlyph g : ACTIVE_GLYPHS) {
            float life = (g.age + partialTick) / (float) g.maxAge;
            float alphaFactor = life < 0.2F ? (life / 0.2F) : (1.0F - (life - 0.2F) / 0.8F);
            int alpha = Mth.clamp((int) (alphaFactor * 225.0F), 0, 255);
            if (alpha <= 4) continue;

            float wobbleX = Mth.sin((g.age + partialTick) * 0.45F + g.driftPhase) * 3.5F;
            float px = g.normX * sw + wobbleX;
            float py = (g.normY + g.velY * partialTick) * sh;

            int fg = ARGB.color(alpha, g.baseRgb);
            int shadow = ARGB.color((int) (alpha * 0.85F), 0x1E0836);

            ctx.pose().pushMatrix();
            ctx.pose().translate(px, py);
            ctx.pose().scale(g.scale, g.scale);
            ctx.text(font, g.symbol, 1, 1, shadow, false);
            ctx.text(font, g.symbol, 0, 0, fg, false);
            ctx.pose().popMatrix();
        }
    }
}
