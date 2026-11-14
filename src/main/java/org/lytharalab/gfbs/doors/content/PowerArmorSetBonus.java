package org.lytharalab.gfbs.doors.content;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;

/**
 * The T-51b set bonus, ported from HBM's {@code ArmorFSB.handleTick}.
 *
 * <p>In NTM the chestplate carries a list of {@code PotionEffect}s and the tick handler re-applies all of
 * them while the full set is worn ({@code hasFSBArmor}: every armour slot holding the same armour
 * material). The T-51b registers exactly one entry - a one-second amplifier-0 potion that is refreshed
 * every tick, which is how HBM keeps a permanent buff that also disappears within a second of taking the
 * armour off. This reproduces that shape, with the movement speed effect.</p>
 *
 * <p>Two deliberate differences: the effect has no particles (HBM leaves them on, which puts a swirl
 * around the player at all times), and it is not gated on a charge because this port has no power
 * system - {@code isArmorEnabled} has nothing to check yet.</p>
 *
 * <p>Applied server-side only: potion effects are server-authoritative, and a client-side instance would
 * simply be overwritten by the next sync.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class PowerArmorSetBonus {
    private static final MobEffect EFFECT = MobEffects.MOVEMENT_SPEED;
    private static final int AMPLIFIER = 0;
    /** One second, like HBM; refreshed before it runs out. */
    private static final int DURATION = 20;
    /** Refresh once the remaining time drops below this, instead of every single tick. */
    private static final int REFRESH_BELOW = 10;

    private PowerArmorSetBonus() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        if (player.level().isClientSide() || !PowerArmorCharge.hasFullSet(player)) {
            return;
        }
        // HBM drains every worn piece on its own armour tick; an empty piece also switches the set off.
        PowerArmorCharge.drainWornSet(player);
        if (!PowerArmorCharge.hasPoweredSet(player)) {
            return;
        }

        MobEffectInstance current = player.getEffect(EFFECT);
        if (current != null) {
            // Never downgrade: a stronger speed potion keeps running, and our own refresh waits until
            // the instance is about to expire.
            if (current.getAmplifier() > AMPLIFIER || current.getDuration() > REFRESH_BELOW) {
                return;
            }
        }
        player.addEffect(new MobEffectInstance(EFFECT, DURATION, AMPLIFIER, true, false, true));
    }

}
