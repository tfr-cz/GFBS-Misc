package org.lytharalab.gfbs.doors.client.armor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderArmEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.HazmatArmorItem;

/**
 * Draws the suit's arm in first person instead of the player's skin arm.
 *
 * <p>Forge fires {@link RenderArmEvent} from {@code PlayerRenderer#renderRightHand} /
 * {@code renderLeftHand}, at the point where the vanilla arm would be drawn and with the first-person
 * pose already on the stack. Cancelling it and rendering our own arm there puts the sleeve and the
 * glove where the bare arm used to be, which is what HBM does - and it matters more than usual here,
 * because the rest of the body is replaced by the suit (see {@link HazmatBodyReplacement}), so a skin
 * arm in the corner of the screen would be the one place the player still sees skin.</p>
 *
 * <p>The pose is rebuilt locally: vanilla's first-person arm is the model with zeroed animation
 * ({@code setupAnim} with all-zero arguments) and a zeroed arm pitch, so the same values are applied to
 * the cached suit model before it is drawn. Only that arm's subtree is rendered, so the torso and hood
 * never end up in front of the camera.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class HazmatFirstPersonArm {
    private static final ResourceLocation CHEST_TEXTURE =
        new ResourceLocation("gfbs_doors", "textures/armor/hazmat_chest.png");

    private HazmatFirstPersonArm() {
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        AbstractClientPlayer player = event.getPlayer();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.isEmpty() || !(chest.getItem() instanceof HazmatArmorItem)) {
            return;
        }

        HumanoidModel<LivingEntity> suit = HazmatArmorModels.get(EquipmentSlot.CHEST);
        suit.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

        ModelPart arm = event.getArm() == HumanoidArm.RIGHT ? suit.rightArm : suit.leftArm;
        arm.xRot = 0.0F;
        arm.yRot = 0.0F;
        arm.zRot = 0.0F;

        PoseStack pose = event.getPoseStack();
        VertexConsumer buffer = event.getMultiBufferSource().getBuffer(RenderType.entitySolid(CHEST_TEXTURE));
        arm.render(pose, buffer, event.getPackedLight(), OverlayTexture.NO_OVERLAY);
        event.setCanceled(true);
    }
}
