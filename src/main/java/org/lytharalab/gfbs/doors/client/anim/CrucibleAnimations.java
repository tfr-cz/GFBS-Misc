package org.lytharalab.gfbs.doors.client.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.ModItems;
import org.lytharalab.gfbs.doors.content.ModSounds;

import java.util.Random;

/**
 * The Crucible's clips and when to start them, ported from {@code ItemCrucible#getAnimation} and
 * {@code ItemCrucible#onArmorTick}/{@code #onEntitySwing}.
 *
 * <p>NTM started these from the server and pushed them to the client with a packet. They are purely visual, so this
 * port starts them where they are used, on the client, off the local player's own state: a swing is detected from
 * {@code LivingEntity#swinging} and the equip clip from a change of the held item.</p>
 *
 * <p>Guard plates: HBM folded them shut (90 degrees) the moment the sword was drawn, held them for 800 ms, then
 * snapped them open in 50 ms. That is the whole equip clip.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class CrucibleAnimations {
    public static final String SWING_ROT = "SWING_ROT";
    public static final String SWING_TRANS = "SWING_TRANS";
    public static final String GUARD_ROT = "GUARD_ROT";

    private static final Random RANDOM = new Random();
    private static Item lastHeld;

    private CrucibleAnimations() {
    }

    /** Called once per client tick; does nothing unless the Crucible is the held item. */
    public static void tick() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastHeld = null;
            return;
        }
        ItemStack held = player.getMainHandItem();
        Item item = held.getItem();
        boolean ours = item == ModItems.CRUCIBLE.get();
        if (!ours) {
            lastHeld = item;
            return;
        }
        int slot = player.getInventory().selected;
        // HBM only started a swing clip when the previous one had run out, so a fast combo animates every third
        // hit instead of restarting the tumble every 250 ms.
        if (player.swinging && !ItemAnimations.isPlaying(SWING_ROT)) {
            ItemAnimations.play(slot, item, swing());
            player.playNotifySound(ModSounds.CRUCIBLE_SWING.get(), SoundSource.PLAYERS, 1.0F,
                0.8F + RANDOM.nextFloat() * 0.2F);
        }
        if (item != lastHeld) {
            ItemAnimations.play(slot, item, equip());
        }
        lastHeld = item;
    }

    /** {@code ItemCrucible#getAnimation} SWING: a tumble left, then right, then back to rest. */
    private static BusAnimation swing() {
        int offset = RANDOM.nextInt(80) - 20;
        return new BusAnimation()
            .addBus(SWING_ROT, new BusAnimationSequence()
                .addRot(90 - offset, 90 - offset, 35, 75)
                .addRot(90 + offset, 90 - offset, -45, 150)
                .addRot(0, 0, 0, 500))
            .addBus(SWING_TRANS, new BusAnimationSequence()
                .addPos(-3, 0, 0, 75)
                .addPos(8, 0, 0, 150)
                .addPos(0, 0, 0, 500));
    }

    /** {@code ItemCrucible#getAnimation} EQUIP: guards snap shut, hold, then open. */
    private static BusAnimation equip() {
        return new BusAnimation()
            .addBus(GUARD_ROT, new BusAnimationSequence()
                .addRot(90, 0, 1, 0)
                .addRot(90, 0, 1, 800)
                .addRot(0, 0, 1, 50));
    }
}
