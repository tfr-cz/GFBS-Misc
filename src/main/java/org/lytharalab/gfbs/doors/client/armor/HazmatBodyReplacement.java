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
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

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
 *   <li>helmet hides only {@code hat}: the head itself stays, because the visor is glass and the
 *       face is supposed to be visible through it. The hat layer is hidden because it is inflated by
 *       0.5 and would poke through the hood's walls (the hood's inside is only 0.23 wider than the
 *       head)</li>
 *   <li>chestplate hides {@code body} and both arms - the sleeves reach past the wrist into the gloves
 *       and the torso runs from the collar to below the belt</li>
 *   <li>leggings hide both legs, but only together with the boots: the leggings stop below the knee
 *       and the boots cover the shins, so hiding the legs on their own would leave the player with
 *       floating thighs. Without the boots the vanilla legs stay and the leggings ride on them</li>
 *   <li>boots alone hide nothing: hiding the legs would leave the boots with no thigh above them</li>
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
    /** Players whose 3D skin meshes were dropped while the helmet hid their hair. */
    private static final Set<AbstractClientPlayer> SUPPRESSED =
        Collections.newSetFromMap(new WeakHashMap<>());

    private HazmatBodyReplacement() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // PlayerRenderer only ever draws client players, so this cast is exact.
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        boolean helmet = wears(player, EquipmentSlot.HEAD);
        boolean chestplate = wears(player, EquipmentSlot.CHEST);
        // HBM's leggings cover the whole leg, so they hide it on their own; the hazmat leggings stop
        // below the knee and only complete the leg together with the boots.
        boolean legs = wears(player, EquipmentSlot.LEGS, PowerArmorItem.class)
            || (wears(player, EquipmentSlot.LEGS) && wears(player, EquipmentSlot.FEET));

        if (helmet) {
            model.hat.visible = false;
            // 3D Skin Layers builds its hair mesh from the outer texture layer, inflated like the hat
            // layer - it would grow through the hood. Its meshes are gated on the head part, which is
            // visible now, so they have to be dropped explicitly while the helmet is on.
            SUPPRESSED.add(player);
            ThirdPartySkinLayers.suppress(player);
        } else if (SUPPRESSED.remove(player)) {
            ThirdPartySkinLayers.invalidate(player);
        }
        if (chestplate) {
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
        }
        // The leggings end below the knee; only the boots complete the leg, so the vanilla legs are
        // kept (and the leggings ride over them) until both pieces are on.
        if (legs) {
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        }
    }

    private static boolean wears(AbstractClientPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty()
            && (stack.getItem() instanceof HazmatArmorItem || stack.getItem() instanceof PowerArmorItem);
    }

    private static boolean wears(AbstractClientPlayer player, EquipmentSlot slot, Class<?> itemType) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && itemType.isInstance(stack.getItem());
    }
}
