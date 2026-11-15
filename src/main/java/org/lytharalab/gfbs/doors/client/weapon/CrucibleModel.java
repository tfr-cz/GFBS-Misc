package org.lytharalab.gfbs.doors.client.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.lytharalab.gfbs.gltf.api.model.GltfBounds;
import org.lytharalab.gfbs.gltf.api.model.GltfMesh;
import org.lytharalab.gfbs.gltf.api.model.GltfNode;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

/**
 * Loads HBM's Crucible mesh and answers the two questions the renderer has about it: how big it is, and which
 * nodes make up the glowing blade.
 *
 * <p>The OBJ importer names nodes {@code <object>/<material>}, and the import (see
 * {@code tools/import_hbm_crucible.py}) gives the blade its own material, so the split is on the object name.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CrucibleModel {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors/crucible");
    private static final ResourceLocation ID = new ResourceLocation("gfbs_doors", "models/weapons/crucible.obj");
    /** HBM binds this part with lighting disabled and full brightness forced. */
    private static final String BLADE = "Blade";

    private static GltfInstance instance;
    private static GltfBounds bounds = GltfBounds.EMPTY;
    private static int[] bladeNodes = new int[0];
    private static int[] bodyNodes = new int[0];
    private static boolean requested;

    private CrucibleModel() {
    }

    @Nullable
    public static GltfInstance instance() {
        if (instance == null) {
            request();
        }
        return instance;
    }

    /** Bounding box of the whole sword in the mesh's own units. */
    public static GltfBounds bounds() {
        return bounds;
    }

    /**
     * Switches the instance between its two passes: the emissive blade alone, or everything else.
     *
     * <p>NTM drew the blade with {@code glDisable(GL_LIGHTING)} and full brightness, so it has to be a separate
     * draw here as well - there is no per-node light level to ask for.</p>
     */
    public static void setBladePass(GltfInstance model, boolean bladeOnly) {
        for (int node : bladeNodes) {
            model.setNodeVisible(node, bladeOnly);
        }
        for (int node : bodyNodes) {
            model.setNodeVisible(node, !bladeOnly);
        }
    }

    private static void request() {
        if (requested) {
            return;
        }
        requested = true;
        ClientGltfApi.models().load(ID).whenComplete((asset, error) -> {
            if (error != null) {
                requested = false;
                LOGGER.error("Unable to load the Crucible model {}", ID,
                    error instanceof CompletionException && error.getCause() != null ? error.getCause() : error);
                return;
            }
            Minecraft.getInstance().execute(() -> register(asset));
        });
    }

    private static void register(GltfAsset asset) {
        GltfInstance model = new GltfInstance(asset);
        model.renderOptions()
            .frustumCulling(false)
            .occlusionCulling(false)
            .castShadows(false)
            .cull(GltfRenderOptions.CullMode.FORCE_NO_CULL);

        List<Integer> blade = new ArrayList<>();
        List<Integer> body = new ArrayList<>();
        GltfBounds total = GltfBounds.EMPTY;
        for (int index = 0; index < asset.nodes().size(); index++) {
            GltfNode node = asset.nodes().get(index);
            if (isBlade(node.name())) {
                blade.add(index);
            } else {
                body.add(index);
            }
            GltfBounds local = nodeBounds(asset, node);
            if (local.valid()) {
                total = total.union(node.hasMatrix() ? local.transform(nodeMatrix(node)) : local);
            }
        }

        bladeNodes = blade.stream().mapToInt(Integer::intValue).toArray();
        bodyNodes = body.stream().mapToInt(Integer::intValue).toArray();
        bounds = total;
        instance = model;
        LOGGER.info("Crucible mesh loaded: {} nodes ({} blade), bounds y {}..{}",
            asset.nodes().size(), bladeNodes.length, total.minY(), total.maxY());
    }

    private static GltfBounds nodeBounds(GltfAsset asset, GltfNode node) {
        GltfBounds local = GltfBounds.EMPTY;
        for (int meshIndex : node.meshes()) {
            if (meshIndex < 0 || meshIndex >= asset.meshes().size()) {
                continue;
            }
            GltfMesh mesh = asset.meshes().get(meshIndex);
            for (GltfPrimitive primitive : mesh.primitives()) {
                local = local.union(primitive.bounds());
            }
        }
        return local;
    }

    private static Matrix4f nodeMatrix(GltfNode node) {
        float[] m = node.matrix();
        Matrix4f matrix = new Matrix4f();
        matrix.set(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7],
            m[8], m[9], m[10], m[11], m[12], m[13], m[14], m[15]);
        return matrix;
    }

    /**
     * True for the blade, whichever half of {@code <object>/<material>} carries the name.
     *
     * <p>An importer that merges by material would name the node {@code blade} rather than {@code Blade/blade}, and
     * guessing wrong here only costs the glow, so both halves are accepted.</p>
     */
    private static boolean isBlade(String nodeName) {
        for (String part : nodeName.split("/")) {
            if (BLADE.equalsIgnoreCase(part)) {
                return true;
            }
        }
        return false;
    }

    /** Drops the instance so the next frame reloads; used on resource reload and disconnect. */
    public static void clear() {
        if (instance != null) {
            instance.close();
        }
        ClientGltfApi.models().invalidate(ID);
        instance = null;
        bounds = GltfBounds.EMPTY;
        bladeNodes = new int[0];
        bodyNodes = new int[0];
        requested = false;
    }
}
