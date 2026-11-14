package org.lytharalab.gfbs.doors.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws HBM's T-51b power armour as an imported mesh instead of boxes.
 *
 * <p>Each slot's OBJ keeps HBM's object names, and every object is authored in vanilla biped pixels
 * (the head sits at y -8.65, the soles at y 24.24), so the renderer only has to reproduce what
 * {@code ModelPart} would do: rotate that object around its biped pivot. For geometry expressed in
 * <em>absolute</em> model coordinates that transform is {@code T(pivot) * R * T(-pivot)}, written into
 * the node's local matrix, and the whole piece is then drawn at 1/16 scale.</p>
 *
 * <p>Arms and legs follow the player model's own parts, so the suit animates with the player without a
 * single animation clip: the mesh is rigid, the joint is where the vanilla part is.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class PowerArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final float MODEL_SCALE = 1.0F / 16.0F;

    public PowerArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        List<PowerArmorModels.Suit> suits = new ArrayList<>();

        for (EquipmentSlot slot : PowerArmorModels.SLOTS) {
            if (!wears(player, slot)) {
                continue;
            }
            PowerArmorModels.Suit suit = PowerArmorModels.get(slot);
            if (suit == null) {
                continue;
            }
            for (PowerArmorModels.Binding binding : suit.bindings()) {
                suit.instance().setNodeVisible(binding.node(), true);
                posePart(suit, binding, partOf(model, binding.part()));
            }
            suits.add(suit);
        }

        if (suits.isEmpty()) {
            return;
        }
        pose.pushPose();
        pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        for (PowerArmorModels.Suit suit : suits) {
            GltfRenderer.render(suit.instance(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }

    /** {@code T(pivot) * R * T(-pivot)} in biped pixels, written as the node's local matrix. */
    public static void posePart(PowerArmorModels.Suit suit, PowerArmorModels.Binding binding, ModelPart part) {
        Quaternionf rotation = new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot);
        Matrix4f matrix = new Matrix4f()
            .translate(part.x, part.y, part.z)
            .rotate(rotation)
            .translate(-part.x, -part.y, -part.z);
        float[] columnMajor = new float[16];
        matrix.get(columnMajor);
        suit.instance().nodes().node(binding.node()).localMatrix(columnMajor);
    }

    public static ModelPart partOf(PlayerModel<?> model, PowerArmorModels.Part part) {
        return switch (part) {
            case HEAD -> model.head;
            case BODY -> model.body;
            case LEFT_ARM -> model.leftArm;
            case RIGHT_ARM -> model.rightArm;
            case LEFT_LEG -> model.leftLeg;
            case RIGHT_LEG -> model.rightLeg;
        };
    }

    private static boolean wears(AbstractClientPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && stack.getItem() instanceof PowerArmorItem;
    }
}
