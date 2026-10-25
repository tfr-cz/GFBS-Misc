package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.lytharalab.gfbs.gltf.api.client.material.GltfMaterialOverride;
import org.lytharalab.gfbs.gltf.api.client.material.GltfMaterialVariant;
import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.lytharalab.gfbs.gltf.api.model.GltfMaterial;

import java.util.LinkedHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class DoorVisual {
    private final DoorSpec spec;
    private final GltfAsset asset;
    private final Map<String, GltfInstance> parts = new LinkedHashMap<>();
    private GltfInstance whole;
    private static final GltfMaterialOverride ALPHA_CUTOUT = GltfMaterialOverride.builder()
            .alphaMode(AlphaMode.MASK)
            .alphaCutoff(0.5F)
            .doubleSided(true)
            .build();
    private int appliedSkin = -1;

    public DoorVisual(DoorSpec spec, GltfAsset asset) {
        this.spec = spec;
        this.asset = asset;

        for (String part : spec.parts) {
            GltfInstance instance = new GltfInstance(asset);
            instance.renderOptions().frustumCulling(false).castShadows(true);
            for (int i = 0; i < asset.nodes().size(); i++) {
                instance.setNodeVisible(i, baseName(asset.nodes().get(i).name()).equals(part));
            }
            parts.put(part, instance);
        }
    }

    public GltfAsset asset() {
        return asset;
    }

    public void applySkin(int skin) {
        if (appliedSkin == skin) return;
        appliedSkin = skin;

        for (Map.Entry<String, GltfInstance> entry : parts.entrySet()) {
            applyVariant(entry.getValue(), entry.getKey(), skin);
        }
        if (whole != null) {
            applyVariant(whole, "frame", skin);
        }
    }

    private void applyVariant(GltfInstance instance, String part, int skin) {
        int materialIndex = materialIndex(spec.materialFor(part, skin));
        if (materialIndex < 0) return;
        instance.nodes().defineVariant("gfbs_skin", new GltfMaterialVariant(materialIndex, ALPHA_CUTOUT));
        for (int i = 0; i < asset.nodes().size(); i++) {
            instance.nodes().applyVariant(i, false, "gfbs_skin");
        }
    }

    private GltfInstance ensureWhole() {
        if (whole == null) {
            whole = new GltfInstance(asset);
            whole.renderOptions().frustumCulling(false).castShadows(true);
            if (appliedSkin >= 0) {
                applyVariant(whole, "frame", appliedSkin);
            }
        }
        return whole;
    }

    public void setNodeMatrix(String part, float[] columnMajor) {
        GltfInstance instance = ensureWhole();
        for (int i = 0; i < asset.nodes().size(); i++) {
            if (baseName(asset.nodes().get(i).name()).equals(part)) {
                instance.nodes().node(i).localMatrix(columnMajor);
            }
        }
    }

    public void renderWhole(PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        GltfRenderer.render(ensureWhole(), poseStack, buffers, packedLight, packedOverlay);
    }

    private static String baseName(String nodeName) {
        int slash = nodeName.indexOf('/');
        return slash < 0 ? nodeName : nodeName.substring(0, slash);
    }

    private int materialIndex(String name) {
        for (int i = 0; i < asset.materials().size(); i++) {
            GltfMaterial material = asset.materials().get(i);
            if (material.name().equals(name)) return i;
        }
        return -1;
    }

    public void renderPart(String part, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        GltfInstance instance = parts.get(part);
        if (instance != null) {
            GltfRenderer.render(instance, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    public boolean hasPart(String part) {
        return parts.containsKey(part);
    }

    public Iterable<String> parts() {
        return parts.keySet();
    }

    public void close() {
        for (GltfInstance instance : parts.values()) {
            instance.close();
        }
        parts.clear();
        if (whole != null) {
            whole.close();
            whole = null;
        }
    }
}
