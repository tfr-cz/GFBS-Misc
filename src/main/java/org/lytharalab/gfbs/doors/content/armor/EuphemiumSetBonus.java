package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * HBM's {@code ArmorEuphemium#onArmorTick}: while the whole suit is worn the wearer is kept on regeneration,
 * resistance, fire resistance and saturation, and their fall is capped.
 *
 * <p>1.20.1 has no per-piece armour tick, and the bonus is a property of the set rather than of a piece, so this
 * runs once per player tick instead of once per worn piece. The effects are re-applied every tick with a 5 tick
 * duration, as in NTM, so they lapse the same tick a piece comes off instead of lingering.</p>
 */
@EventBusSubscriber(
    modid = "gfbs_misc"
)
public final class EuphemiumSetBonus {
    /** NTM passes 127, which is level 128 with 1.20.1's zero-based amplifier. */
    private static final int AMPLIFIER = 127;
    private static final int DURATION_TICKS = 5;
    /** HBM's terminal fall speed while the suit is worn. */
    private static final double MAX_FALL_SPEED = -0.25D;

    private EuphemiumSetBonus() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Player player = event.player;
        if (!EuphemiumArmorItem.isCompleteSet(player)) {
            return;
        }

        // HBM clamps the fall itself rather than handing out slow falling: cap the speed, then forget the drop
        // so no fall damage is accumulated on the way either. Both sides run this, because the client predicts
        // the same motion and a server-only clamp would rubber-band.
        Vec3 motion = player.getDeltaMovement();
        if (motion.y < MAX_FALL_SPEED) {
            player.setDeltaMovement(motion.x, MAX_FALL_SPEED, motion.z);
            player.resetFallDistance();
        }

        // Effects are authoritative on the server; letting the client apply them too would only fight the sync.
        if (player.level().isClientSide()) {
            return;
        }
        refresh(player, MobEffects.REGENERATION);
        refresh(player, MobEffects.DAMAGE_RESISTANCE);
        refresh(player, MobEffects.FIRE_RESISTANCE);
        refresh(player, MobEffects.SATURATION);
    }

    private static void refresh(Player player, MobEffect effect) {
        player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, AMPLIFIER, true, true, true));
    }
}
