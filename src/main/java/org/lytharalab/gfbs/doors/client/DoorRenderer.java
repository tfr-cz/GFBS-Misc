package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorGeometry;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.DoorSpecs;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

@OnlyIn(Dist.CLIENT)
public final class DoorRenderer implements BlockEntityRenderer<DoorBlockEntity> {
    public DoorRenderer(Context context) {
    }

    public void render(DoorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (be.isCore()) {
            DoorSpec spec = be.getSpec();
            GltfAsset asset = DoorModelAssets.get(spec.model);
            if (asset != null) {
                DoorVisual visual = DoorVisualCache.get(be, spec, asset);
                visual.applySkin(be.getSkin());
                float open = Mth.clamp(be.getOpenProgress(partialTick), 0.0F, 1.0F);
                float ticks = be.getOpenTicks(partialTick);
                poseStack.pushPose();
                poseStack.translate(0.5, 0.0, 0.5);
                applyLargeScale(spec, be.getFacing(), poseStack);

                float yaw = switch (be.getFacing()) {
                    case NORTH -> 90.0F;
                    case WEST -> 180.0F;
                    case SOUTH -> 270.0F;
                    default -> 0.0F;
                };
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                renderModel(spec, visual, open, ticks, poseStack, buffers, packedLight, packedOverlay, be.getState(), be.getExtraHeight());
                poseStack.popPose();
            }
        }
    }

    private static void applyLargeScale(DoorSpec spec, Direction facing, PoseStack poseStack) {
        if (spec.baseId != null) {
            DoorSpec base = (DoorSpec)DoorSpecs.BY_ID.get(spec.baseId);
            if (base != null) {
                int[] oldRot = DoorGeometry.rotateDims(base.dimensions, facing);
                int[] newRot = DoorGeometry.rotateDims(spec.dimensions, facing);
                if (facing != Direction.NORTH && facing != Direction.SOUTH) {
                    double oldLow = (double)(-oldRot[2]) - 0.5;
                    double newLow = (double)(-newRot[2]) - 0.5;
                    poseStack.translate(0.0, 0.0, (double)((float)(newLow - (double)spec.scaleW * oldLow)));
                    poseStack.scale(1.0F, spec.scaleH, spec.scaleW);
                } else {
                    double oldLow = (double)(-oldRot[4]) - 0.5;
                    double newLow = (double)(-newRot[4]) - 0.5;
                    poseStack.translate((double)((float)(newLow - (double)spec.scaleW * oldLow)), 0.0, 0.0);
                    poseStack.scale(spec.scaleW, spec.scaleH, 1.0F);
                }
            }
        }
    }

    public static boolean modelWidthIsX(DoorSpec spec) {
        String baseId = spec.baseId != null ? spec.baseId : spec.id;
        return "fire_door".equals(baseId) || "water_door".equals(baseId) || "large_vehicle_door".equals(baseId) || "reactor_door".equals(baseId);
    }

    public static void renderModel(
        DoorSpec spec, DoorVisual visual, float open, float ticks, PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay
    ) {
        renderModel(spec, visual, open, ticks, poseStack, buffers, packedLight, packedOverlay, (byte)0, 0);
    }

    public static void renderModel(
        DoorSpec spec,
        DoorVisual visual,
        float open,
        float ticks,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay,
        byte state
    ) {
        renderModel(spec, visual, open, ticks, poseStack, buffers, packedLight, packedOverlay, state, 0);
    }

