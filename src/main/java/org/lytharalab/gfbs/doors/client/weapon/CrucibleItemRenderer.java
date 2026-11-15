package org.lytharalab.gfbs.doors.client.weapon;

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
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.lytharalab.gfbs.gltf.api.model.GltfBounds;

/**
 * Draws HBM's Crucible mesh as an item - grid, hand, ground and item frame.
 *
 * <p>The item model is {@code builtin/entity}, so vanilla hands the draw over to this renderer with an identity
 * item transform; every pose below is ours. Two things carry over from NTM:</p>
 * <ul>
 *   <li>the sword is authored blade-up in its own units (about 13 of them, y -1.63 .. 11.38), so the hand poses
 *       rotate it into the classic diagonal and put the grip near the hand rather than the model's centre;</li>
 *   <li>the blade is drawn a second time at full brightness with the rest of the mesh hidden, because 1.7.10 had
 *       {@code glDisable(GL_LIGHTING)} per part and 1.20.1 only has one light level per draw.</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public final class CrucibleItemRenderer extends BlockEntityWithoutLevelRenderer {
    /** Half the on-screen length in the inventory, in blocks (a slot is 1). */
    private static final float GUI_FILL = 0.46F;
    private static final float GROUND_FILL = 0.30F;
    private static final float HAND_FILL = 0.72F;
    /** The grip sits at the model's bottom; this is how far into the slot that is allowed to reach. */
    private static final float HAND_BOTTOM = -0.35F;

    private static CrucibleItemRenderer instance;

    private CrucibleItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static CrucibleItemRenderer getInstance() {
        if (instance == null) {
            instance = new CrucibleItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        GltfInstance model = CrucibleModel.instance();
        GltfBounds bounds = CrucibleModel.bounds();
        if (model == null || !bounds.valid()) {
            return;
        }

        boolean flat = context == ItemDisplayContext.GUI
            || context == ItemDisplayContext.FIXED
            || context == ItemDisplayContext.GROUND;
        boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        pose.pushPose();
        // The renderer is called with the item cube's corner at the origin, so move to its centre first.
        pose.translate(0.5F, 0.5F, 0.5F);
        if (flat) {
            pose(pose, bounds, context == ItemDisplayContext.GUI ? GUI_FILL : GROUND_FILL);
        } else {
            hand(pose, bounds, leftHand);
        }

        int packedLight = flat ? LightTexture.FULL_BRIGHT : light;
        CrucibleModel.setBladePass(model, false);
        GltfRenderer.render(model, pose, buffers, packedLight, overlay);
        CrucibleModel.setBladePass(model, true);
        GltfRenderer.render(model, pose, buffers, LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }

    /** Shown on its side, three-quarter view, scaled so the whole sword stays inside the slot. */
    private static void pose(PoseStack pose, GltfBounds bounds, float fill) {
        pose.mulPose(Axis.YP.rotationDegrees(35.0F));
        pose.mulPose(Axis.XP.rotationDegrees(-20.0F));
        scaleToFit(pose, bounds, fill);
        pose.translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
    }

    /** Held like a sword: blade up and across the body, grip in the fist. */
    private static void hand(PoseStack pose, GltfBounds bounds, boolean leftHand) {
        float sign = leftHand ? 1.0F : -1.0F;
        pose.mulPose(Axis.YP.rotationDegrees(90.0F * sign));
        pose.mulPose(Axis.ZP.rotationDegrees(45.0F * sign));
        scaleToFit(pose, bounds, HAND_FILL);
        pose.translate(-bounds.centerX(), HAND_BOTTOM / scaleFor(bounds, HAND_FILL) - bounds.minY(), -bounds.centerZ());
    }

    private static void scaleToFit(PoseStack pose, GltfBounds bounds, float fill) {
        float scale = scaleFor(bounds, fill);
        pose.scale(scale, scale, scale);
    }

    /** {@code fill} is the half-extent of the bounding sphere, so the model ends up {@code 2*fill} across. */
    private static float scaleFor(GltfBounds bounds, float fill) {
        return bounds.radius() > 1.0E-4F ? fill / bounds.radius() : fill;
    }
}
