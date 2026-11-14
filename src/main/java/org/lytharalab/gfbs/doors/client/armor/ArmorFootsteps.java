package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.lytharalab.gfbs.doors.content.armor.HazmatArmorItem;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Heavy footsteps for the suits, using HBM's own {@code footsteps/powered1..3}.
 *
 * <p>Vanilla resolves a step sound from the block under the player, which would give the suit the
 * sound of grass or gravel. Instead the distance the player actually walks is accumulated and a step
 * is played every {@link #STRIDE} blocks, the same cadence the vanilla stride produces, so the suit
 * stomps at the rhythm of the walk rather than at a fixed interval.</p>
 *
 * <p>Only the local player is handled: vanilla sends other players' steps from the server, and the
 * suits have no server-side behaviour yet ("appearance only"), so their steps stay vanilla for now.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class ArmorFootsteps {
    /** Blocks per step; vanilla's stride lands close to this, so the beats line up with the walk. */
    private static final double STRIDE = 1.55D;
    /** Below this much movement per tick the player is not walking, just shuffling in place. */
    private static final double MIN_STEP_MOVEMENT = 0.01D;

    private static final Map<Player, Double> WALKED = Collections.synchronizedMap(new WeakHashMap<>());

    private ArmorFootsteps() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        // Only the local player: remote players' steps already arrive from the server, and adding a
        // second clip on top of vanilla's would double every step.
        if (!player.level().isClientSide() || Minecraft.getInstance().player != player
            || !(player instanceof AbstractClientPlayer)) {
            return;
        }

        Suit suit = suitOf(player);
        if (suit == null) {
            WALKED.remove(player);
            return;
        }
        if (!onFoot(player)) {
            WALKED.remove(player);
            return;
        }

        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < MIN_STEP_MOVEMENT) {
            return;
        }

        double walked = WALKED.merge(player, moved, Double::sum);
        if (walked < STRIDE) {
            return;
        }
        WALKED.put(player, walked - STRIDE);

        float pitch = 1.0F + (player.getRandom().nextFloat() - 0.5F) * 0.12F;
        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
            ModSounds.ARMOR_STEP_POWER.get(), SoundSource.PLAYERS,
            suit.volume, pitch, false);
    }

    private static boolean onFoot(Player player) {
        return player.onGround()
            && !player.isInWater()
            && !player.isInLava()
            && !player.onClimbable()
            && !player.isPassenger()
            && !player.getAbilities().flying
            && !player.isSpectator()
            && !player.isCrouching();
    }

    private static Suit suitOf(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) {
            return null;
        }
        if (boots.getItem() instanceof PowerArmorItem) {
            return Suit.POWER;
        }
        if (boots.getItem() instanceof HazmatArmorItem) {
            return Suit.HAZMAT;
        }
        return null;
    }

    /** The power armour is a powered exoskeleton, the hazmat suit is rubber - same clip, very different weight. */
    private enum Suit {
        POWER(0.9F),
        HAZMAT(0.45F);

        private final float volume;

        Suit(float volume) {
            this.volume = volume;
        }
    }
}
