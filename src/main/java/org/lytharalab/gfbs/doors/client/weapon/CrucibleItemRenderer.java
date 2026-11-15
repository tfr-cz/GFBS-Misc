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
    /** HBM's swing translation was authored for a much larger on-screen sword; keep a hint of the lunge. */
    private static final float SWING_LUNGE = 0.4F;

    /**
     * Held pose, tuned against NTM screenshots: the vanilla {@code handheld} transform without its -90 degree yaw
     * (that yaw turns a flat sprite edge-on, which is not what the Crucible does) and with the roll, height and size
     * adjustable at runtime through {@code /gfbs_crucible} so they can be dialled in without a rebuild.
     *
     * <p>{@code y} is in sixteenths of a block, like the JSON, and {@code size} is the sword's height in blocks.</p>
     */
    private static float thirdRoll = -40.0F;
    private static float thirdY = 1.6F;
    private static float thirdSize = 1.8F;
    private static float firstRoll = -5.0F;
    private static float firstY = 3.2F;
    private static float firstSize = 1.5F;
    private static ItemTransform thirdTransform;
    private static ItemTransform firstTransform;
    private static final ItemTransform NONE = new ItemTransform(
        new Vector3f(0.0F, 0.0F, 0.0F),
        new Vector3f(0.0F, 0.0F, 0.0F),
        new Vector3f(1.0F, 1.0F, 1.0F));

    private static CrucibleItemRenderer instance;

    /** Used by {@code /gfbs_crucible}: roll in degrees, y in sixteenths of a block, size in blocks. */
    public static void setPose(boolean firstPerson, float roll, float y, float size) {
        if (firstPerson) {
            firstRoll = roll;
            firstY = y;
            firstSize = size;
            firstTransform = null;
        } else {
            thirdRoll = roll;
            thirdY = y;
            thirdSize = size;
            thirdTransform = null;
        }
    }

    public static String describePose() {
        return String.format(java.util.Locale.ROOT,
            "third: roll %.1f y %.2f size %.2f | first: roll %.1f y %.2f size %.2f",
            thirdRoll, thirdY / 16.0F, thirdSize, firstRoll, firstY / 16.0F, firstSize);
    }

    private static ItemTransform thirdTransform() {
        if (thirdTransform == null) {
            thirdTransform = new ItemTransform(new Vector3f(0.0F, 0.0F, thirdRoll),
                new Vector3f(0.0F, thirdY / 16.0F, 0.5F / 16.0F), new Vector3f(0.85F, 0.85F, 0.85F));
        }
        return thirdTransform;
    }

    private static ItemTransform firstTransform() {
        if (firstTransform == null) {
            firstTransform = new ItemTransform(new Vector3f(0.0F, 0.0F, firstRoll),
                new Vector3f(1.13F / 16.0F, firstY / 16.0F, 1.13F / 16.0F),
                new Vector3f(0.68F, 0.68F, 0.68F));
        }
        return firstTransform;
    }

    private static float spriteHeight(ItemDisplayContext context) {
        return switch (context) {
            case GUI -> GUI_HEIGHT;
            case GROUND, FIXED -> GROUND_HEIGHT;
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> firstSize;
            default -> thirdSize;
        };
    }

    private static ItemTransform transformFor(ItemDisplayContext context) {
        if (context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            return firstTransform();
        }
        if (context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            return thirdTransform();
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

        // HBM only drew the blade when its GUARD_ROT bus was idle (`rot[2] == 0.0`, and that channel is 1 for the
        // whole equip clip): hilt and folded guards are raised first, the blade appears once the fold is done.
        // The flat views are exempt, like NTM's inventory pass, so the slot is never an empty handle.
        boolean bladeVisible = flat || !ItemAnimations.isPlaying(CrucibleAnimations.GUARD_ROT);
        if (bladeVisible) {
            CrucibleModel.setBladePass(model, true);
            GltfRenderer.render(model, pose, buffers, LightTexture.FULL_BRIGHT, overlay);
        }
        pose.popPose();
    }
}
