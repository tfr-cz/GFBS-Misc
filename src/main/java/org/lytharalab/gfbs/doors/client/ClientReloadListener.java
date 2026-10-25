package org.lytharalab.gfbs.doors.client;

import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@OnlyIn(Dist.CLIENT)
public final class ClientReloadListener implements PreparableReloadListener {
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            DoorClientSoundHooks.clearAll();
            DoorVisualCache.clear();
            CargoElevatorVisualCache.clear();
            DoorModelAssets.clear();
            DoorItemRenderer.clear();
            TransitionSealAnimation.clear();
        }, gameExecutor);
    }
}
