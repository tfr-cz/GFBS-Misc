package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The four hand-written meshes of the protective suit.
 *
 * <p>Each armor slot gets its own mesh instead of sharing one biped, so the pieces can differ in
 * silhouette (boxy hood, backpack tank, chunky boots) - the same split HBM uses for its power-armor
 * OBJ groups. All coordinates are vanilla biped units (1 unit = 1/16 block, +Y down, +Z south), and
 * every mesh declares the seven standard {@link HumanoidModel} part names so the vanilla armor layer
 * can pose it.</p>
 *
 * <p>Textures are 128x128 with four flat 64x64 quadrants: primary (0,0), trim (64,0), dark (0,64)
 * and accent (64,64). Boxes sample a whole quadrant, which keeps the coarse solid-colour look
 * without per-face UV work.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatArmorModel extends HumanoidModel<LivingEntity> {
    private static final int PRIMARY_U = 0;
    private static final int PRIMARY_V = 0;
    private static final int TRIM_U = 64;
    private static final int TRIM_V = 0;
    private static final int DARK_U = 0;
    private static final int DARK_V = 64;
    private static final int ACCENT_U = 64;
    private static final int ACCENT_V = 64;

    private HazmatArmorModel(ModelPart root) {
        super(root);
    }

    public static HazmatArmorModel of(LayerDefinition definition) {
        return new HazmatArmorModel(definition.bakeRoot());
    }

    // ------------------------------------------------------------------ slots

    /** Boxy hood with an open face, chin guard, side vents, top knob and a front lamp. */
    public static LayerDefinition helmet() {
        MeshDefinition mesh = baseMesh();
        PartDefinition head = mesh.getRoot().getChild("head");
        head.addOrReplaceChild("hood", boxes(TRIM_U, TRIM_V)
            .addBox(-5.0F, -9.0F, -5.0F, 10.0F, 3.0F, 10.0F)      // top shell
            .addBox(-5.0F, -6.0F, -5.0F, 1.0F, 6.0F, 10.0F)       // left wall
            .addBox(4.0F, -6.0F, -5.0F, 1.0F, 6.0F, 10.0F)        // right wall
            .addBox(-5.0F, -6.0F, 2.5F, 10.0F, 6.0F, 2.5F)        // back wall
            .addBox(-1.2F, -10.8F, -1.2F, 2.4F, 2.0F, 2.4F),      // top knob
            PartPose.ZERO);
        head.addOrReplaceChild("face", boxes(DARK_U, DARK_V)
            .addBox(-4.6F, -7.4F, -5.7F, 9.2F, 1.4F, 1.4F)        // visor top bar
            .addBox(-4.6F, -1.8F, -5.7F, 9.2F, 1.8F, 11.4F)       // chin guard
            .addBox(-5.6F, -5.6F, -1.6F, 0.6F, 1.0F, 1.0F)        // vents
            .addBox(-5.6F, -5.6F, 1.2F, 0.6F, 1.0F, 1.0F)
            .addBox(5.0F, -5.6F, -1.6F, 0.6F, 1.0F, 1.0F)
            .addBox(5.0F, -5.6F, 1.2F, 0.6F, 1.0F, 1.0F),
            PartPose.ZERO);
        head.addOrReplaceChild("lamp", boxes(ACCENT_U, ACCENT_V)
            .addBox(-1.6F, -8.4F, -6.2F, 3.2F, 1.8F, 1.0F),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Torso shell, brass hood ring, chest plate, belt and the two back tanks. */
    public static LayerDefinition chest() {
        MeshDefinition mesh = baseMesh();
        PartDefinition body = mesh.getRoot().getChild("body");
        body.addOrReplaceChild("torso", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-4.5F, -0.5F, -2.5F, 9.0F, 12.5F, 5.0F),
            PartPose.ZERO);
        body.addOrReplaceChild("trim", boxes(TRIM_U, TRIM_V)
            .addBox(-5.5F, -0.3F, -5.5F, 11.0F, 2.6F, 11.0F)      // hood ring
            .addBox(-4.0F, 0.6F, -3.3F, 8.0F, 6.5F, 0.9F)         // chest plate
            .addBox(-3.2F, 2.0F, 3.2F, 2.6F, 7.0F, 2.6F)          // tanks
            .addBox(0.6F, 2.0F, 3.2F, 2.6F, 7.0F, 2.6F),
            PartPose.ZERO);
        body.addOrReplaceChild("hardware", boxes(DARK_U, DARK_V)
            .addBox(-4.7F, 10.6F, -2.7F, 9.4F, 1.6F, 5.4F)        // belt
            .addBox(-4.0F, 1.0F, 2.4F, 8.0F, 9.0F, 1.0F)          // back plate
            .addBox(-3.2F, 8.8F, 3.2F, 2.6F, 1.0F, 2.6F)          // tank caps
            .addBox(0.6F, 8.8F, 3.2F, 2.6F, 1.0F, 2.6F),
            PartPose.ZERO);

        mesh.getRoot().getChild("right_arm").addOrReplaceChild("sleeve", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-3.4F, -1.6F, -2.4F, 4.4F, 9.0F, 4.8F),
            PartPose.ZERO);
        mesh.getRoot().getChild("right_arm").addOrReplaceChild("pad", boxes(TRIM_U, TRIM_V)
            .addBox(-4.6F, -3.0F, -2.8F, 5.2F, 3.4F, 5.6F),
            PartPose.ZERO);
        mesh.getRoot().getChild("right_arm").addOrReplaceChild("glove", boxes(ACCENT_U, ACCENT_V)
            .addBox(-3.4F, 6.6F, -2.4F, 4.4F, 3.6F, 4.8F),
            PartPose.ZERO);

        mesh.getRoot().getChild("left_arm").addOrReplaceChild("sleeve", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-1.0F, -1.6F, -2.4F, 4.4F, 9.0F, 4.8F),
            PartPose.ZERO);
        mesh.getRoot().getChild("left_arm").addOrReplaceChild("pad", boxes(TRIM_U, TRIM_V)
            .addBox(-0.6F, -3.0F, -2.8F, 5.2F, 3.4F, 5.6F),
            PartPose.ZERO);
        mesh.getRoot().getChild("left_arm").addOrReplaceChild("glove", boxes(ACCENT_U, ACCENT_V)
            .addBox(-1.0F, 6.6F, -2.4F, 4.4F, 3.6F, 4.8F),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** Yellow legs (thigh + knee pad + shin stub) plus the hip band that sits on the body part. */
    public static LayerDefinition legs() {
        MeshDefinition mesh = baseMesh();
        mesh.getRoot().getChild("body").addOrReplaceChild("hips", boxes(DARK_U, DARK_V)
            .addBox(-4.6F, 10.2F, -2.6F, 9.2F, 1.8F, 5.2F),
            PartPose.ZERO);
        legBoxes(mesh, "right_leg");
        legBoxes(mesh, "left_leg");
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void legBoxes(MeshDefinition mesh, String part) {
        PartDefinition leg = mesh.getRoot().getChild(part);
        leg.addOrReplaceChild("thigh", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-2.6F, 0.0F, -2.6F, 5.2F, 7.0F, 5.2F)
            .addBox(-2.3F, 7.4F, -2.3F, 4.6F, 1.2F, 4.6F),
            PartPose.ZERO);
        leg.addOrReplaceChild("knee", boxes(TRIM_U, TRIM_V)
            .addBox(-2.8F, 6.2F, -3.1F, 5.6F, 1.4F, 1.6F),
            PartPose.ZERO);
    }

    /** Black boots with a cuff and a chunky sole that sticks out in front. */
    public static LayerDefinition boots() {
        MeshDefinition mesh = baseMesh();
        bootBoxes(mesh, "right_leg");
        bootBoxes(mesh, "left_leg");
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void bootBoxes(MeshDefinition mesh, String part) {
        PartDefinition leg = mesh.getRoot().getChild(part);
        leg.addOrReplaceChild("boot", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-2.7F, 8.4F, -2.7F, 5.4F, 3.0F, 5.4F),
            PartPose.ZERO);
        leg.addOrReplaceChild("sole", boxes(TRIM_U, TRIM_V)
            .addBox(-2.8F, 8.1F, -2.8F, 5.6F, 0.7F, 5.6F)         // cuff
            .addBox(-2.9F, 11.2F, -3.9F, 5.8F, 1.3F, 6.9F),       // sole
            PartPose.ZERO);
    }

    // ------------------------------------------------------------------ helpers

    private static CubeListBuilder boxes(int u, int v) {
        return CubeListBuilder.create().texOffs(u, v);
    }

    /** Empty biped skeleton with vanilla pivots; each slot only fills the parts it needs. */
    private static MeshDefinition baseMesh() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        return mesh;
    }
}