    public static void renderModel(
        DoorSpec spec,
        DoorVisual visual,
        float open,
        float ticks,
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay,
        byte state,
        int extraHeight
    ) {
        DoorSpec shape = spec.baseId == null ? spec : (DoorSpec)DoorSpecs.BY_ID.getOrDefault(spec.baseId, spec);
        String var11 = shape.id;
        switch (var11) {
            case "vault_door":
                renderVault(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "fire_door":
                renderFire(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "sliding_blast_door":
                renderSlidingBlast(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "sliding_seal_door":
                renderSeal(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "secure_access_door":
                renderSecure(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "round_airlock_door":
                renderAirlock(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "qe_sliding_door":
                renderQeSliding(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "qe_containment":
                renderContainment(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "water_door":
                renderWater(visual, open, state, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "reactor_door":
                renderReactor(visual, spec, ticks, state, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "silo_hatch":
                renderSilo(visual, ticks, -1.875F, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "silo_hatch_large":
                renderSilo(visual, ticks, -2.875F, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "large_vehicle_door":
                renderVehicle(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "cargo_door":
                renderCargo(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "transition_seal":
                renderTransitionSeal(visual, open, poseStack, buffers, packedLight, packedOverlay);
                break;
            case "blast_door":
                renderLegacyBlastDoor(visual, open, extraHeight, poseStack, buffers, packedLight, packedOverlay);
                break;
            default:
                renderPart(visual, "frame", poseStack, buffers, packedLight, packedOverlay);
        }
    }

    private static void renderVault(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float t = open * 6000.0F;
        float pull = sinFull(clamp01(t / 2000.0F));
        float slide = clamp01((t - 2000.0F) / 4000.0F) * 5.0F;
        double roll = 360.0 * (double)slide / 13.351768777756622;
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate((double)(-pull), 0.0, (double)slide);
        rotateAround(pose, 0.0, 2.5, 0.0, (float)roll, Axis.XP);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        renderPart(visual, "Label", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderFire(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(-0.5, 0.0, 0.0);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 2.75 * (double)open, 0.0);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSlidingBlast(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float t = open * 1200.0F;
        float lock = clamp01(t / 200.0F);
        float rise;
        if (t < 350.0F) {
            rise = 0.0F;
        } else if (t < 550.0F) {
            rise = 0.05F * ((t - 350.0F) / 200.0F);
        } else {
            rise = 0.05F + 0.95F * smoothstep(clamp01((t - 550.0F) / 650.0F));
        }

        float lockAngle = 90.0F + 90.0F * lock;
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 0.0, (double)(rise * 2.125F));
        renderPart(visual, "LeftDoor", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, 0.0, 1.8125, 0.0, lockAngle, Axis.XP);
        renderPart(visual, "RightLock", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0, 0.0, (double)(-rise * 2.125F));
        renderPart(visual, "RightDoor", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, 0.0, 1.8125, 0.0, lockAngle, Axis.XP);
        renderPart(visual, "LeftLock", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSeal(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.5, 0.0, 0.0);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 0.0, 0.9 * (double)open);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderSecure(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.0, 1.0, 0.0);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 3.5 * (double)open, 0.0);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderAirlock(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.0, 0.0, 0.5);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 0.0, 1.5 * (double)open);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0, 0.0, -1.5 * (double)open);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderQeSliding(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.53125, 0.001, 0.5);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 0.0, 0.95 * (double)open);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0, 0.0, -0.95 * (double)open);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderContainment(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(0.25, 0.0, 0.0);
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 2.25 * (double)open, 0.0);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderWater(DoorVisual visual, float open, byte state, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float doorOpen;
        float bolt;
        if (state == 3) {
            bolt = clamp01(open * 2.0F);
            doorOpen = clamp01(open * 2.0F - 1.0F);
        } else if (state == 2) {
            float elapsed = 1.0F - open;
            doorOpen = 1.0F - clamp01(elapsed * 2.0F);
            bolt = 1.0F - clamp01(elapsed * 2.0F - 1.0F);
        } else if (state == 1) {
            doorOpen = 1.0F;
            bolt = 1.0F;
        } else {
            doorOpen = 0.0F;
            bolt = 0.0F;
        }

        pose.pushPose();
        pose.translate(0.375, 0.0, 0.0);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, -1.1875, 0.0, 0.0, -120.0F * doorOpen, Axis.YP);
        renderPart(visual, "Door_Cube.003", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(-0.4 * (double)bolt, 0.0, 0.0);
        renderPart(visual, "Bolts", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        rotateAround(pose, 0.40625, 2.28125, 0.0, 360.0F * bolt, Axis.ZP);
        renderPart(visual, "Top", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        rotateAround(pose, 0.40625, 0.71875, 0.0, 360.0F * bolt, Axis.ZP);
        renderPart(visual, "Bottom", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
        pose.popPose();
    }

    private static void renderReactor(
        DoorVisual visual, DoorSpec spec, float ticks, byte state, PoseStack pose, MultiBufferSource buffers, int light, int overlay
    ) {
        float total = Math.max(1.0F, (float)spec.timeToOpen);
        float doorRatio = 0.0F;
        float handleRatio = 0.0F;
        if (state == 1) {
            doorRatio = 1.0F;
            handleRatio = 1.0F;
        } else if (state == 3) {
            handleRatio = sinFull(clamp01(ticks / 20.0F));
            doorRatio = sinFull(clamp01((ticks - 43.0F) / 44.0F));
        } else if (state == 2) {
            float elapsed = total - ticks;
            doorRatio = 1.0F - sinFull(clamp01(elapsed / 44.0F));
            handleRatio = 1.0F - sinFull(clamp01((elapsed - 66.0F) / 20.0F));
        }

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0, 0.0, -0.5);
        renderPart(visual, "FrameA", pose, buffers, light, overlay);
        renderPart(visual, "FrameB", pose, buffers, light, overlay);
        renderPart(visual, "Wall", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, -0.625, 0.0, 0.5, -140.0F * doorRatio, Axis.YP);
        renderPart(visual, "Door", pose, buffers, light, overlay);
        pose.pushPose();
        rotateAround(pose, 0.0, 1.0625, 0.0, -90.0F * handleRatio, Axis.ZP);
        renderPart(visual, "Handle", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
        pose.popPose();
    }

    private static void renderSilo(DoorVisual visual, float ticks, float originZ, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        float translate = 0.25F * smoothstep(clamp01(ticks / 10.0F));
        float rotation = -240.0F * smoothstep(remap01(ticks, 20.0F, 100.0F));
        pose.pushPose();
        pose.translate(0.0, 0.875, (double)originZ);
        pose.mulPose(Axis.XP.rotationDegrees(rotation));
        pose.translate(0.0, -0.875, (double)(-originZ));
        pose.translate(0.0, (double)translate, 0.0);
        renderPart(visual, "Hatch", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderVehicle(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(-3.0 * (double)open, 0.0, 0.0);
        renderPart(visual, "Left", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(3.0 * (double)open, 0.0, 0.0);
        renderPart(visual, "Right", pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderCargo(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        renderPart(visual, "Frame", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, (double)open, 0.0);
        renderPart(visual, "DoorTop", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0, 2.0 * (double)open, 0.0);
        renderPart(visual, "DoorBot", pose, buffers, light, overlay);
        pose.popPose();
    }

    private static void renderTransitionSeal(DoorVisual visual, float open, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        TransitionSealAnimation animation = TransitionSealAnimation.get();
        if (animation != null) {
            float seconds = open * animation.duration();

            for (String node : animation.nodes()) {
                float[] matrix = animation.localMatrix(node, seconds);
                if (matrix != null) {
                    visual.setNodeMatrix(node, matrix);
                }
            }
        }

        visual.renderWhole(pose, buffers, light, overlay);
    }

    private static void renderLegacyBlastDoor(DoorVisual visual, float open, int extraHeight, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        int extra = Math.max(0, extraHeight);
        double lift = (5.0 + (double)extra) * (double)open;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        renderPart(visual, "Base", pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(0.0, 3.0 + (double)extra, 0.0);
        renderPart(visual, "Block", pose, buffers, light, overlay);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.0, lift, 0.0);
        renderPart(visual, "Tooth", pose, buffers, light, overlay);
        visual.renderPartRepeated("Slider", 4 + extra, 1.0F, pose, buffers, light, overlay);
        pose.popPose();
        pose.popPose();
    }

    private static void renderPart(DoorVisual visual, String part, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        visual.renderPart(part, pose, buffers, light, overlay);
    }

    private static void rotateAround(PoseStack pose, double x, double y, double z, float angle, Axis axis) {
        pose.translate(x, y, z);
        pose.mulPose(axis.rotationDegrees(angle));
        pose.translate(-x, -y, -z);
    }

    private static float clamp01(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static float smoothstep(float value) {
        float x = clamp01(value);
        return x * x * (3.0F - 2.0F * x);
    }

    private static float sinFull(float value) {
        return 0.5F - 0.5F * Mth.cos(clamp01(value) * (float) Math.PI);
    }

    private static float remap01(float value, float min, float max) {
        if (max <= min) {
            return value >= max ? 1.0F : 0.0F;
        } else {
            return clamp01((value - min) / (max - min));
        }
    }
}
