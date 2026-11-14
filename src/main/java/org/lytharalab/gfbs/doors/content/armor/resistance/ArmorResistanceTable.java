package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A piece's or a set's resistance profile - the 1.20.1 shape of HBM's {@code ResistanceStats}.
 *
 * <p>HBM built these with {@code addCategory("PHYS", dt, dr)}, {@code addExact(name, dt, dr)} and
 * {@code setOther(dt, dr)}; the builder below keeps those names so a ported set can be written down
 * exactly as it appears in NTM.</p>
 */
public final class ArmorResistanceTable {
    private final Map<DamageCategory, ArmorResistance> categories = new EnumMap<>(DamageCategory.class);
    private final Map<ResourceKey<DamageType>, ArmorResistance> exact = new HashMap<>();
    private ArmorResistance other = ArmorResistance.NONE;

    public static Builder builder() {
        return new Builder();
    }

    /** Just the category entry, ignoring exact types - used for tooltips and debugging. */
    public ArmorResistance resolveCategory(DamageCategory category) {
        ArmorResistance entry = this.categories.get(category);
        return entry != null ? entry : this.other;
    }

    /** The entry that applies to a damage source: exact type first, then category, then the fallback. */
    public ArmorResistance resolve(DamageSource source) {
        ArmorResistance byType = this.exact.get(source.typeHolder().unwrapKey().orElse(null));
        if (byType != null) {
            return byType;
        }
        ArmorResistance byCategory = this.categories.get(DamageCategory.of(source));
        return byCategory != null ? byCategory : this.other;
    }

    public static final class Builder {
        private final ArmorResistanceTable table = new ArmorResistanceTable();

        public Builder addCategory(DamageCategory category, float threshold, float resistance) {
            this.table.categories.put(category, new ArmorResistance(threshold, resistance));
            return this;
        }

        public Builder addExact(ResourceKey<DamageType> type, float threshold, float resistance) {
            this.table.exact.put(type, new ArmorResistance(threshold, resistance));
            return this;
        }

        public Builder setOther(float threshold, float resistance) {
            this.table.other = new ArmorResistance(threshold, resistance);
            return this;
        }

        public ArmorResistanceTable build() {
            return this.table;
        }
    }
}
