package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.lytharalab.gfbs.gltf.api.model.GltfNode;

@OnlyIn(Dist.CLIENT)
public final class DoorVisual {
    private final DoorSpec spec;
    private final GltfAsset asset;
    private final GltfInstance instance;
    private final Map<String, int[]> partNodes = new LinkedHashMap<>();
    private final Map<String, Integer> partMaterial = new HashMap<>();
    private static final GltfMaterialOverride ALPHA_CUTOUT = GltfMaterialOverride.builder()
        .alphaMode(AlphaMode.MASK)
        .alphaCutoff(0.5F)
        .doubleSided(true)
        .build();
    private int appliedSkin = -1;
    private final Map<String, Integer> materialIndex = new HashMap<>();
    private String activePart = "\u0000unset";
    private static final String UNSET = "\u0000unset";
    private static final String ALL_VISIBLE = "\u0000all";
    private final int nodeCount;
    private int appliedVariant = -1;

    public DoorVisual(DoorSpec spec, GltfAsset asset) {
        this.spec = spec;
        this.asset = asset;
        this.nodeCount = asset.nodes().size();
        this.instance = new GltfInstance(asset);
        this.instance.renderOptions().frustumCulling(false).castShadows(true);
        GltfCompat.applyOptions(this.instance.renderOptions());

        for (String part : spec.parts) {
            List nodes = new ArrayList();

            for (int i = 0; i < this.nodeCount; i++) {
                if (baseName(((GltfNode)asset.nodes().get(i)).name()).equals(part)) {
                    nodes.add(i);
                }
            }

            int[] arr = new int[nodes.size()];

            for (int ix = 0; ix < arr.length; ix++) {
                arr[ix] = (Integer)nodes.get(ix);
            }

            this.partNodes.put(part, arr);
        }
    }

    public GltfAsset asset() {
        return this.asset;
    }

    public void applySkin(int skin) {
        if (this.appliedSkin != skin) {
            this.appliedSkin = skin;
            this.partMaterial.clear();

            for (String part : this.partNodes.keySet()) {
                this.partMaterial.put(part, this.materialIndex(this.spec.materialFor(part, skin)));
            }

            this.activePart = "\u0000unset";
            this.appliedVariant = -1;
        }
    }

    private void showPart(String part) {
        if (!part.equals(this.activePart)) {
            this.activePart = part;
            int[] wanted = "\u0000all".equals(part) ? null : (int[])this.partNodes.get(part);

            for (int i = 0; i < this.nodeCount; i++) {
                boolean visible = wanted == null || contains(wanted, i);
                this.instance.setNodeVisible(i, visible);
            }

            if (!"\u0000all".equals(part)) {
                Integer mat = (Integer)this.partMaterial.get(part);
                if (mat != null && mat >= 0 && mat != this.appliedVariant) {
                    this.appliedVariant = mat;
                    this.instance.nodes().defineVariant("gfbs_skin", new GltfMaterialVariant(mat, ALPHA_CUTOUT));

                    for (int i = 0; i < this.nodeCount; i++) {
                        this.instance.nodes().applyVariant(i, false, "gfbs_skin");
                    }
                }
            }
        }
    }

    private static boolean contains(int[] arr, int value) {
        for (int v : arr) {
            if (v == value) {
                return true;
            }
        }

        return false;
    }

    public void setNodeMatrix(String part, float[] columnMajor) {
        for (int i = 0; i < this.nodeCount; i++) {
            if (baseName(((GltfNode)this.asset.nodes().get(i)).name()).equals(part)) {
                this.instance.nodes().node(i).localMatrix(columnMajor);
            }
        }
    }

    public void renderWhole(PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        this.showPart("\u0000all");
        GltfRenderer.render(this.instance, poseStack, buffers, packedLight, packedOverlay);
    }

    private static String baseName(String nodeName) {
        int slash = nodeName.indexOf(47);
        return slash < 0 ? nodeName : nodeName.substring(0, slash);
    }

    private int materialIndex(String name) {
        if (this.materialIndex.isEmpty()) {
            for (int i = 0; i < this.asset.materials().size(); i++) {
                GltfMaterial material = (GltfMaterial)this.asset.materials().get(i);
                this.materialIndex.putIfAbsent(material.name(), i);
            }
        }

        Integer index = (Integer)this.materialIndex.get(name);
        return index == null ? -1 : index;
    }

    public void renderPart(String part, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (this.partNodes.containsKey(part)) {
            this.showPart(part);
            GltfRenderer.render(this.instance, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    public void renderPartRepeated(String part, int count, float step, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (count > 0 && this.partNodes.containsKey(part)) {
            this.showPart(part);

            for (int i = 0; i < count; i++) {
                poseStack.pushPose();
                poseStack.translate(0.0, (double)((float)i * step), 0.0);
                GltfRenderer.render(this.instance, poseStack, buffers, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
    }

    public boolean hasPart(String part) {
        return this.partNodes.containsKey(part);
    }

    public Iterable<String> parts() {
        return this.partNodes.keySet();
    }

    public void close() {
        this.partNodes.clear();
        this.partMaterial.clear();
        this.materialIndex.clear();
        this.instance.close();
    }
}
