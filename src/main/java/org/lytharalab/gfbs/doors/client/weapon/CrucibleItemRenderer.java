package org.lytharalab.gfbs.doors.client.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.lytharalab.gfbs.doors.client.anim.BusAnimationSequence;
import org.lytharalab.gfbs.doors.client.anim.CrucibleAnimations;
import org.lytharalab.gfbs.doors.client.anim.ItemAnimations;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.lytharalab.gfbs.gltf.api.model.GltfBounds;

/**
 * Draws HBM's Crucible mesh as an item - grid, hand, ground and item frame.
 *
 * <p>The item model is {@code builtin/entity}, so vanilla hands the draw over to this renderer instead of drawing a
 * baked quad, and the JSON {@code display} block is not applied on that path. So instead of inventing a pose, the
 * renderer treats the mesh as if it were a 16x16 sprite: it is laid into the item's unit cube (bottom at y=0,
 * centred in x/z, flat towards +Z) and then handed the same display transforms a vanilla sword gets -
 * {@code thirdperson} [0,-90,55], {@code firstperson} [0,-90,25], both straight out of
 * {@code models/item/handheld.json}. Doing it that way is what makes the blade face the viewer instead of the edge:
 * the mesh's flat normal is its x axis, so the sprite-style quarter turn about y has to come before the pose.</p>
 *
 * <p>The blade is drawn a second time at full brightness with the rest of the mesh hidden, because 1.7.10 had
 * {@code glDisable(GL_LIGHTING)} per part and 1.20.1 has one light level per draw.</p>
 *
 * <p>The swing and equip clips are applied last, in the mesh's own units.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CrucibleItemRenderer extends BlockEntityWithoutLevelRenderer {
    /**
     * How tall the sword is drawn, in blocks - 1.0 would be exactly a full 16x16 sprite.
     *
     * <p>HBM's Crucible is not sword-sized: in NTM it takes up most of the right half of the first-person view and
     * reaches well above the player's head in third person, which is roughly 1.8 blocks of blade. The inventory and
     * the ground keep it small enough to fit where it is drawn.</p>
     */
    private static final float GUI_HEIGHT = 0.95F;
    private static final float GROUND_HEIGHT = 0.55F;
    private static final float FIRST_PERSON_HEIGHT = 1.5F;
    private static final float THIRD_PERSON_HEIGHT = 1.8F;
    /** HBM's swing translation was authored for a much larger on-screen sword; keep a hint of the lunge. */
    private static final float SWING_LUNGE = 0.4F;

    /**
     * Vanilla's {@code handheld} transform with the translation divided by 16 like the JSON loader does - minus its
     * -90 degree yaw. That yaw exists because a vanilla sword is a flat quad whose face already points at the viewer;
     * turning it is what makes a vanilla sword show its edge from the front. HBM wanted the opposite (the Crucible's
     * blade faces whoever is looking at it), so the roll alone is kept.
     */
    private static final ItemTransform THIRD_PERSON = new ItemTransform(
        new Vector3f(0.0F, 0.0F, 55.0F),
        new Vector3f(0.0F, 4.0F / 16.0F, 0.5F / 16.0F),
        new Vector3f(0.85F, 0.85F, 0.85F));
    private static final ItemTransform FIRST_PERSON = new ItemTransform(
        new Vector3f(0.0F, 0.0F, 25.0F),
        new Vector3f(1.13F / 16.0F, 3.2F / 16.0F, 1.13F / 16.0F),
        new Vector3f(0.68F, 0.68F, 0.68F));
    private static final ItemTransform NONE = new ItemTransform(
        new Vector3f(0.0F, 0.0F, 0.0F),
        new Vector3f(0.0F, 0.0F, 0.0F),
        new Vector3f(1.0F, 1.0F, 1.0F));

    private static CrucibleItemRenderer instance;

    private static float spriteHeight(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> GUI_HEIGHT;
            case GROUND, FIXED -> GROUND_HEIGHT;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> FIRST_PERSON_HEIGHT;
            default -> THIRD_PERSON_HEIGHT;
        };
    }

    private static ItemTransform transformFor(ItemDisplayContext context) {
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return FIRST_PERSON;
        }
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return THIRD_PERSON;
        }
        return NONE;
    }

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

        boolean firstPerson = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        boolean leftHand = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        boolean flat = context == ItemDisplayContext.GUI
            || context == ItemDisplayContext.FIXED
            || context == ItemDisplayContext.GROUND;

        pose.pushPose();
        // The renderer is called with the item cube already shifted by -0.5, i.e. the sprite sits at -0.5..0.5.
        // The display transform is normally applied one step earlier, around the sprite's 0..1 corner, so shift
        // back there first.
        pose.translate(0.5F, 0.5F, 0.5F);
        transformFor(context).apply(leftHand, pose);
        // Vanilla shifts the sprite by -0.5 *after* the display transform; our entry pose already carried that
        // shift, so redo it here to keep the rotations pivoting where they do for a real item.
        pose.translate(-0.5F, -0.5F, -0.5F);

        // Now draw the mesh as the sprite would be drawn: bottom edge on y=0, centred in x and z.
        float height = Math.max(0.001F, bounds.maxY() - bounds.minY());
        float scale = spriteHeight(context) / height;
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.scale(scale, scale, scale);
        pose.translate(-bounds.centerX(), -bounds.minY(), -bounds.centerZ());
        // The mesh is thin along x, so a quarter turn makes that thin axis the sprite's normal (+z).
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));

        // NTM's animation buses, in model units: translate first, then X/Y/Z rotations.
        ItemAnimations.applyTranslation(pose, CrucibleAnimations.SWING_TRANS, SWING_LUNGE);
        ItemAnimations.applyRotation(pose, CrucibleAnimations.SWING_ROT);
        CrucibleModel.applyGuardAngle(model, ItemAnimations.channel(CrucibleAnimations.GUARD_ROT,
            BusAnimationSequence.Dimension.RX));

        int packedLight = flat ? LightTexture.FULL_BRIGHT : light;
        CrucibleModel.setBladePass(model, false);
        GltfRenderer.render(model, pose, buffers, packedLight, overlay);
        CrucibleModel.setBladePass(model, true);
        GltfRenderer.render(model, pose, buffers, LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
    }
}
