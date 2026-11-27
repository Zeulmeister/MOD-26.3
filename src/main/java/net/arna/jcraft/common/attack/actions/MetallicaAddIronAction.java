package net.arna.jcraft.common.attack.actions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.core.MoveAction;
import net.arna.jcraft.api.attack.core.MoveActionType;
import net.arna.jcraft.api.registry.JTagRegistry;
import net.arna.jcraft.api.stand.IIronCapable;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

@Getter
@RequiredArgsConstructor(staticName = "addIron")
public class MetallicaAddIronAction extends MoveAction<MetallicaAddIronAction, IAttacker<?, ?>> {
    private final float iron;

    @Override
    public void perform(IAttacker<?, ?> attacker, LivingEntity user, Set<LivingEntity> targets) {
        boolean foundIronEntity = false;
        for (final LivingEntity target : targets) {
            if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.wrapAsHolder(target.getType()).is(JTagRegistry.IRONLESS_ENTITIES)) {
                foundIronEntity = true;
                break;
            }
        }
        if (foundIronEntity && attacker instanceof IIronCapable ironStand) {
            ironStand.addIron(iron);
        }
    }

    @Override
    public @NonNull MoveActionType<MetallicaAddIronAction> getType() {
        return Type.INSTANCE;
    }

    public static class Type extends MoveActionType<MetallicaAddIronAction> {
        public static final Type INSTANCE = new Type();

        @Override
        public Codec<MetallicaAddIronAction> getCodec() {
            return RecordCodecBuilder.create(instance -> instance.group(
                    runMoment(),
                    Codec.FLOAT.fieldOf("iron").forGetter(MetallicaAddIronAction::getIron)
            ).apply(instance, apply(MetallicaAddIronAction::addIron)));
        }
    }
}