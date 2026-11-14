package org.lytharalab.gfbs.doors.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.armor.HazmatArmorItem;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;

/**
 * Draws the helmet's visor pane through the glTF renderer, so the face behind it shows through.
 *
 * <p>The armour layer renders through {@code RenderType.armorCutoutNoCull}, an alpha test, which has
 * no partial transparency: the pane's cell in the helmet sheet is therefore empty and the pane comes
 * from a glTF asset instead, whose {@code BLEND} material lands on the glTF renderer's translucent
 * render type. The pane is modelled directly in the biped model space the armour model uses, so all
 * this layer has to do is apply the head part's transform and a 1/16 scale.</p>
 *
 * <p>Until the asset has finished loading - and if it failed - the pane is drawn from the armour
 * model with the fallback sheet, so the window never goes missing. Both paths use the same pose copy
 * the armour layer itself performs, so they land in exactly the same place.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatVisorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation FALLBACK_TEXTURE =
        new ResourceLocation("gfbs_doors", "textures/armor/hazmat_visor.png");
    private static final float MODEL_SCALE = 1.0F / 16.0F;

    public HazmatVisorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float age, float yaw, float pitch) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !(helmet.getItem() instanceof HazmatArmorItem)) {
            return;
        }
        if (!(HazmatArmorModels.get(EquipmentSlot.HEAD) instanceof HazmatArmorModel suit)) {
            return;
        }

        @SuppressWarnings("unchecked")
        HumanoidModel<AbstractClientPlayer> posed = (HumanoidModel<AbstractClientPlayer>) (HumanoidModel<?>) suit;
        this.getParentModel().copyPropertiesTo(posed);

        HazmatVisorModel.request();
        GltfInstance visor = HazmatVisorModel.instance();

        pose.pushPose();
        suit.head.translateAndRotate(pose);
        if (visor != null) {
            pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
            GltfRenderer.render(visor, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        } else {
            VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(FALLBACK_TEXTURE));
            suit.visor().render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
    }
}
