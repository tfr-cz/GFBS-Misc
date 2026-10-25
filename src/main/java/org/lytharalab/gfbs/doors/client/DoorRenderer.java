package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

@OnlyIn(Dist.CLIENT)
public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    public DoorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DoorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!be.isCore()) return;

        DoorSpec spec = be.getSpec();
        GltfAsset asset = DoorModelAssets.get(spec.model);
        if (asset == null) return;

        DoorVisual visual = DoorVisualCache.get(be, spec, asset);
        visual.applySkin(be.getSkin());

        float open = Mth.clamp(be.getOpenProgress(partialTick), 0F, 1F);
        float ticks = be.getOpenTicks(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        float yaw = switch (be.getFacing()) {
            case NORTH -> 90F;
            case WEST -> 180F;
            case SOUTH -> 270F;
            default -> 0F;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));

        renderModel(spec, visual, open, ticks, poseStack, buffers, packedLight, packedOverlay, be.getState());

        poseStack.popPose();
    }

    public static void renderModel(DoorSpec spec, DoorVisual visual, float open, float ticks,
                                   PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        renderModel(spec, visual, open, ticks, poseStack, buffers, packedLight, packedOverlay, (byte) 0);
    }

    public static void renderModel(DoorSpec spec, DoorVisual visual, float open, float ticks,
                                   PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay,
                                   byte state) {
        switch (spec.id) {
            case "vault_door" -> renderVault(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "fire_door" -> renderFire(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "sliding_blast_door" -> renderSlidingBlast(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "sliding_seal_door" -> renderSeal(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "secure_access_door" -> renderSecure(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "round_airlock_door" -> renderAirlock(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "qe_sliding_door" -> renderQeSliding(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "qe_containment" -> renderContainment(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "water_door" -> renderWater(visual, open, state, poseStack, buffers, packedLight, packedOverlay);
            case "silo_hatch" -> renderSilo(visual, ticks, -1.875F, poseStack, buffers, packedLight, packedOverlay);
            case "silo_hatch_large" -> renderSilo(visual, ticks, -2.875F, poseStack, buffers, packedLight, packedOverlay);
            case "large_vehicle_door" -> renderVehicle(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "cargo_door" -> renderCargo(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "transition_seal" -> renderTransitionSeal(visual, open, poseStack, buffers, packedLight, packedOverlay);
            case "blast_door" -> renderLegacyBlastDoor(visual, open, poseStack, buffers, packedLight, packedOverlay);
            default -> renderPart(visual, "frame", poseStack, buffers, packedLight, packedOverlay);
        }

    }

    private static void renderVault(DoorVisual visual, float open, PoseStack pose,
                                    MultiBufferSource buffers, int light, int overlay) {
        float t = open * 6000F;
        float pull = sinFull(clamp01(t / 2000F));
        float slide = clamp01((t - 2000F) / 4000F) * 5F;
        double roll = 360D * slide / (4.25D * Math.PI);

        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(-pull, 0D, slide);
        rotateAround(pose, 0D, 2.5D, 0D, (float) roll, Axis.XP);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        renderPart(visual, "Label", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderFire(DoorVisual visual, float open, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        pose.translate(-0.5D, 0D, 0D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0D, 2.75D * open, 0D);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSlidingBlast(DoorVisual visual, float open, PoseStack pose,
                                           MultiBufferSource buffers, int light, int overlay) {
        float t = open * 1200F;
        float lock = clamp01(t / 200F);
        float rise;
        if (t < 350F) {
            rise = 0F;
        } else if (t < 550F) {
            rise = 0.05F * ((t - 350F) / 200F);
        } else {
            rise = 0.05F + 0.95F * smoothstep(clamp01((t - 550F) / 650F));
        }
        float lockAngle = 90F + 90F * lock;

        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(0D, 0D, rise * 2.125F);
        renderPart(visual, "LeftDoor", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, 0D, 1.8125D, 0D, lockAngle, Axis.XP);
        renderPart(visual, "RightLock", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 0D, -rise * 2.125F);
        renderPart(visual, "RightDoor", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, 0D, 1.8125D, 0D, lockAngle, Axis.XP);
        renderPart(visual, "LeftLock", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSeal(DoorVisual visual, float open, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5D, 0D, 0D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        // Plain sideways slide like the other sliding doors: the panel runs into the
        // neighbouring wall pocket instead of being squashed into the wall plane.
        pose.translate(0D, 0D, 0.9D * open);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSecure(DoorVisual visual, float open, PoseStack pose,
                                     MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0D, 1D, 0D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0D, 3.5D * open, 0D);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderAirlock(DoorVisual visual, float open, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0D, 0D, 0.5D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(0D, 0D, 1.5D * open);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 0D, -1.5D * open);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderQeSliding(DoorVisual visual, float open, PoseStack pose,
                                        MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.53125D, 0.001D, 0.5D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(0D, 0D, 0.95D * open);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 0D, -0.95D * open);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderContainment(DoorVisual visual, float open, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.25D, 0D, 0D);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0D, 2.25D * open, 0D);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderWater(DoorVisual visual, float open, byte state, PoseStack pose,
                                    MultiBufferSource buffers, int light, int overlay) {
        float doorOpen = open;
        float bolt = open;
        if (state == DoorBlockEntity.STATE_OPENING) {
            bolt = clamp01(open * 2F);
            doorOpen = clamp01(open * 2F - 1F);
        } else if (state == DoorBlockEntity.STATE_CLOSING) {
            float elapsed = 1F - open;
            doorOpen = 1F - clamp01(elapsed * 2F);
            bolt = 1F - clamp01(elapsed * 2F - 1F);
        } else if (state == DoorBlockEntity.STATE_OPEN) {
            doorOpen = 1F;
            bolt = 1F;
        } else {
            doorOpen = 0F;
            bolt = 0F;
        }

        pose.pushPose();
        pose.translate(0.375D, 0D, 0D);
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        rotateAround(pose, -1.1875D, 0D, 0D, -120F * doorOpen, Axis.YP);
        renderPart(visual, "Door_Cube.003", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(-0.4D * bolt, 0D, 0D);
        renderPart(visual, "Bolts", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        rotateAround(pose, 0.40625D, 2.28125D, 0D, 360F * bolt, Axis.ZP);
        renderPart(visual, "Top", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        rotateAround(pose, 0.40625D, 0.71875D, 0D, 360F * bolt, Axis.ZP);
        renderPart(visual, "Bottom", pose, buffers, light, overlay);
        pose.popPose();

        pose.popPose();
        pose.popPose();
    }

    private static void renderSilo(DoorVisual visual, float ticks, float originZ, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        float translate = 0.25F * smoothstep(clamp01(ticks / 10F));
        float rotation = -240F * smoothstep(remap01(ticks, 20F, 100F));

        pose.pushPose();
        pose.translate(0D, 0.875D, originZ);
        pose.mulPose(Axis.XP.rotationDegrees(rotation));
        pose.translate(0D, -0.875D, -originZ);
        pose.translate(0D, translate, 0D);
        renderPart(visual, "Hatch", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderVehicle(DoorVisual visual, float open, PoseStack pose,
                                      MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90F));
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(-3D * open, 0D, 0D);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(3D * open, 0D, 0D);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderCargo(DoorVisual visual, float open, PoseStack pose,
                                    MultiBufferSource buffers, int light, int overlay) {
        renderPart(visual, "Frame", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(0D, open, 0D);
        renderPart(visual, "DoorTop", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, 2D * open, 0D);
        renderPart(visual, "DoorBot", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderTransitionSeal(DoorVisual visual, float open, PoseStack pose,
                                             MultiBufferSource buffers, int light, int overlay) {
        TransitionSealAnimation animation = TransitionSealAnimation.get();
        if (animation != null) {
            float seconds = open * animation.duration();
            for (String node : animation.nodes()) {
                float[] matrix = animation.localMatrix(node, seconds);
                if (matrix != null) visual.setNodeMatrix(node, matrix);
            }
        }
        visual.renderWhole(pose, buffers, light, overlay);
    }

    private static void renderLegacyBlastDoor(DoorVisual visual, float open, PoseStack pose,
                                              MultiBufferSource buffers, int light, int overlay) {
        double lift = 5D * open;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180F));
        renderPart(visual, "Base", pose, buffers, light, overlay);

        pose.pushPose();
        pose.translate(0D, 3D, 0D);
        renderPart(visual, "Block", pose, buffers, light, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0D, lift, 0D);
        renderPart(visual, "Tooth", pose, buffers, light, overlay);
        for (int i = 0; i < 4; i++) {
            pose.pushPose();
            pose.translate(0D, i, 0D);
            renderPart(visual, "Slider", pose, buffers, light, overlay);
            pose.popPose();
        }
        pose.popPose();
        pose.popPose();
    }

    private static void renderPart(DoorVisual visual, String part, PoseStack pose,
                                   MultiBufferSource buffers, int light, int overlay) {
        visual.renderPart(part, pose, buffers, light, overlay);
    }

    private static void rotateAround(PoseStack pose, double x, double y, double z, float angle, Axis axis) {
        pose.translate(x, y, z);
        pose.mulPose(axis.rotationDegrees(angle));
        pose.translate(-x, -y, -z);
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0F, 1F);
    }

    private static float smoothstep(float value) {
        float x = clamp01(value);
        return x * x * (3F - 2F * x);
    }

    private static float sinFull(float value) {
        return 0.5F - 0.5F * Mth.cos(clamp01(value) * (float) Math.PI);
    }

    private static float remap01(float value, float min, float max) {
        if (max <= min) return value >= max ? 1F : 0F;
        return clamp01((value - min) / (max - min));
    }
}
