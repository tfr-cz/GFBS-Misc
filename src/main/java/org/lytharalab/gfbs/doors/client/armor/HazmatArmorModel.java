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
 * silhouette (layered hood, filter canisters, backpack tanks, chunky boots) - the same split HBM
 * uses for its power-armor OBJ groups. All coordinates are vanilla biped units (1 unit = 1/16
 * block, +Y down, +Z south), and every mesh declares the seven standard {@link HumanoidModel} part
 * names so the vanilla armor layer can pose it.</p>
 *
 * <p>Detail is expressed as stacked shells rather than one box per limb: a base shell carries the
 * silhouette, then rims/plaits sit slightly proud of it, then hardware (straps, buckles, rivets,
 * canisters, valves) rides on top. Small parts are 0.3-1.5 units, so they read as trim from a
 * distance and as machinery up close.</p>
 *
 * <p>Textures are 256x256 with eight flat 64x64 cells, one per material:</p>
 * <pre>
 *   primary  (0,0)    shadow  (64,0)   trim   (128,0)   dark   (192,0)
 *   accent   (0,64)   glass   (64,64)  metal  (128,64)  lamp   (192,64)
 * </pre>
 * <p>A box samples the whole cell from its origin, so the widest unwrap here (the torso,
 * 2*(10.8+6.0) x 14.2+6.0 = 34 x 20) still sits inside one 64x64 cell.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatArmorModel extends HumanoidModel<LivingEntity> {
    // -------------------------------------------------------------- materials
    private static final int PRIMARY_U = 0;
    private static final int PRIMARY_V = 0;
    private static final int SHADOW_U = 64;
    private static final int SHADOW_V = 0;
    private static final int TRIM_U = 128;
    private static final int TRIM_V = 0;
    private static final int DARK_U = 192;
    private static final int DARK_V = 0;
    private static final int ACCENT_U = 0;
    private static final int ACCENT_V = 64;
    private static final int GLASS_U = 64;
    private static final int GLASS_V = 64;
    private static final int METAL_U = 128;
    private static final int METAL_V = 64;
    private static final int LAMP_U = 192;
    private static final int LAMP_V = 64;

    private HazmatArmorModel(ModelPart root) {
        super(root);
    }

    public static HazmatArmorModel of(LayerDefinition definition) {
        return new HazmatArmorModel(definition.bakeRoot());
    }

    // ------------------------------------------------------------------ slots

    /** Layered hood, recessed visor with a frame, chin vent, side filter canisters and head lamp. */
    public static LayerDefinition helmet() {
        MeshDefinition mesh = baseMesh();
        PartDefinition head = mesh.getRoot().getChild("head");

        head.addOrReplaceChild("hood", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-5.2F, -10.6F, -5.2F, 10.4F, 1.9F, 10.4F)      // crown
            .addBox(-5.7F, -8.7F, -5.7F, 11.4F, 2.6F, 11.4F)       // upper shell
            .addBox(-5.7F, -6.1F, -5.7F, 1.1F, 6.5F, 11.4F)        // left wall
            .addBox(4.6F, -6.1F, -5.7F, 1.1F, 6.5F, 11.4F)         // right wall
            .addBox(-5.7F, -6.1F, 3.0F, 11.4F, 6.5F, 2.7F),        // back wall
            PartPose.ZERO);
        head.addOrReplaceChild("hoodTrim", boxes(SHADOW_U, SHADOW_V)
            .addBox(-5.9F, -7.4F, -5.9F, 11.8F, 0.8F, 11.8F)       // panel seam band
            .addBox(-5.5F, -0.2F, 2.9F, 11.0F, 1.1F, 2.9F)         // rear skirt
            .addBox(-5.4F, -9.6F, -5.4F, 10.8F, 0.7F, 10.8F),      // crown step
            PartPose.ZERO);
        head.addOrReplaceChild("seal", boxes(DARK_U, DARK_V)
            .addBox(-4.9F, 0.3F, -4.9F, 9.8F, 1.6F, 9.8F)          // neck seal
            .addBox(-4.4F, 0.0F, -4.4F, 8.8F, 0.7F, 8.8F),         // seal lip
            PartPose.ZERO);
        head.addOrReplaceChild("face", boxes(DARK_U, DARK_V)
            .addBox(-5.2F, -6.9F, -6.6F, 10.4F, 1.4F, 1.5F)        // brow brim
            .addBox(-5.2F, -5.5F, -6.5F, 0.9F, 5.4F, 1.0F)         // visor frame left
            .addBox(4.3F, -5.5F, -6.5F, 0.9F, 5.4F, 1.0F)          // visor frame right
            .addBox(-5.2F, -0.3F, -6.6F, 10.4F, 2.1F, 13.0F),      // chin guard
            PartPose.ZERO);
        head.addOrReplaceChild("visor", boxes(GLASS_U, GLASS_V)
            .addBox(-4.3F, -5.5F, -6.35F, 8.6F, 5.2F, 0.7F),       // smoked pane
            PartPose.ZERO);
        head.addOrReplaceChild("vents", boxes(ACCENT_U, ACCENT_V)
            .addBox(-3.7F, -0.1F, -6.75F, 1.7F, 1.3F, 0.3F)        // chin slits
            .addBox(-0.9F, -0.1F, -6.75F, 1.7F, 1.3F, 0.3F)
            .addBox(1.9F, -0.1F, -6.75F, 1.7F, 1.3F, 0.3F)
            .addBox(-6.5F, -1.2F, -2.4F, 0.8F, 1.2F, 4.8F)         // cheek intake grille
            .addBox(5.8F, -1.2F, -2.4F, 0.8F, 1.2F, 4.8F),
            PartPose.ZERO);
        head.addOrReplaceChild("canisters", boxes(TRIM_U, TRIM_V)
            .addBox(-7.3F, -3.9F, -1.9F, 1.5F, 3.6F, 3.8F)         // left cartridge
            .addBox(5.8F, -3.9F, -1.9F, 1.5F, 3.6F, 3.8F),         // right cartridge
            PartPose.ZERO);
        head.addOrReplaceChild("canisterCaps", boxes(METAL_U, METAL_V)
            .addBox(-7.4F, -4.3F, -2.0F, 1.7F, 0.7F, 4.0F)         // left cap
            .addBox(-7.4F, -0.2F, -2.0F, 1.7F, 0.7F, 4.0F)         // left base
            .addBox(5.7F, -4.3F, -2.0F, 1.7F, 0.7F, 4.0F)          // right cap
            .addBox(5.7F, -0.2F, -2.0F, 1.7F, 0.7F, 4.0F),         // right base
            PartPose.ZERO);
        head.addOrReplaceChild("lampHousing", boxes(DARK_U, DARK_V)
            .addBox(-2.3F, -9.8F, -6.7F, 4.6F, 2.9F, 1.4F),        // lamp bracket
            PartPose.ZERO);
        head.addOrReplaceChild("lamp", boxes(LAMP_U, LAMP_V)
            .addBox(-1.7F, -9.3F, -7.0F, 3.4F, 1.9F, 0.8F),        // lens
            PartPose.ZERO);
        head.addOrReplaceChild("knob", boxes(TRIM_U, TRIM_V)
            .addBox(-1.4F, -12.1F, -1.4F, 2.8F, 1.6F, 2.8F),       // top knob
            PartPose.ZERO);
        head.addOrReplaceChild("brimRivets", boxes(METAL_U, METAL_V)
            .addBox(-4.6F, -6.7F, -6.85F, 0.8F, 0.8F, 0.4F)        // brim rivets
            .addBox(3.8F, -6.7F, -6.85F, 0.8F, 0.8F, 0.4F)
            .addBox(-6.9F, -3.3F, -1.0F, 0.5F, 0.5F, 0.5F)         // cartridge bolts
            .addBox(6.4F, -3.3F, -1.0F, 0.5F, 0.5F, 0.5F),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** Torso shell, brass hood ring, chest harness, belt with buckle and the two back tanks. */
    public static LayerDefinition chest() {
        MeshDefinition mesh = baseMesh();
        PartDefinition body = mesh.getRoot().getChild("body");

        body.addOrReplaceChild("torso", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-5.4F, -0.8F, -3.0F, 10.8F, 14.2F, 6.0F),
            PartPose.ZERO);
        body.addOrReplaceChild("panelling", boxes(SHADOW_U, SHADOW_V)
            .addBox(-5.5F, 2.6F, -3.1F, 11.0F, 1.0F, 6.2F)         // waist seam
            .addBox(-5.5F, 8.8F, -3.1F, 11.0F, 0.9F, 6.2F)         // chest seam
            .addBox(-5.5F, -0.7F, -3.1F, 11.0F, 2.4F, 6.2F),       // shoulder yoke
            PartPose.ZERO);
        body.addOrReplaceChild("collar", boxes(TRIM_U, TRIM_V)
            .addBox(-6.5F, -1.4F, -6.5F, 13.0F, 3.2F, 13.0F)       // hood ring
            .addBox(-4.8F, 3.2F, -4.0F, 9.6F, 6.2F, 1.2F),         // chest plate
            PartPose.ZERO);
        body.addOrReplaceChild("harness", boxes(ACCENT_U, ACCENT_V)
            .addBox(-5.6F, 7.6F, -3.3F, 11.2F, 1.4F, 0.6F)         // chest strap
            .addBox(-0.6F, 0.6F, -3.3F, 1.2F, 10.8F, 0.5F)         // centre zip
            .addBox(-4.7F, 1.0F, -3.3F, 1.4F, 6.0F, 0.5F)          // shoulder strap left
            .addBox(3.3F, 1.0F, -3.3F, 1.4F, 6.0F, 0.5F)           // shoulder strap right
            .addBox(-3.2F, 10.4F, 5.6F, 1.0F, 1.6F, 1.0F)          // tank hose
            .addBox(-2.9F, 4.6F, 5.5F, 0.8F, 5.8F, 0.8F),
            PartPose.ZERO);
        body.addOrReplaceChild("badge", boxes(METAL_U, METAL_V)
            .addBox(-1.2F, 4.4F, -4.15F, 3.2F, 2.2F, 0.4F),        // chest badge
            PartPose.ZERO);
        body.addOrReplaceChild("belt", boxes(DARK_U, DARK_V)
            .addBox(-5.8F, 11.2F, -3.2F, 11.6F, 2.2F, 6.4F)        // belt
            .addBox(-3.3F, 1.0F, 3.0F, 6.6F, 10.4F, 0.9F)          // spine plate
            .addBox(-4.6F, 10.4F, 3.9F, 3.6F, 0.9F, 3.6F)          // tank collars
            .addBox(1.0F, 10.4F, 3.9F, 3.6F, 0.9F, 3.6F),
            PartPose.ZERO);
        body.addOrReplaceChild("tanks", boxes(TRIM_U, TRIM_V)
            .addBox(-4.6F, 1.6F, 3.95F, 3.6F, 9.0F, 3.6F)          // left tank
            .addBox(1.0F, 1.6F, 3.95F, 3.6F, 9.0F, 3.6F),          // right tank
            PartPose.ZERO);
        body.addOrReplaceChild("tankBands", boxes(METAL_U, METAL_V)
            .addBox(-4.7F, 3.2F, 3.85F, 3.8F, 0.6F, 3.8F)          // left bands
            .addBox(-4.7F, 7.8F, 3.85F, 3.8F, 0.6F, 3.8F)
            .addBox(0.9F, 3.2F, 3.85F, 3.8F, 0.6F, 3.8F)           // right bands
            .addBox(0.9F, 7.8F, 3.85F, 3.8F, 0.6F, 3.8F)
            .addBox(-3.5F, 11.3F, 5.2F, 1.6F, 1.1F, 1.6F)          // valves
            .addBox(1.9F, 11.3F, 5.2F, 1.6F, 1.1F, 1.6F)
            .addBox(-1.6F, 11.4F, -3.5F, 3.2F, 1.5F, 0.5F)         // buckle
            .addBox(-4.7F, 11.2F, -3.35F, 0.9F, 2.2F, 0.3F)        // belt loops
            .addBox(1.4F, 11.2F, -3.35F, 0.9F, 2.2F, 0.3F),
            PartPose.ZERO);

        arm(mesh, "right_arm", false);
        arm(mesh, "left_arm", true);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** One arm: padded sleeve, brass pad with rim, cuff ring, gloved hand with a knuckle plate. */
    private static void arm(MeshDefinition mesh, String part, boolean mirrored) {
        PartDefinition arm = mesh.getRoot().getChild(part);
        arm.addOrReplaceChild("sleeve", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(mx(-4.3F, 6.0F, mirrored), -3.2F, -3.2F, 6.0F, 10.6F, 6.4F),
            PartPose.ZERO);
        arm.addOrReplaceChild("cuff", boxes(TRIM_U, TRIM_V)
            .addBox(mx(-4.4F, 6.2F, mirrored), 5.4F, -3.3F, 6.2F, 1.5F, 6.6F),
            PartPose.ZERO);
        arm.addOrReplaceChild("pad", boxes(TRIM_U, TRIM_V)
            .addBox(mx(-5.3F, 6.0F, mirrored), -4.6F, -3.25F, 6.0F, 3.9F, 6.5F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("padRim", boxes(DARK_U, DARK_V)
            .addBox(mx(-5.4F, 6.2F, mirrored), -1.1F, -3.35F, 6.2F, 0.7F, 6.7F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("padRidge", boxes(SHADOW_U, SHADOW_V)
            .addBox(mx(-5.1F, 5.6F, mirrored), -4.9F, -3.05F, 5.6F, 0.7F, 6.1F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("glove", boxes(ACCENT_U, ACCENT_V)
            .addBox(mx(-4.3F, 6.0F, mirrored), 6.8F, -3.2F, 6.0F, 3.7F, 6.4F),
            PartPose.ZERO);
        arm.addOrReplaceChild("knuckles", boxes(METAL_U, METAL_V)
            .addBox(mx(-4.1F, 5.6F, mirrored), 8.2F, -3.4F, 5.6F, 1.4F, 0.5F),
            PartPose.ZERO);
    }

    /** Mirrors a box origin across the arm pivot: right-side x becomes -(x + width) on the left. */
    private static float mx(float x, float width, boolean mirrored) {
        return mirrored ? -(x + width) : x;
    }

    /** Yellow legs with thigh pocket, straps and knee pads; the hip band rides on the body part. */
    public static LayerDefinition legs() {
        MeshDefinition mesh = baseMesh();
        mesh.getRoot().getChild("body").addOrReplaceChild("hips", boxes(DARK_U, DARK_V)
            .addBox(-4.8F, 10.1F, -2.9F, 9.6F, 2.0F, 5.8F),
            PartPose.ZERO);
        mesh.getRoot().getChild("body").addOrReplaceChild("hipBuckle", boxes(METAL_U, METAL_V)
            .addBox(-1.3F, 10.3F, -3.1F, 2.6F, 1.5F, 0.5F),
            PartPose.ZERO);
        legBoxes(mesh, "right_leg");
        legBoxes(mesh, "left_leg");
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void legBoxes(MeshDefinition mesh, String part) {
        PartDefinition leg = mesh.getRoot().getChild(part);
        leg.addOrReplaceChild("thigh", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-2.4F, -1.5F, -3.0F, 4.8F, 8.2F, 6.0F),
            PartPose.ZERO);
        leg.addOrReplaceChild("thighSeam", boxes(SHADOW_U, SHADOW_V)
            .addBox(-2.35F, 3.6F, -3.05F, 4.7F, 0.8F, 6.1F)        // horizontal seam
            .addBox(-2.35F, 6.5F, -2.95F, 4.7F, 1.4F, 5.9F),       // lower cuff
            PartPose.ZERO);
        leg.addOrReplaceChild("pocket", boxes(DARK_U, DARK_V)
            .addBox(-1.8F, 1.8F, -3.35F, 3.6F, 3.2F, 0.6F),        // cargo pocket
            PartPose.ZERO);
        leg.addOrReplaceChild("pocketFlap", boxes(ACCENT_U, ACCENT_V)
            .addBox(-1.9F, 2.6F, -3.5F, 3.8F, 0.8F, 0.4F)          // pocket flap
            .addBox(-2.55F, 4.4F, -3.15F, 0.5F, 2.0F, 6.3F),       // outer side strap
            PartPose.ZERO);
        leg.addOrReplaceChild("knee", boxes(TRIM_U, TRIM_V)
            .addBox(-2.7F, 5.2F, -3.9F, 5.4F, 2.2F, 2.2F),
            PartPose.ZERO);
        leg.addOrReplaceChild("kneeBolts", boxes(METAL_U, METAL_V)
            .addBox(-2.1F, 5.7F, -4.05F, 0.7F, 0.7F, 0.3F)
            .addBox(1.4F, 5.7F, -4.05F, 0.7F, 0.7F, 0.3F),
            PartPose.ZERO);
    }

    /** Boots: shell, grey cuff, steel toe cap, ankle strap with buckle and a two-layer sole. */
    public static LayerDefinition boots() {
        MeshDefinition mesh = baseMesh();
        bootBoxes(mesh, "right_leg");
        bootBoxes(mesh, "left_leg");
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void bootBoxes(MeshDefinition mesh, String part) {
        PartDefinition leg = mesh.getRoot().getChild(part);
        leg.addOrReplaceChild("boot", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-2.7F, 7.4F, -3.2F, 5.4F, 3.7F, 6.4F),
            PartPose.ZERO);
        leg.addOrReplaceChild("cuff", boxes(TRIM_U, TRIM_V)
            .addBox(-2.8F, 6.8F, -3.3F, 5.6F, 1.0F, 6.6F)          // top cuff
            .addBox(-2.85F, 8.1F, -3.3F, 5.7F, 0.9F, 6.6F),        // ankle strap
            PartPose.ZERO);
        leg.addOrReplaceChild("toeCap", boxes(METAL_U, METAL_V)
            .addBox(-2.7F, 8.6F, -4.4F, 5.4F, 2.5F, 1.3F),         // steel toe
            PartPose.ZERO);
        leg.addOrReplaceChild("heel", boxes(DARK_U, DARK_V)
            .addBox(-2.5F, 9.4F, 2.7F, 5.0F, 1.7F, 1.2F),          // heel block
            PartPose.ZERO);
        leg.addOrReplaceChild("buckle", boxes(METAL_U, METAL_V)
            .addBox(-0.8F, 8.1F, -3.5F, 1.6F, 1.1F, 0.4F),
            PartPose.ZERO);
        leg.addOrReplaceChild("sole", boxes(DARK_U, DARK_V)
            .addBox(-2.9F, 11.1F, -4.6F, 5.8F, 1.0F, 8.0F),        // sole body
            PartPose.ZERO);
        leg.addOrReplaceChild("tread", boxes(SHADOW_U, SHADOW_V)
            .addBox(-2.9F, 12.1F, -4.6F, 5.8F, 0.4F, 1.4F)         // tread blocks
            .addBox(-2.9F, 12.1F, -1.6F, 5.8F, 0.4F, 2.0F)
            .addBox(-2.9F, 12.1F, 2.0F, 5.8F, 0.4F, 1.4F),
            PartPose.ZERO);
    }

    // ---------------------------------------------------------------- helpers

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
