package net.arna.jcraft.mixin;

import net.arna.jcraft.common.util.IJExplosion;
import net.arna.jcraft.common.util.JExplosionModifier;
import net.minecraft.world.level.ServerExplosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin implements IJExplosion {

    @Unique
    @Nullable
    private JExplosionModifier jcraft$modifier;

    @Override
    public void jcraft$setModifier(JExplosionModifier modifier) {
        this.jcraft$modifier = modifier;
    }
}
