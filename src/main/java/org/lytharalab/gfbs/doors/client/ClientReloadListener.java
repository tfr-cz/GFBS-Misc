package org.lytharalab.gfbs.doors.client;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.client.armor.HazmatVisorModel;

@OnlyIn(Dist.CLIENT)
public final class ClientReloadListener implements PreparableReloadListener {
    public CompletableFuture reload(
        PreparationBarrier barrier,
        ResourceManager resourceManager,
        ProfilerFiller preparationsProfiler,
        ProfilerFiller reloadProfiler,
        Executor backgroundExecutor,
        Executor gameExecutor
    ) {
        return barrier.wait(Unit.INSTANCE).thenRunAsync(() -> {
            DoorClientSoundHooks.clearAll();
            DoorVisualCache.clear();
            DoorModelAssets.clear();
            DoorItemRenderer.clear();
            HazmatVisorModel.clear();
            TransitionSealAnimation.clear();
        }, gameExecutor);
    }
}
