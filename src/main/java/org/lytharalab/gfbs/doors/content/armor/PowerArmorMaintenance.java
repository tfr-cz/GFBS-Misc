package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

import java.util.List;

/**
 * Clears the leftover {@code Damage} values older builds wrote onto T-51b pieces.
 *
 * <p>The suit never takes durability damage - combat is paid in charge - but before that rule existed the pieces
 * collected damage like normal armour, and stacks in existing worlds still carry it. That number is not inert:
 * {@code ItemStack.hurt} adds to it before calling {@code setDamage}, so a stored value shifts every later bill,
 * and any other mod reads the piece as damaged.</p>
 *
 * <p>{@link PowerArmorItem#setDamage} also clears it on the first hit; this sweep covers everything the player is
 * carrying the moment they join, spawn or die, so nothing keeps a stale value in the meantime.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class PowerArmorMaintenance {
    private PowerArmorMaintenance() {
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        normalize(event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        normalize(event.getEntity());
    }

    private static void normalize(Player player) {
        if (player.level().isClientSide()) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (List<ItemStack> stacks : List.of(inventory.items, inventory.armor, inventory.offhand)) {
            for (ItemStack stack : stacks) {
                if (stack.getItem() instanceof PowerArmorItem piece) {
                    piece.forgetStoredDamage(stack);
                }
            }
        }
    }
}
