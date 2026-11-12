package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.block.DoorItem;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.lytharalab.gfbs.gltf.api.model.GltfBounds;
import org.lytharalab.gfbs.gltf.api.model.GltfMesh;
import org.lytharalab.gfbs.gltf.api.model.GltfNode;
import org.lytharalab.gfbs.gltf.api.model.GltfPrimitive;

@OnlyIn(Dist.CLIENT)
public final class DoorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static DoorItemRenderer instance;
    private final Map<DoorSpec, DoorVisual> visuals = new HashMap<>();
    private final Map<DoorSpec, DoorItemRenderer.Fit> fits = new HashMap<>();
    private static final float GUI_FILL = 0.42F;
    private static final float WORLD_FILL = 0.3F;

    private DoorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static DoorItemRenderer getInstance() {
        if (instance == null) {
            instance = new DoorItemRenderer();
        }

        return instance;
    }

    public static void clear() {
        if (instance != null) {
            instance.clearCaches();
        }
    }

    private void clearCaches() {
        for (DoorVisual visual : this.visuals.values()) {
            visual.close();
        }

        this.visuals.clear();
        this.fits.clear();
    }

    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof DoorItem doorItem) {
            this.renderDoor(doorItem.spec(), context, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    private void renderDoor(DoorSpec spec, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        GltfAsset asset = DoorModelAssets.get(spec.model);
        if (asset != null) {
            DoorVisual visual = (DoorVisual)this.visuals.get(spec);
            if (visual == null || visual.asset() != asset) {
                if (visual != null) {
                    visual.close();
                }

                visual = new DoorVisual(spec, asset);
                visual.applySkin(0);
                this.visuals.put(spec, visual);
            }

            DoorItemRenderer.Fit fit = (DoorItemRenderer.Fit)this.fits.get(spec);
            if (fit == null || fit.asset != asset) {
                fit = DoorItemRenderer.Fit.measure(asset, spec);
                this.fits.put(spec, fit);
            }

            float fill = context == ItemDisplayContext.GUI ? 0.42F : 0.3F;
            float scale = fit.radius > 1.0E-4F ? fill / fit.radius : fill;
            poseStack.pushPose();
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.YP.rotationDegrees(itemYaw(spec)));
            poseStack.mulPose(Axis.XP.rotationDegrees(itemPitch(spec)));
            poseStack.scale(scale, scale, scale);
            poseStack.translate(-fit.centerX, -fit.centerY, -fit.centerZ);
            DoorRenderer.renderModel(spec, visual, 0.0F, 0.0F, poseStack, buffers, context == ItemDisplayContext.GUI ? 15728880 : packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static float itemYaw(DoorSpec spec) {
        return 45.0F;
    }

    private static float itemPitch(DoorSpec spec) {
        String baseId = spec.baseId != null ? spec.baseId : spec.id;
        return baseId.startsWith("silo_hatch") ? 45.0F : 5.0F;
    }

    private static final class Fit {
        final GltfAsset asset;
        final float centerX;
        final float centerY;
        final float centerZ;
        final float radius;

        private Fit(GltfAsset asset, GltfBounds b) {
            this.asset = asset;
            this.centerX = b.centerX();
            this.centerY = b.centerY();
            this.centerZ = b.centerZ();
            this.radius = b.radius();
        }

        static DoorItemRenderer.Fit measure(GltfAsset asset, DoorSpec spec) {
            GltfBounds total = GltfBounds.EMPTY;

            for (GltfNode node : asset.nodes()) {
                int[] meshes = node.meshes();
                if (meshes.length != 0) {
                    GltfBounds local = GltfBounds.EMPTY;

                    for (int mi : meshes) {
                        if (mi >= 0 && mi < asset.meshes().size()) {
                            GltfMesh mesh = (GltfMesh)asset.meshes().get(mi);

                            for (GltfPrimitive prim : mesh.primitives()) {
                                local = local.union(prim.bounds());
                            }
                        }
                    }

                    if (local.valid()) {
                        GltfBounds world = node.hasMatrix() ? local.transform(nodeMatrix(node)) : local;
                        total = total.union(world);
                    }
                }
            }

            if (!total.valid()) {
                return new DoorItemRenderer.Fit(asset, new GltfBounds(-0.5F, -0.5F, -0.5F, 0.5F, 0.5F, 0.5F));
            } else {
                if (spec.baseId != null) {
                    boolean widthX = DoorRenderer.modelWidthIsX(spec);
                    float sx = widthX ? spec.scaleW : 1.0F;
                    float sz = widthX ? 1.0F : spec.scaleW;
                    Matrix4f m = new Matrix4f().scale(sx, spec.scaleH, sz);
                    total = total.transform(m);
                }

                return new DoorItemRenderer.Fit(asset, total);
            }
        }

        private static Matrix4f nodeMatrix(GltfNode node) {
            float[] m = node.matrix();
            Matrix4f mat = new Matrix4f();
            mat.set(m[0], m[1], m[2], m[3], m[4], m[5], m[6], m[7], m[8], m[9], m[10], m[11], m[12], m[13], m[14], m[15]);
            return mat;
        }
    }
}
