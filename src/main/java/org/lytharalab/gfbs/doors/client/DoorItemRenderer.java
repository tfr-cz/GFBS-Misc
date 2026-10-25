package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorItem;
import org.lytharalab.gfbs.doors.content.block.DoorItem;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class DoorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static DoorItemRenderer instance;

    private final Map<DoorSpec, DoorVisual> visuals = new HashMap<>();
    private CargoElevatorVisual elevatorVisual;
    private GltfAsset elevatorAsset;

    private DoorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static DoorItemRenderer getInstance() {
        if (instance == null) instance = new DoorItemRenderer();
        return instance;
    }

    public static void clear() {
        if (instance != null) instance.clearCaches();
    }

    private void clearCaches() {
        for (DoorVisual visual : visuals.values()) visual.close();
        visuals.clear();
        if (elevatorVisual != null) elevatorVisual.close();
        elevatorVisual = null;
        elevatorAsset = null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof DoorItem doorItem) {
            renderDoor(doorItem.spec(), context, poseStack, buffers, packedLight, packedOverlay);
        } else if (stack.getItem() instanceof CargoElevatorItem) {
            renderElevator(context, poseStack, buffers, packedLight, packedOverlay);
        }
    }

    private void renderDoor(DoorSpec spec, ItemDisplayContext context, PoseStack poseStack,
                            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        GltfAsset asset = DoorModelAssets.get(spec.model);
        if (asset == null) return;

        DoorVisual visual = visuals.get(spec);
        if (visual == null || visual.asset() != asset) {
            if (visual != null) visual.close();
            visual = new DoorVisual(spec, asset);
            visual.applySkin(0);
            visuals.put(spec, visual);
        }

        View view = viewFor(spec);
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        float scale = targetSize(context) / view.size;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0D, -view.centerY, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees("transition_seal".equals(spec.id) ? 90F : 45F));
        poseStack.mulPose(Axis.XP.rotationDegrees(15F));
        DoorRenderer.renderModel(spec, visual, 0F, 0F, poseStack, buffers,
                context == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderElevator(ItemDisplayContext context, PoseStack poseStack,
                                MultiBufferSource buffers, int packedLight, int packedOverlay) {
        GltfAsset asset = DoorModelAssets.get("machines/elevator");
        if (asset == null) return;
        if (elevatorVisual == null || elevatorAsset != asset) {
            if (elevatorVisual != null) elevatorVisual.close();
            elevatorVisual = new CargoElevatorVisual(asset);
            elevatorAsset = asset;
        }

        float scale = targetSize(context) / 3F;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0D, -0.375D, 0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(45F));
        poseStack.mulPose(Axis.XP.rotationDegrees(15F));
        int light = context == ItemDisplayContext.GUI ? LightTexture.FULL_BRIGHT : packedLight;
        elevatorVisual.renderPart("Base", poseStack, buffers, light, packedOverlay);
        elevatorVisual.renderPart("Platform", poseStack, buffers, light, packedOverlay);
        elevatorVisual.renderPart("Piston", poseStack, buffers, light, packedOverlay);
        elevatorVisual.renderPart("Guides", poseStack, buffers, light, packedOverlay);
        poseStack.popPose();
    }

    private static float targetSize(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> 1.2F;
            case FIXED -> 0.9F;
            case GROUND -> 0.45F;
            default -> 0.7F;
        };
    }

    private static View viewFor(DoorSpec spec) {
        return switch (spec.id) {
            case "fire_door" -> new View(4.0F, 1.5F);
            case "sliding_blast_door" -> new View(7.0F, 2.0F);
            case "secure_access_door" -> new View(5.0F, 1.5F);
            case "qe_sliding_door" -> new View(3.7F, 1.0F);
            case "qe_containment" -> new View(3.0F, 1.5F);
            case "water_door" -> new View(3.0F, 1.5F);
            case "cargo_door" -> new View(3.0F, 1.5F);
            case "large_vehicle_door" -> new View(7.0F, 3.0F);
            case "vault_door" -> new View(9.0F, 2.5F);
            case "silo_hatch" -> new View(5.0F, 0.5F);
            case "silo_hatch_large" -> new View(7.0F, 0.5F);
            case "round_airlock_door" -> new View(4.0F, 2.0F);
            case "sliding_seal_door" -> new View(2.0F, 1.0F);
            case "transition_seal" -> new View(30.0F, 12.0F);
            case "blast_door" -> new View(7.0F, 3.0F);
            default -> new View(4.0F, 1.5F);
        };
    }

    private record View(float size, float centerY) {
    }
}
