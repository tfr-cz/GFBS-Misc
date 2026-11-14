package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.HazmatArmorItem;

/**
 * Replaces the player's own model with the suit wherever the suit covers the body.
 *
 * <p>This is the HBM approach: the armour is not a shell worn over a body, it <b>is</b> the body. Where
 * a suit piece is worn, the matching vanilla {@link PlayerModel} parts are hidden, so nothing of the
 * player's skin - neither the base layer nor the outer layer - can ever show through a seam or swing
 * out of an arm hole. The suit itself is rendered by the vanilla armour layer, which is unaffected:
 * that layer takes its part visibility from {@code setPartVisibility}, not from the player model, so
 * hiding the body cannot hide the armour.</p>
 *
 * <p>Coverage, so nothing goes missing:</p>
 * <ul>
 *   <li>helmet hides {@code head} - the hood covers it whole, and the visor is opaque anyway</li>
 *   <li>chestplate hides {@code body} and both arms - the sleeves reach past the wrist into the gloves
 *       and the torso runs from the collar to below the belt</li>
 *   <li>leggings hide both legs - the thigh shells meet the boot cuffs</li>
 *   <li>boots hide nothing extra: a bare-foot slot would leave a leg with no thigh above the boot</li>
 * </ul>
 *
 * <p>The outer skin layer is hidden along with the parts it belongs to. 3D Skin Layers needs no
 * special handling: its meshes are gated on the base part being visible, so hiding the body switches
 * it off by itself. Capes and ears keep rendering - in 1.20.1 they are drawn by their own layers with
 * their own model parts, not by the player model, so they simply sit on top of the suit.</p>
 *
 * <p>{@link RenderPlayerEvent.Pre} fires right after {@code setModelProperties} re-applies the player's
 * own toggles, and only the covered parts are hidden - nothing is ever forced back on, so taking a
 * piece off restores the body on the next frame.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class HazmatBodyReplacement {
    private HazmatBodyReplacement() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // PlayerRenderer only ever draws client players, so this cast is exact.
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        if (wears(player, EquipmentSlot.HEAD)) {
            model.head.visible = false;
            model.hat.visible = false;
        }
        if (wears(player, EquipmentSlot.CHEST)) {
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        if (wears(player, EquipmentSlot.LEGS)) {
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }

    private static boolean wears(AbstractClientPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && stack.getItem() instanceof HazmatArmorItem;
    }
}
