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
 * Turns off the player's skin overlay everywhere the protective suit covers the body.
 *
 * <p>Two separate things have to be dealt with, because the overlay can be drawn twice:</p>
 *
 * <ol>
 *   <li><b>Vanilla's flat second layer.</b> The suit hides it by flagging the matching
 *       {@link PlayerModel} parts invisible: helmet hides the hat, chestplate hides the jacket and
 *       both sleeves, leggings hide both pants. Boots have no overlay part, and the base
 *       {@code head}/{@code body}/{@code arm}/{@code leg} parts are never touched - the face is
 *       meant to stay visible through the helmet glass, and Forge's {@code copyModelProperties}
 *       copies part visibility from the player model onto the armor model, so hiding a base part
 *       would hide the suit mesh riding on it too.</li>
 *   <li><b>Real 3D skin geometry.</b> If 3D Skin Layers (skinlayers3d) is installed it builds its own
 *       meshes from the skin file and renders them from a layer of its own; those meshes ignore the
 *       overlay part visibility, so they are dropped through the mod's own
 *       {@code PlayerSettings#clearMeshes}. See {@link ThirdPartySkinLayers}, which resolves that
 *       call reflectively and quietly does nothing when the mod is absent or has renamed it.</li>
 * </ol>
 *
 * <p>The work happens on {@link RenderPlayerEvent.Pre}, which Forge fires right after
 * {@code PlayerRenderer#setModelProperties} re-applies the player's skin toggles, so nothing
 * overwrites it that frame. It is recomputed every frame, so taking a piece off restores the
 * overlay immediately.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class HazmatSkinLayers {
    private HazmatSkinLayers() {
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        // PlayerRenderer only ever draws client players, so this cast is exact.
        AbstractClientPlayer player = (AbstractClientPlayer) event.getEntity();
        PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();

        boolean helmet = wears(player, EquipmentSlot.HEAD);
        boolean chest = wears(player, EquipmentSlot.CHEST);
        boolean legs = wears(player, EquipmentSlot.LEGS);

        model.hat.visible = !helmet;
        model.jacket.visible = !chest;
        model.leftSleeve.visible = !chest;
        model.rightSleeve.visible = !chest;
        model.leftPants.visible = !legs;
        model.rightPants.visible = !legs;

        if (helmet || chest || legs) {
            ThirdPartySkinLayers.drop3dMeshes(player);
        }
    }

    private static boolean wears(AbstractClientPlayer player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        return !stack.isEmpty() && stack.getItem() instanceof HazmatArmorItem;
    }
}
