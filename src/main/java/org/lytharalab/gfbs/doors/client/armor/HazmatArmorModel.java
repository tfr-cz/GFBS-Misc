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
 * <p>Textures are 256x256 with eight 64x64 cells, one per material. The cells are painted worn
 * patches (grime, stains, scuffs, rust) rather than flat colours:</p>
 * <pre>
 *   primary  (0,0)    shadow  (64,0)   trim   (128,0)   dark   (192,0)
 *   accent   (0,64)   glass   (64,64)  metal  (128,64)  lamp   (192,64)
 * </pre>
 * <p>A box samples the cell from its origin and each of its six faces lands on a different part of
 * the unwrap rect, so painted noise gives every face its own dirt without any per-face UV work. What
 * must not go into a cell is positional art - seams, logos, edge wear - because it would land on
 * faces at unpredictable offsets. The widest unwrap here (the collar, 2*(11.96+11.96) = 47.8 x 19.1)
 * still sits inside one 64x64 cell.</p>
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
            .addBox(-4.78F, -10.18F, -4.78F, 9.57F, 1.82F, 9.57F)      // crown
            .addBox(-5.24F, -8.35F, -5.24F, 10.49F, 2.5F, 10.49F)       // upper shell
            .addBox(-5.24F, -5.86F, -5.24F, 1.01F, 6.24F, 10.49F)        // left wall
            .addBox(4.23F, -5.86F, -5.24F, 1.01F, 6.24F, 10.49F)         // right wall
            .addBox(-5.24F, -5.86F, 2.76F, 10.49F, 6.24F, 2.48F),        // back wall
            PartPose.ZERO);
        head.addOrReplaceChild("hoodTrim", boxes(SHADOW_U, SHADOW_V)
            .addBox(-5.43F, -7.1F, -5.43F, 10.86F, 0.77F, 10.86F)       // panel seam band
            .addBox(-5.06F, -0.19F, 2.67F, 10.12F, 1.06F, 2.67F)         // rear skirt
            .addBox(-4.97F, -9.22F, -4.97F, 9.94F, 0.67F, 9.94F),      // crown step
            PartPose.ZERO);
        head.addOrReplaceChild("seal", boxes(DARK_U, DARK_V)
            .addBox(-4.51F, 0.29F, -4.51F, 9.02F, 1.54F, 9.02F)          // neck seal
            .addBox(-4.05F, 0.0F, -4.05F, 8.1F, 0.67F, 8.1F),         // seal lip
            PartPose.ZERO);
        head.addOrReplaceChild("face", boxes(DARK_U, DARK_V)
            .addBox(-4.78F, -6.62F, -6.07F, 9.57F, 1.34F, 1.38F)        // brow brim
            .addBox(-4.78F, -5.28F, -5.98F, 0.83F, 5.18F, 0.92F)         // visor frame left
            .addBox(3.96F, -5.28F, -5.98F, 0.83F, 5.18F, 0.92F)          // visor frame right
            .addBox(-4.78F, -0.29F, -6.07F, 9.57F, 2.02F, 11.96F),      // chin guard
            PartPose.ZERO);
        head.addOrReplaceChild("visor", boxes(GLASS_U, GLASS_V)
            .addBox(-3.96F, -5.28F, -5.84F, 7.91F, 4.99F, 0.64F),       // smoked pane
            PartPose.ZERO);
        head.addOrReplaceChild("vents", boxes(ACCENT_U, ACCENT_V)
            .addBox(-3.4F, -0.1F, -6.21F, 1.56F, 1.25F, 0.28F)        // chin slits
            .addBox(-0.83F, -0.1F, -6.21F, 1.56F, 1.25F, 0.28F)
            .addBox(1.75F, -0.1F, -6.21F, 1.56F, 1.25F, 0.28F)
            .addBox(-5.98F, -1.15F, -2.21F, 0.74F, 1.15F, 4.42F)         // cheek intake grille
            .addBox(5.34F, -1.15F, -2.21F, 0.74F, 1.15F, 4.42F),
            PartPose.ZERO);
        head.addOrReplaceChild("canisters", boxes(TRIM_U, TRIM_V)
            .addBox(-6.72F, -3.74F, -1.75F, 1.38F, 3.46F, 3.5F)         // left cartridge
            .addBox(5.34F, -3.74F, -1.75F, 1.38F, 3.46F, 3.5F),         // right cartridge
            PartPose.ZERO);
        head.addOrReplaceChild("canisterCaps", boxes(METAL_U, METAL_V)
            .addBox(-6.81F, -4.13F, -1.84F, 1.56F, 0.67F, 3.68F)         // left cap
            .addBox(-6.81F, -0.19F, -1.84F, 1.56F, 0.67F, 3.68F)         // left base
            .addBox(5.24F, -4.13F, -1.84F, 1.56F, 0.67F, 3.68F)          // right cap
            .addBox(5.24F, -0.19F, -1.84F, 1.56F, 0.67F, 3.68F),         // right base
            PartPose.ZERO);
        head.addOrReplaceChild("lampHousing", boxes(DARK_U, DARK_V)
            .addBox(-2.12F, -9.41F, -6.16F, 4.23F, 2.78F, 1.29F),        // lamp bracket
            PartPose.ZERO);
        head.addOrReplaceChild("lamp", boxes(LAMP_U, LAMP_V)
            .addBox(-1.56F, -8.93F, -6.44F, 3.13F, 1.82F, 0.74F),        // lens
            PartPose.ZERO);
        head.addOrReplaceChild("knob", boxes(TRIM_U, TRIM_V)
            .addBox(-1.29F, -11.62F, -1.29F, 2.58F, 1.54F, 2.58F),       // top knob
            PartPose.ZERO);
        head.addOrReplaceChild("brimRivets", boxes(METAL_U, METAL_V)
            .addBox(-4.23F, -6.43F, -6.3F, 0.74F, 0.77F, 0.37F)        // brim rivets
            .addBox(3.5F, -6.43F, -6.3F, 0.74F, 0.77F, 0.37F)
            .addBox(-6.35F, -3.17F, -0.92F, 0.46F, 0.48F, 0.46F)         // cartridge bolts
            .addBox(5.89F, -3.17F, -0.92F, 0.46F, 0.48F, 0.46F),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** Torso shell, brass hood ring, chest harness, belt with buckle and the two back tanks. */
    public static LayerDefinition chest() {
        MeshDefinition mesh = baseMesh();
        PartDefinition body = mesh.getRoot().getChild("body");

        body.addOrReplaceChild("torso", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-4.97F, -0.77F, -2.76F, 9.94F, 13.63F, 5.52F),
            PartPose.ZERO);
        body.addOrReplaceChild("panelling", boxes(SHADOW_U, SHADOW_V)
            .addBox(-5.06F, 2.5F, -2.85F, 10.12F, 0.96F, 5.7F)         // waist seam
            .addBox(-5.06F, 8.45F, -2.85F, 10.12F, 0.86F, 5.7F)         // chest seam
            .addBox(-5.06F, -0.67F, -2.85F, 10.12F, 2.3F, 5.7F),       // shoulder yoke
            PartPose.ZERO);
        body.addOrReplaceChild("collar", boxes(TRIM_U, TRIM_V)
            .addBox(-5.98F, -1.34F, -5.98F, 11.96F, 3.07F, 11.96F)       // hood ring
            .addBox(-4.42F, 3.07F, -3.68F, 8.83F, 5.95F, 1.1F),         // chest plate
            PartPose.ZERO);
        body.addOrReplaceChild("harness", boxes(ACCENT_U, ACCENT_V)
            .addBox(-5.15F, 7.3F, -3.04F, 10.3F, 1.34F, 0.55F)         // chest strap
            .addBox(-0.55F, 0.58F, -3.04F, 1.1F, 10.37F, 0.46F)         // centre zip
            .addBox(-4.32F, 0.96F, -3.04F, 1.29F, 5.76F, 0.46F)          // shoulder strap left
            .addBox(3.04F, 0.96F, -3.04F, 1.29F, 5.76F, 0.46F)           // shoulder strap right
            .addBox(-2.94F, 9.98F, 5.15F, 0.92F, 1.54F, 0.92F)          // tank hose
            .addBox(-2.67F, 4.42F, 5.06F, 0.74F, 5.57F, 0.74F),
            PartPose.ZERO);
        body.addOrReplaceChild("badge", boxes(METAL_U, METAL_V)
            .addBox(-1.1F, 4.22F, -3.82F, 2.94F, 2.11F, 0.37F),        // chest badge
            PartPose.ZERO);
        body.addOrReplaceChild("belt", boxes(DARK_U, DARK_V)
            .addBox(-5.34F, 10.75F, -2.94F, 10.67F, 2.11F, 5.89F)        // belt
            .addBox(-3.04F, 0.96F, 2.76F, 6.07F, 9.98F, 0.83F)          // spine plate
            .addBox(-4.23F, 9.98F, 3.59F, 3.31F, 0.86F, 3.31F)          // tank collars
            .addBox(0.92F, 9.98F, 3.59F, 3.31F, 0.86F, 3.31F),
            PartPose.ZERO);
        body.addOrReplaceChild("tanks", boxes(TRIM_U, TRIM_V)
            .addBox(-4.23F, 1.54F, 3.63F, 3.31F, 8.64F, 3.31F)          // left tank
            .addBox(0.92F, 1.54F, 3.63F, 3.31F, 8.64F, 3.31F),          // right tank
            PartPose.ZERO);
        body.addOrReplaceChild("tankBands", boxes(METAL_U, METAL_V)
            .addBox(-4.32F, 3.07F, 3.54F, 3.5F, 0.58F, 3.5F)          // left bands
            .addBox(-4.32F, 7.49F, 3.54F, 3.5F, 0.58F, 3.5F)
            .addBox(0.83F, 3.07F, 3.54F, 3.5F, 0.58F, 3.5F)           // right bands
            .addBox(0.83F, 7.49F, 3.54F, 3.5F, 0.58F, 3.5F)
            .addBox(-3.22F, 10.85F, 4.78F, 1.47F, 1.06F, 1.47F)          // valves
            .addBox(1.75F, 10.85F, 4.78F, 1.47F, 1.06F, 1.47F)
            .addBox(-1.47F, 10.94F, -3.22F, 2.94F, 1.44F, 0.46F)         // buckle
            .addBox(-4.32F, 10.75F, -3.08F, 0.83F, 2.11F, 0.28F)        // belt loops
            .addBox(1.29F, 10.75F, -3.08F, 0.83F, 2.11F, 0.28F),
            PartPose.ZERO);

        arm(mesh, "right_arm", false);
        arm(mesh, "left_arm", true);
        return LayerDefinition.create(mesh, 256, 256);
    }

    /** One arm: padded sleeve, brass pad with rim, cuff ring, gloved hand with a knuckle plate. */
    private static void arm(MeshDefinition mesh, String part, boolean mirrored) {
        PartDefinition arm = mesh.getRoot().getChild(part);
        arm.addOrReplaceChild("sleeve", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(mx(-3.87F, 5.4F, mirrored), -3.07F, -2.94F, 5.4F, 10.18F, 5.89F),
            PartPose.ZERO);
        arm.addOrReplaceChild("cuff", boxes(TRIM_U, TRIM_V)
            .addBox(mx(-3.96F, 5.58F, mirrored), 5.18F, -3.04F, 5.58F, 1.44F, 6.07F),
            PartPose.ZERO);
        arm.addOrReplaceChild("pad", boxes(TRIM_U, TRIM_V)
            .addBox(mx(-4.77F, 5.4F, mirrored), -4.42F, -2.99F, 5.4F, 3.74F, 5.98F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("padRim", boxes(DARK_U, DARK_V)
            .addBox(mx(-4.86F, 5.58F, mirrored), -1.06F, -3.08F, 5.58F, 0.67F, 6.16F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("padRidge", boxes(SHADOW_U, SHADOW_V)
            .addBox(mx(-4.59F, 5.04F, mirrored), -4.7F, -2.81F, 5.04F, 0.67F, 5.61F),
            PartPose.rotation(0.0F, 0.0F, mirrored ? -0.10F : 0.10F));
        arm.addOrReplaceChild("glove", boxes(ACCENT_U, ACCENT_V)
            .addBox(mx(-3.87F, 5.4F, mirrored), 6.53F, -2.94F, 5.4F, 3.55F, 5.89F),
            PartPose.ZERO);
        arm.addOrReplaceChild("knuckles", boxes(METAL_U, METAL_V)
            .addBox(mx(-3.69F, 5.04F, mirrored), 7.87F, -3.13F, 5.04F, 1.34F, 0.46F),
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
            .addBox(-4.51F, 9.7F, -2.73F, 9.02F, 1.92F, 5.45F),
            PartPose.ZERO);
        mesh.getRoot().getChild("body").addOrReplaceChild("hipBuckle", boxes(METAL_U, METAL_V)
            .addBox(-1.22F, 9.89F, -2.91F, 2.44F, 1.44F, 0.47F),
            PartPose.ZERO);
        legBoxes(mesh, "right_leg");
        legBoxes(mesh, "left_leg");
        return LayerDefinition.create(mesh, 256, 256);
    }

    private static void legBoxes(MeshDefinition mesh, String part) {
        PartDefinition leg = mesh.getRoot().getChild(part);
        leg.addOrReplaceChild("thigh", boxes(PRIMARY_U, PRIMARY_V)
            .addBox(-2.26F, -1.44F, -2.82F, 4.51F, 7.87F, 5.64F),
            PartPose.ZERO);
        leg.addOrReplaceChild("thighSeam", boxes(SHADOW_U, SHADOW_V)
            .addBox(-2.21F, 3.46F, -2.87F, 4.42F, 0.77F, 5.73F)        // horizontal seam
            .addBox(-2.21F, 6.24F, -2.77F, 4.42F, 1.34F, 5.55F),       // lower cuff
            PartPose.ZERO);
        leg.addOrReplaceChild("pocket", boxes(DARK_U, DARK_V)
            .addBox(-1.69F, 1.73F, -3.15F, 3.38F, 3.07F, 0.56F),        // cargo pocket
            PartPose.ZERO);
        leg.addOrReplaceChild("pocketFlap", boxes(ACCENT_U, ACCENT_V)
            .addBox(-1.79F, 2.5F, -3.29F, 3.57F, 0.77F, 0.38F)          // pocket flap
            .addBox(-2.4F, 4.22F, -2.96F, 0.47F, 1.92F, 5.92F),       // outer side strap
            PartPose.ZERO);
        leg.addOrReplaceChild("knee", boxes(TRIM_U, TRIM_V)
            .addBox(-2.54F, 4.99F, -3.67F, 5.08F, 2.11F, 2.07F),
            PartPose.ZERO);
        leg.addOrReplaceChild("kneeBolts", boxes(METAL_U, METAL_V)
            .addBox(-1.97F, 5.47F, -3.81F, 0.66F, 0.67F, 0.28F)
            .addBox(1.32F, 5.47F, -3.81F, 0.66F, 0.67F, 0.28F),
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
            .addBox(-2.54F, 7.4F, -3.01F, 5.08F, 3.7F, 6.02F),
            PartPose.ZERO);
        leg.addOrReplaceChild("cuff", boxes(TRIM_U, TRIM_V)
            .addBox(-2.63F, 6.8F, -3.1F, 5.26F, 1.0F, 6.2F)          // top cuff
            .addBox(-2.68F, 8.1F, -3.1F, 5.36F, 0.9F, 6.2F),        // ankle strap
            PartPose.ZERO);
        leg.addOrReplaceChild("toeCap", boxes(METAL_U, METAL_V)
            .addBox(-2.54F, 8.6F, -4.14F, 5.08F, 2.5F, 1.22F),         // steel toe
            PartPose.ZERO);
        leg.addOrReplaceChild("heel", boxes(DARK_U, DARK_V)
            .addBox(-2.35F, 9.4F, 2.54F, 4.7F, 1.7F, 1.13F),          // heel block
            PartPose.ZERO);
        leg.addOrReplaceChild("buckle", boxes(METAL_U, METAL_V)
            .addBox(-0.75F, 8.1F, -3.29F, 1.5F, 1.1F, 0.38F),
            PartPose.ZERO);
        leg.addOrReplaceChild("sole", boxes(DARK_U, DARK_V)
            .addBox(-2.73F, 11.1F, -4.32F, 5.45F, 1.0F, 7.52F),        // sole body
            PartPose.ZERO);
        leg.addOrReplaceChild("tread", boxes(SHADOW_U, SHADOW_V)
            .addBox(-2.73F, 12.1F, -4.32F, 5.45F, 0.4F, 1.32F)         // tread blocks
            .addBox(-2.73F, 12.1F, -1.5F, 5.45F, 0.4F, 1.88F)
            .addBox(-2.73F, 12.1F, 1.88F, 5.45F, 0.4F, 1.32F),
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
