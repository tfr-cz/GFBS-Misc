package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The T-51b's footstep, ported from HBM instead of invented.
 *
 * <p>In NTM the T-51b is registered with {@code .setStep("hbm:step.metal")} - the {@code powered1..3}
 * set belongs to the other powered suits - and {@code ArmorFSB#steppy} plays it as
 * {@code player.playSound(sound, 0.25F, 1.0F)}: volume 0.25, pitch fixed at 1.0, no randomisation. The
 * rhythm comes from the same counter vanilla uses, {@code distanceWalkedOnStepModified} (renamed
 * {@code moveDist} after 1.14, still {@code += movement * 0.6}), and it only fires when the block under
 * the feet is not air. All of that is reproduced here - the first version used the wrong clip, doubled
 * the volume, jittered the pitch and drove the cadence from horizontal distance instead.</p>
 *
 * <p>HBM has no custom step sound for the hazmat suit, so only the complete T-51b set clicks.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
    modid = "gfbs_misc",
    value = {Dist.CLIENT}
)
public final class ArmorFootsteps {
    /** HBM plays at 0.25; the sample is already a heavy metal step. */
    private static final float VOLUME = 0.25F;
    private static final float PITCH = 1.0F;
    /** One unit of moveDist is one vanilla stride (moveDist gains 0.6 per block walked). */
    private static final float STEP_UNITS = 1.0F;
    /** A jump this large is a teleport, not a stride. */
    private static final float MAX_STRIDE_DELTA = 4.0F;

    private static final Map<Player, Float> LAST_MOVE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Player, Float> PENDING = Collections.synchronizedMap(new WeakHashMap<>());

    private ArmorFootsteps() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Player player = event.player;
        // Local player only: remote players' steps already arrive from the server.
        if (!player.level().isClientSide() || Minecraft.getInstance().player != player
            || !(player instanceof AbstractClientPlayer)) {
            return;
        }

        // moveDist is the counter Entity#move advances. Entity.xo is only written on teleports, which is
        // why an "x - xo" delta never worked.
        float moved = player.moveDist;
        Float previous = LAST_MOVE.put(player, moved);

        if (!PowerArmorCharge.hasPoweredSet(player) || !canStep(player)) {
            PENDING.put(player, 0.0F);
            return;
        }
        if (previous == null) {
            return;
        }

        float delta = moved - previous;
        if (delta <= 0.0F || delta > MAX_STRIDE_DELTA) {
            PENDING.put(player, 0.0F);
            return;
        }

        float pending = PENDING.getOrDefault(player, 0.0F) + delta;
        if (pending < STEP_UNITS) {
            PENDING.put(player, pending);
            return;
        }
        PENDING.put(player, pending - STEP_UNITS);

        player.level().playLocalSound(player.getX(), player.getY(), player.getZ(),
            ModSounds.ARMOR_STEP_METAL.get(), SoundSource.PLAYERS, VOLUME, PITCH, false);
    }

    /** HBM's conditions: on the ground, no liquid or ladder, not riding or flying, with a floor below. */
    private static boolean canStep(Player player) {
        return player.onGround()
            && !player.isInWater()
            && !player.isInLava()
            && !player.onClimbable()
            && !player.isPassenger()
            && !player.getAbilities().flying
            && !player.isSpectator()
            && !player.getBlockStateOn().isAir();
    }

}
