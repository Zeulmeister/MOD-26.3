package net.arna.jcraft.mixin;

import net.arna.jcraft.api.splatter.JSplatterManager;
import net.arna.jcraft.common.events.JBlockEvents;
import net.arna.jcraft.common.tickable.Timestops;
import net.arna.jcraft.common.util.IJSplatterManagerHolder;
import net.arna.jcraft.mixin_logic.LevelAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Level.class)
public abstract class LevelMixin implements LevelAddon, IJSplatterManagerHolder {

    @Unique
    private boolean jcraft$ignoreSetBlock = false;

    @Unique
    private JSplatterManager jcraft$splatterManager;

    @Override
    public void jcraft$setIgnoreSetBlock(boolean value) {
        this.jcraft$ignoreSetBlock = value;
    }

    @Override
    public JSplatterManager jcraft$getSplatterManager() {
        if (this.jcraft$splatterManager == null) {
            this.jcraft$splatterManager = new JSplatterManager((Level) (Object) this);
        }
        return this.jcraft$splatterManager;
    }

    @Inject(
            method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void jcraft$fireBeforeSetEvent(BlockPos pos, BlockState newState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
        if (this.jcraft$ignoreSetBlock) {
            return;
        }
        Level self = (Level) (Object) this;
        BlockState oldState = self.getBlockState(pos);
        if (Objects.equals(oldState, newState)) {
            return;
        }
        if (JBlockEvents.BEFORE_SET.invoker().setBlock(pos, oldState, newState, self).interruptsFurtherEvaluation()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void jcraft$freezeBlockEntitiesInTimestop(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        Level self = (Level) (Object) this;
        if (Timestops.isInTSRangeForLevel(self, pos)) {
            cir.setReturnValue(false);
        }
    }
}
