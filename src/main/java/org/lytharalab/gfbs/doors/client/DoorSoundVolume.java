package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;

/**
 * Volume model shared by the vanilla loop and the Auralis loop.
 *
 * <p>Vanilla picks the {@code BLOCKS} slider up automatically for a {@code SoundSource.BLOCKS} instance, so the
 * vanilla path only needs the per-door base volume. Auralis bypasses that mixing, so it has to apply both the
 * {@code BLOCKS} and {@code MASTER} sliders itself.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class DoorSoundVolume {
    private DoorSoundVolume() {
    }

    public static float baseVolume(DoorSpec spec) {
        return spec == null ? 0.0F : Mth.clamp(spec.soundVolume, 0.0F, 1.0F);
    }

    public static float blocksMultiplier() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.options != null
            ? Mth.clamp(mc.options.getSoundSourceVolume(SoundSource.BLOCKS), 0.0F, 1.0F)
            : 1.0F;
    }

    public static float auralisMultiplier() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) {
            return 1.0F;
        }
        float blocks = Mth.clamp(mc.options.getSoundSourceVolume(SoundSource.BLOCKS), 0.0F, 1.0F);
        float master = Mth.clamp(mc.options.getSoundSourceVolume(SoundSource.MASTER), 0.0F, 1.0F);
        return blocks * master;
    }
}
