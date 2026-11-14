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
 * Hides the player's own model entirely while a suit is worn.
 *
 * <p>One exception to "hide the player": the head under the hazmat helmet stays, because that suit's
 * visor is glass and the face behind it is part of the design. The T-51b helmet is closed, so its head
 * is hidden with the rest.</p>
 *
 * <p>Two rules:</p>
 * <ul>
 *   <li><b>Full set worn</b> - the whole player model is hidden, base layer and outer layer alike. The
 *       suit is the body, which is what HBM does, and it is the only way to stop the clipping that
 *       shows up at walking speed: partially hiding leaves the remaining vanilla geometry free to swing
 *       the hip and shoulder straight through the armour shells.</li>
 *   <li><b>Partial wear</b> - only the covered parts are hidden, otherwise the player would end up with
 *       invisible limbs between visible ones. This is where the suits are not fully "the body" yet, so
 *       a fast walk there can still clip; wearing the complete set avoids it.</li>
 * </ul>
 *
 * <p>The outer skin layer is a part of the body model, so it goes with it. 3D Skin Layers needs no
 * handling for the third-person pass - its meshes are gated on the base parts being visible - but it is
 * suppressed explicitly all the same, because its first-person path injects a mesh into the sleeve
 * regardless.</p>
 *
 * <p>{@link RenderPlayerEvent.Pre} fires right after {@code setModelProperties} re-applies the player's
 * own toggles, and nothing is ever forced back on, so taking the suit off restores the body on the next
 * frame.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class HazmatBodyReplacement {
    /** Players whose 3D skin meshes we cleared, so they can be rebuilt once the suit comes off. */
    private static final Set<AbstractClientPlayer> SUPPRESSED =
        Collections.newSetFromMap(new WeakHashMap<>());

    private HazmatBodyReplacement() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // PlayerRenderer only ever draws client players, so this cast is exact.
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        boolean helmet = ours(player, EquipmentSlot.HEAD);
        boolean chestplate = ours(player, EquipmentSlot.CHEST);
        boolean leggings = ours(player, EquipmentSlot.LEGS);
        boolean boots = ours(player, EquipmentSlot.FEET);
        // The hazmat visor is glass and is supposed to show the face behind it, so that head stays.
        // The T-51b helmet is closed, so its head is hidden like the rest of the body.
        boolean glassVisor = ours(player, EquipmentSlot.HEAD, HazmatArmorItem.class);

        if (!(helmet || chestplate || leggings)) {
            if (SUPPRESSED.remove(player)) {
                // Cleared meshes are never rebuilt on their own: forget the cached skin once.
                ThirdPartySkinLayers.invalidate(player);
            }
            return;
        }

        if (helmet && chestplate && leggings && boots) {
            // Full set: the player goes away, except the head under a glass visor - the hazmat suit is
            // designed to show its face through the pane, while a closed helmet shows nothing anyway.
            model.head.visible = glassVisor;
            model.hat.visible = false;
            model.body.visible = false;
            model.rightArm.visible = false;
            model.leftArm.visible = false;
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.jacket.visible = false;
            model.leftSleeve.visible = false;
            model.rightSleeve.visible = false;
            model.leftPants.visible = false;
            model.rightPants.visible = false;
        } else {
            // Partial wear: only hide what the worn pieces really cover, otherwise the player would be
            // left with invisible limbs between two visible ones.
            if (helmet) {
                model.head.visible = glassVisor;
                model.hat.visible = false;
            }
            if (chestplate) {
                model.body.visible = false;
                model.rightArm.visible = false;
                model.leftArm.visible = false;
                model.jacket.visible = false;
                model.leftSleeve.visible = false;
                model.rightSleeve.visible = false;
            }
            // HBM's leggings cover the whole leg, so they hide it alone; the hazmat leggings stop below
            // the knee and only complete the leg with the boots.
            boolean hideLegs = (leggings && boots)
                || (leggings && ours(player, EquipmentSlot.LEGS, PowerArmorItem.class));
            if (hideLegs) {
                model.rightLeg.visible = false;
                model.leftLeg.visible = false;
                model.leftPants.visible = false;
                model.rightPants.visible = false;
            }
        }

        SUPPRESSED.add(player);
        ThirdPartySkinLayers.suppress(player);
    }


    private static boolean ours(AbstractClientPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty()
            && (stack.getItem() instanceof HazmatArmorItem || stack.getItem() instanceof PowerArmorItem);
    }

    private static boolean ours(AbstractClientPlayer player, EquipmentSlot slot, Class<?> itemType) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && itemType.isInstance(stack.getItem());
    }
}
