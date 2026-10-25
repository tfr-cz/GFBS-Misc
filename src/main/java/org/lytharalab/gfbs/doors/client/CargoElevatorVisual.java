package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.lytharalab.gfbs.gltf.api.client.material.GltfMaterialOverride;
import org.lytharalab.gfbs.gltf.api.client.material.GltfMaterialVariant;
import org.lytharalab.gfbs.gltf.api.model.AlphaMode;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

import java.util.LinkedHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class CargoElevatorVisual {
    private final GltfAsset asset;
    private final Map<String, GltfInstance> parts = new LinkedHashMap<>();

    private static final GltfMaterialOverride ALPHA_CUTOUT = GltfMaterialOverride.builder()
            .alphaMode(AlphaMode.MASK)
            .alphaCutoff(0.5F)
            .doubleSided(true)
            .build();

    public CargoElevatorVisual(GltfAsset asset) {
        this.asset = asset;
        for (String part : new String[]{"Base", "Platform", "Piston", "Guides"}) {
            GltfInstance instance = new GltfInstance(asset);
            instance.renderOptions().frustumCulling(false).castShadows(true);
            for (int i = 0; i < asset.nodes().size(); i++) {
                instance.setNodeVisible(i, baseName(asset.nodes().get(i).name()).equals(part));
            }
            instance.nodes().defineVariant("alpha_cutout", GltfMaterialVariant.override(ALPHA_CUTOUT));
            for (int i = 0; i < asset.nodes().size(); i++) {
                instance.nodes().applyVariant(i, false, "alpha_cutout");
            }
            parts.put(part, instance);
        }
    }

    private static String baseName(String nodeName) {
        int slash = nodeName.indexOf('/');
        return slash < 0 ? nodeName : nodeName.substring(0, slash);
    }

    public GltfAsset asset() {
        return asset;
    }

    public void renderPart(String part, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        GltfInstance instance = parts.get(part);
        if (instance != null) {
            GltfRenderer.render(instance, poseStack, buffers, light, overlay);
        }
    }

    public void close() {
        for (GltfInstance instance : parts.values()) instance.close();
        parts.clear();
    }
}
