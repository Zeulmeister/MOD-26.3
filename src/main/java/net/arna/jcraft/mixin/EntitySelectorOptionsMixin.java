package net.arna.jcraft.mixin;

import net.arna.jcraft.JCraft;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(EntitySelectorOptions.class)
public abstract class EntitySelectorOptionsMixin {

    @Shadow
    private static void register(
            String id,
            EntitySelectorOptions.Modifier handler,
            Predicate<EntitySelectorParser> predicate,
            Component tooltip
    ) {
    }

    @Inject(method = "bootStrap", at = @At("TAIL"))
    private static void jcraft$registerEntitySelectorOptions(CallbackInfo ci) {
        JCraft.registerEntitySelectorOptions(EntitySelectorOptionsMixin::register);
    }
}
