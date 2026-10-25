package org.lytharalab.gfbs.doors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

@OnlyIn(Dist.CLIENT)
public final class CargoElevatorRenderer implements BlockEntityRenderer<CargoElevatorBlockEntity> {
    public CargoElevatorRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CargoElevatorBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        if (!be.isCore()) return;

        GltfAsset asset = DoorModelAssets.get("machines/elevator");
        if (asset == null) return;

        CargoElevatorVisual visual = CargoElevatorVisualCache.get(be, asset);
        double extension = be.getExtensionInterp(partialTick);
        int height = be.getHeight();

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);

        visual.renderPart("Base", poseStack, buffers, packedLight, packedOverlay);

        for (int i = 0; i <= height; i++) {
            poseStack.pushPose();
            poseStack.translate(0D, i, 0D);
            visual.renderPart("Guides", poseStack, buffers, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0D, extension, 0D);
        visual.renderPart("Platform", poseStack, buffers, packedLight, packedOverlay);
        poseStack.popPose();

        int pistons = Mth.floor(extension) + 1;
        for (int i = 0; i < pistons; i++) {
            poseStack.pushPose();
            poseStack.translate(0D, extension - i, 0D);
            visual.renderPart("Piston", poseStack, buffers, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.popPose();
    }
}
