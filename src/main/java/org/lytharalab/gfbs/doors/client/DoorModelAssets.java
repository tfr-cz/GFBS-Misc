package org.lytharalab.gfbs.doors.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.GFBSDoors;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class DoorModelAssets {
    private static final Logger LOGGER = LoggerFactory.getLogger(GFBSDoors.MODID);
    private static final Map<ResourceLocation, CompletableFuture<GltfAsset>> REQUESTS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> KNOWN = new HashSet<>();

    private DoorModelAssets() {
    }

    @Nullable
    public static GltfAsset get(String modelPath) {
        ResourceLocation id = new ResourceLocation(GFBSDoors.MODID, "models/" + modelPath + ".obj");
        Optional<GltfAsset> loaded = ClientGltfApi.models().getIfLoaded(id);
        if (loaded.isPresent()) return loaded.get();

        REQUESTS.computeIfAbsent(id, key -> {
            KNOWN.add(key);
            CompletableFuture<GltfAsset> future = ClientGltfApi.models().load(key);
            future.whenComplete((asset, error) -> {
                if (error != null) {
                    REQUESTS.remove(key, future);
                    LOGGER.error("Unable to load door model {}", key, error);
                }
            });
            return future;
        });
        return null;
    }

    public static void clear() {
        REQUESTS.clear();
        for (ResourceLocation id : KNOWN) {
            ClientGltfApi.models().invalidate(id);
        }
        KNOWN.clear();
    }
}
