package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A legal {@link HumanoidModel} with all seven biped parts present but no geometry.
 *
 * <p>The vanilla armour layer demands a humanoid model and force-copies pose and part visibility into
 * whatever it is given, so "render nothing here" has to be expressed as an empty model rather than
 * {@code null} (which would mean "use the vanilla one").</p>
 */
@OnlyIn(Dist.CLIENT)
public final class EmptyArmorModel {
    private static HumanoidModel<LivingEntity> instance;

    private EmptyArmorModel() {
    }

    public static synchronized HumanoidModel<LivingEntity> get() {
        if (instance == null) {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 0.0F, 0.0F));
            root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
            root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
            root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
            root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
            instance = new HumanoidModel<>(LayerDefinition.create(mesh, 64, 32).bakeRoot());
        }
        return instance;
    }
}
