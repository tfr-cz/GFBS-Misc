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

/**
 * Draws the helmet's visor pane as real glass, so the face behind it shows through.
 *
 * <p>The armour layer renders through {@code RenderType.armorCutoutNoCull}, which is an alpha test:
 * a pixel is either fully drawn or discarded, so there is no way to get partial transparency there.
 * The pane therefore has an empty cell in the helmet sheet (the alpha test throws that box away) and
 * is drawn once more here from {@code hazmat_visor.png} with {@code RenderType.entityTranslucent}.</p>
 *
 * <p>This layer is appended to the player renderers, so it runs after the armour layer in the same
 * frame. It still copies the pose itself rather than relying on that order - {@code copyPropertiesTo}
 * is exactly what the armour layer does before rendering, and it leaves the cached model posed for
 * whichever player is being drawn.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatVisorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation("gfbs_doors", "textures/armor/hazmat_visor.png");

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

        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        suit.visor().render(pose, consumer, light, OverlayTexture.NO_OVERLAY);
    }
}
