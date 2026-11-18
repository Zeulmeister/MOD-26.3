package net.arna.jcraft.api.registry;

import net.arna.jcraft.common.command.JCraftChangesCommand;
import net.arna.jcraft.common.data.AttackerDataLoader;
import net.arna.jcraft.common.data.MoveSetLoader;
import net.arna.jcraft.common.util.EvolutionItemHandler;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public interface JServerReloadListenerRegistry {
    @FunctionalInterface
    interface LegacyReloadListener {
        CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager manager,
                                       ProfilerFiller prepProfiler, ProfilerFiller applyProfiler,
                                       Executor backgroundExecutor, Executor gameExecutor);
    }

    static PreparableReloadListener adapt(LegacyReloadListener legacy) {
        return (sharedState, backgroundExecutor, barrier, gameExecutor) ->
                legacy.reload(barrier, sharedState.resourceManager(), InactiveProfiler.INSTANCE, InactiveProfiler.INSTANCE, backgroundExecutor, gameExecutor);
    }

    static void register(Consumer<PreparableReloadListener> registrar) {
        registrar.accept(adapt(MoveSetLoader::onReload));
        registrar.accept(adapt(JCraftChangesCommand::onReload));
        registrar.accept(adapt(EvolutionItemHandler::onReload));
        registrar.accept(adapt(AttackerDataLoader::onReload));
    }
}
