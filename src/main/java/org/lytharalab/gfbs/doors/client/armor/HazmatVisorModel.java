package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.CompletionException;

/**
 * Loads and owns the glTF instance of the visor pane.
 *
 * <p>The pane is a single quad under {@code assets/gfbs_doors/models/armor/visor.obj}. Its material
 * has {@code d 0.45}, which {@code ObjAssetImporter} imports as {@code AlphaMode.BLEND}, so the
 * renderer picks its translucent render type - real partial alpha, which the armour layer's alpha
 * test cannot express. Culling is forced off because a quad seen from behind would otherwise vanish
 * the moment the head turns away from the camera.</p>
 *
 * <p>The asset loads asynchronously; until it arrives (and if it never does) {@link HazmatVisorLayer}
 * falls back to drawing the pane from the armour model, so the visor never simply disappears.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatVisorModel {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors/visor");
    private static final ResourceLocation MODEL =
        new ResourceLocation("gfbs_doors", "models/armor/visor.obj");

    private static GltfInstance instance;
    private static boolean requested;

    private HazmatVisorModel() {
    }

    /** The ready instance, or {@code null} while the asset is still loading. */
    @Nullable
    public static GltfInstance instance() {
        return instance;
    }

    /** Kicks off the load; safe to call every frame. */
    public static void request() {
        if (instance != null || requested) {
            return;
        }
        requested = true;
        ClientGltfApi.models().load(MODEL).whenComplete((asset, error) -> {
            if (error != null) {
                LOGGER.error("Unable to load the visor model {}", MODEL,
                    error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                return;
            }
            Minecraft.getInstance().execute(() -> build(asset));
        });
    }

    private static void build(GltfAsset asset) {
        GltfInstance created = new GltfInstance(asset);
        created.renderOptions()
            .frustumCulling(false)
            .occlusionCulling(false)
            .castShadows(false)
            .cull(GltfRenderOptions.CullMode.FORCE_NO_CULL);
        instance = created;
    }

    /** Drops the instance so the next frame reloads it - resource reload and disconnect both need this. */
    public static void clear() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        requested = false;
        ClientGltfApi.models().invalidate(MODEL);
    }
}
