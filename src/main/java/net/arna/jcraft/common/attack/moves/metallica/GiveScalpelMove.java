package net.arna.jcraft.common.attack.moves.metallica;

import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.NonNull;
import net.arna.jcraft.api.attack.MoveType;
import net.arna.jcraft.api.attack.moves.AbstractMove;
import net.arna.jcraft.api.registry.JItemRegistry;
import net.arna.jcraft.api.stand.IIronCapable;
import net.arna.jcraft.api.stand.StandEntity;
import net.arna.jcraft.common.entity.projectile.ScalpelProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class GiveScalpelMove<A extends StandEntity<? extends A, ?>> extends AbstractMove<GiveScalpelMove<A>, A> {
    public GiveScalpelMove(int cooldown, int windup, int duration, float moveDistance) {
        super(cooldown, windup, duration, moveDistance);
    }

    @Override
    public @NonNull MoveType<GiveScalpelMove<A>> getMoveType() {
        return Type.INSTANCE.cast();
    }

    @Override
    public @NonNull Set<LivingEntity> perform(A attacker, LivingEntity user) {
        if (user instanceof final ServerPlayer playerEntity) {
            playerEntity.addItem(JItemRegistry.SCALPEL.get().getDefaultInstance());
            if (attacker instanceof IIronCapable iron) {
                iron.drainIron(ScalpelProjectile.IRON_COST);
            }
        }

        return Set.of();
    }

    @Override
    protected @NonNull GiveScalpelMove<A> getThis() {
        return this;
    }

    @Override
    public @NonNull GiveScalpelMove<A> copy() {
        return copyExtras(new GiveScalpelMove<>(getCooldown(), getWindup(), getDuration(), getMoveDistance()));
    }

    public static final class Type extends AbstractMove.Type<GiveScalpelMove<?>> {
        public static final GiveScalpelMove.Type INSTANCE = new GiveScalpelMove.Type();

        @Override
        protected @NotNull App<RecordCodecBuilder.Mu<GiveScalpelMove<?>>, GiveScalpelMove<?>> buildCodec(RecordCodecBuilder.Instance<GiveScalpelMove<?>> instance) {
            return baseDefault(instance, GiveScalpelMove::new);
        }
    }
}