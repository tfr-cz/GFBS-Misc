package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.util.Mth;

/**
 * One HBM resistance entry: a flat threshold plus a percentage, in that order of application.
 *
 * @param threshold flat damage absorbed first ({@code dt} in NTM)
 * @param resistance fraction that survives, so 0.25 blocks a quarter ({@code dr} in NTM)
 */
public record ArmorResistance(float threshold, float resistance) {
    public static final ArmorResistance NONE = new ArmorResistance(0.0F, 0.0F);

    /**
     * HBM's {@code DamageResistanceHandler#calculateDamage}: absorb up to {@code dt}, then scale what is
     * left by {@code 1 - dr}. No pierce value is modelled because nothing in this port pierces yet.
     */
    public float apply(float amount) {
        float remaining = amount - Math.max(0.0F, this.threshold);
        if (remaining <= 0.0F) {
            return 0.0F;
        }
        return remaining * (1.0F - Mth.clamp(this.resistance, 0.0F, 1.0F));
    }

    /** True when this entry alone would cancel the hit outright (HBM's {@code dr >= 1}). */
    public boolean isComplete() {
        return this.resistance >= 1.0F;
    }
}
