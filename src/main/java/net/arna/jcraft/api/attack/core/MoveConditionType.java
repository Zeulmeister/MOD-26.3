package net.arna.jcraft.api.attack.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

public interface MoveConditionType<C extends MoveCondition<C, ?>> {
    Codec<C> getCodec();

    @SuppressWarnings("unchecked")
    default MapCodec<C> getMapCodec() {
        Codec<C> c = getCodec();
        return c instanceof MapCodec.MapCodecCodec<C> mcc ? mcc.codec() : c.fieldOf("value");
    }
}
