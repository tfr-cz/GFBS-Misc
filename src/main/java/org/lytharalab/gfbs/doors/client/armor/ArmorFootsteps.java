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
 * <p>Only a complete set makes the sound: boots on their own are just boots. The rule is the same one
 * {@code HazmatBodyReplacement} uses for hiding the body - all four slots ours - with the volume taken
 * from the boots, because that is the piece that meets the ground.</p>
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
    /** Blocks per step, converted to walkDist units: Entity#move adds 0.6 per block walked. */
    private static final double BLOCKS_PER_STEP = 1.55D;
    private static final float STEP_UNITS = (float) (BLOCKS_PER_STEP * 0.6D);
    /** A jump this large is a teleport, not a stride; drop it instead of firing a step. */
    private static final float MAX_STRIDE_DELTA = 4.0F;

    private static final Map<Player, Float> LAST_WALK = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Float> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

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

        // Entity.xo is only written on teleports in 1.20.1 (absMoveTo/moveTo), so a per-tick
        // "getX() - xo" delta is almost always zero - which is why this never fired. walkDist is the
        // stride counter Entity#move actually moves, so it is the right thing to read.
        float walked = player.walkDist;
        Float previous = LAST_WALK.put(player, walked);

        Suit suit = suitOf(player);
        if (suit == null || !onFoot(player)) {
            PENDING.put(player, 0.0F);
            return;
        }
        if (previous == null) {
            return;
        }

        float delta = walked - previous;
        if (delta <= 0.0F || delta > MAX_STRIDE_DELTA) {
            // Standing still, or the player was moved rather than walked.
            PENDING.put(player, 0.0F);
            return;
        }

        float pending = PENDING.getOrDefault(player, 0.0F) + delta;
        if (pending < STEP_UNITS) {
            PENDING.put(player, pending);
            return;
        }
        PENDING.put(player, pending - STEP_UNITS);

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

    /** A complete set of one of our suits; the volume follows the boots, the piece on the ground. */
    private static Suit suitOf(Player player) {
        EquipmentSlot[] slots = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : slots) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()
                || !(stack.getItem() instanceof PowerArmorItem || stack.getItem() instanceof HazmatArmorItem)) {
                return null;
            }
        }
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        return boots.getItem() instanceof PowerArmorItem ? Suit.POWER : Suit.HAZMAT;
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
