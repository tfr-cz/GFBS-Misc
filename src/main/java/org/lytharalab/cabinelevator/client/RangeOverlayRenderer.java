package org.lytharalab.cabinelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlockEntity;

/**
 * Renders the creative range box from the controller's block entity renderer
 * instead of subscribing to a global level render stage event.
 */
@OnlyIn(Dist.CLIENT)
public final class RangeOverlayRenderer implements BlockEntityRenderer<ElevatorControllerBlockEntity> {
    public RangeOverlayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ElevatorControllerBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!RangeOverlay.isShowing(be.getBlockPos())) {
            return;
        }
        AABB world = RangeOverlay.shownAabb();
        if (world == null) {
            return;
        }
        AABB box = world.move(-be.getBlockPos().getX(), -be.getBlockPos().getY(), -be.getBlockPos().getZ());
        VertexConsumer buffer = buffers.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, buffer, box, 0.72F, 0.45F, 0.16F, 0.9F);
        LevelRenderer.renderLineBox(poseStack, buffer, box.deflate(0.05D), 0.95F, 0.78F, 0.45F, 0.45F);
    }
}
