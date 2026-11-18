package net.arna.jcraft.common.attack.actions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import lombok.NonNull;
import net.arna.jcraft.api.attack.IAttacker;
import net.arna.jcraft.api.attack.core.MoveAction;
import net.arna.jcraft.api.attack.core.MoveActionType;
import net.arna.jcraft.common.util.JCodecUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Set;

@Getter
public class ScoreboardAction extends MoveAction<ScoreboardAction, IAttacker<?, ?>> {
    private final String objective;
    private final Operation operation;
    private final int value;

    public ScoreboardAction(String objective, Operation operation, int value) {
        this.objective = objective;
        this.operation = operation;
        this.value = value;
    }

    @Override
    public void perform(IAttacker<?, ?> attacker, LivingEntity user, Set<LivingEntity> targets) {
        Scoreboard scoreboard = user.level().getScoreboard();
        Objective obj = scoreboard.getObjective(this.objective);
        if (obj == null) {
            obj = scoreboard.addObjective(this.objective, ObjectiveCriteria.DUMMY, Component.literal(this.objective), ObjectiveCriteria.RenderType.INTEGER, true, null);
        }
        ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(user.getScoreboardName()), obj);
        switch (this.operation) {
            case ADD -> score.set(score.get() + this.value);
            case MULTIPLY -> score.set(score.get() * this.value);
            case SET -> score.set(this.value);
        }
    }

    @Override
    public @NonNull MoveActionType<ScoreboardAction> getType() {
        return Type.INSTANCE;
    }

    public enum Operation {
        ADD, MULTIPLY, SET;
        public static final Codec<Operation> CODEC = JCodecUtils.createEnumCodec(Operation.class);
    }

    public static class Type extends MoveActionType<ScoreboardAction> {
        public static final Type INSTANCE = new Type();

        @Override
        public Codec<ScoreboardAction> getCodec() {
            return RecordCodecBuilder.create(instance -> instance.group(
                    runMoment(),
                    Codec.STRING.fieldOf("objective").forGetter(ScoreboardAction::getObjective),
                    Operation.CODEC.fieldOf("operation").forGetter(ScoreboardAction::getOperation),
                    Codec.INT.fieldOf("value").forGetter(ScoreboardAction::getValue)
            ).apply(instance, apply(ScoreboardAction::new)));
        }
    }
}
