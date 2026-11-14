package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * HBM's four damage categories plus the "everything else" bucket.
 *
 * <p>NTM decided the category from flags on the 1.7.10 {@code DamageSource} (isExplosion / isFire /
 * isProjectile / its own energy damage classes). 1.20.1 replaced those flags with damage type tags, so the
 * mapping is done with the tags instead; anything that matches none of them lands in {@link #OTHER},
 * which is what HBM's {@code setOther} covers.</p>
 */
public enum DamageCategory {
    PHYSICAL,
    FIRE,
    EXPLOSION,
    ENERGY,
    OTHER;

    public static DamageCategory of(DamageSource source) {
        var holder = source.typeHolder();
        if (holder.is(DamageTypeTags.IS_EXPLOSION)) {
            return EXPLOSION;
        }
        if (holder.is(DamageTypeTags.IS_FIRE)) {
            return FIRE;
        }
        if (holder.is(DamageTypeTags.IS_PROJECTILE) || holder.is(DamageTypeTags.IS_FALL)
            || holder.is(DamageTypeTags.IS_DROWNING) || holder.is(DamageTypeTags.IS_FREEZING)) {
            return PHYSICAL;
        }
        // Anything a living entity deals with its body counts as physical, like HBM's EntityDamageSource.
        if (source.getEntity() != null) {
            return PHYSICAL;
        }
        // Vanilla has no laser/plasma/microwave, so ENERGY is only ever reached by exact registrations.
        return OTHER;
    }
}
