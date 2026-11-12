package org.lytharalab.gfbs.doors.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OnlyIn(Dist.CLIENT)
public final class DoorModelAssets {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors");
    private static final Map<ResourceLocation, CompletableFuture<GltfAsset>> REQUESTS = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> KNOWN = ConcurrentHashMap.newKeySet();
    private static final Map<String, ResourceLocation> ID_CACHE = new HashMap<>(32);

    private DoorModelAssets() {
    }

    private static ResourceLocation idFor(String modelPath) {
        ResourceLocation cached = (ResourceLocation)ID_CACHE.get(modelPath);
        if (cached != null) {
            return cached;
        } else {
            ResourceLocation id = new ResourceLocation("gfbs_doors", "models/" + modelPath + ".obj");
            ID_CACHE.put(modelPath, id);
            return id;
        }
    }

    @Nullable
    public static GltfAsset get(String modelPath) {
        ResourceLocation id = idFor(modelPath);
        Optional loaded = ClientGltfApi.models().getIfLoaded(id);
        if (loaded.isPresent()) {
            return (GltfAsset)loaded.get();
        } else {
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
    }

    public static void clear() {
        REQUESTS.clear();

        for (ResourceLocation id : KNOWN) {
            ClientGltfApi.models().invalidate(id);
        }

        KNOWN.clear();
        ID_CACHE.clear();
    }
}
