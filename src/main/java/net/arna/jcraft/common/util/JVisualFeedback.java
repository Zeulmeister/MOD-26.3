package net.arna.jcraft.common.util;

import io.netty.buffer.Unpooled;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.stand.OriginEntity;
import net.arna.jcraft.common.network.s2c.ServerChannelFeedbackPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * Server-side visual feedback dispatcher for aura-colored afterimage trails,
 * Time Skip warp echoes, and Heavy/Ultimate impact shock-shake bursts.
 * Purely common logic (zero client imports).
 */
public final class JVisualFeedback {
    public static final short CONTROL_AURA_AFTERIMAGE = 15;
    public static final short CONTROL_IMPACT_EFFECT = 16;

    private JVisualFeedback() {}

    public static int resolveAuraRgb(@Nullable Entity actor, @Nullable StandEntity<?, ?> stand) {
        if (stand == null) {
            if (actor instanceof StandEntity<?, ?> directStand) {
                stand = directStand;
            } else if (actor instanceof LivingEntity living) {
                stand = JUtils.getStand(living);
            }
        }
        if (stand instanceof OriginEntity origin) {
            if (origin.hasCustomGlowRgb()) {
                return origin.getCustomGlowRgb();
            }
            if (origin.getSkin() == 4) {
                return 0x0C0C16; // Absolute Abyss: shadow-black veil + silver wisps
            }
            if (origin.getSkin() == 5) {
                return 0xFFD84A; // Celestial Divinity: radiant solar gold
            }
        }
        if (stand != null) {
            Vector3f c = stand.getAuraColor();
            if (c != null) {
                int r = Mth.clamp(Math.round(c.x() * 255.0f), 0, 255);
                int g = Mth.clamp(Math.round(c.y() * 255.0f), 0, 255);
                int b = Mth.clamp(Math.round(c.z() * 255.0f), 0, 255);
                if (r + g + b > 24) {
                    return (r << 16) | (g << 8) | b;
                }
            }
        }
        return 0xA855F7;
    }

    public static void sendAfterimageTrail(@Nullable LivingEntity actor, int durationTicks) {
        if (actor == null || durationTicks <= 0 || !(actor.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int rgb = resolveAuraRgb(actor, null);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeShort(CONTROL_AURA_AFTERIMAGE);
        buf.writeByte(0); // continuous trail
        buf.writeInt(actor.getId());
        buf.writeInt(rgb);
        buf.writeInt(durationTicks);
        ServerChannelFeedbackPacket.send(JUtils.around(serverLevel, actor.position(), 64), buf);
    }

    public static void sendWarpAfterimage(@Nullable LivingEntity actor, Vec3 fromPos, Vec3 toPos) {
        if (actor == null || fromPos == null || toPos == null || !(actor.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (fromPos.distanceToSqr(toPos) < 0.25) {
            return;
        }
        int rgb = resolveAuraRgb(actor, null);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeShort(CONTROL_AURA_AFTERIMAGE);
        buf.writeByte(1); // warp line between fromPos and toPos
        buf.writeInt(actor.getId());
        buf.writeInt(rgb);
        buf.writeDouble(fromPos.x);
        buf.writeDouble(fromPos.y);
        buf.writeDouble(fromPos.z);
        buf.writeDouble(toPos.x);
        buf.writeDouble(toPos.y);
        buf.writeDouble(toPos.z);
        ServerChannelFeedbackPacket.send(JUtils.around(serverLevel, toPos, 64), buf);
    }

    public static void sendImpactEffect(ServerLevel level, Vec3 pos, float intensity, int rgb, boolean isUltimate) {
        if (level == null || pos == null) {
            return;
        }
        float clamped = Mth.clamp(intensity, 0.15f, 1.0f);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeShort(CONTROL_IMPACT_EFFECT);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeFloat(clamped);
        buf.writeInt(rgb & 0xFFFFFF);
        buf.writeBoolean(isUltimate);
        ServerChannelFeedbackPacket.send(JUtils.around(level, pos, 24), buf);
    }
}
