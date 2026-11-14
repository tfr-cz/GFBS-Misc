package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for armour damage resistances, the 1.20.1 counterpart of HBM's
 * {@code DamageResistanceHandler#registerSet} / {@code #registerItem}.
 *
 * <p>HBM summed the thresholds and percentages of the worn set plus every worn piece (plus the entity's
 * innate profile) before applying them. That is kept: a set and a piece can both contribute, and the sums
 * are clamped at the moment of use.</p>
 *
 * <p>Register once during common setup; the lookup is on the damage path, so the maps are plain
 * {@link HashMap}s keyed by item.</p>
 */
public final class ArmorResistances {
    private static final Map<Item, ArmorResistanceTable> ITEM_TABLES = new HashMap<>();
    private static final Map<List<Item>, ArmorResistanceTable> SET_TABLES = new HashMap<>();

    private ArmorResistances() {
    }

    public static void registerItem(Item item, ArmorResistanceTable table) {
        ITEM_TABLES.put(item, table);
    }

    /**
     * Registers a four-piece set. Order is helmet, chest, legs, boots, matching the slots the lookup walks.
     * The set only counts while all four are worn.
     */
    public static void registerSet(Item helmet, Item chest, Item legs, Item boots, ArmorResistanceTable table) {
        SET_TABLES.put(List.of(helmet, chest, legs, boots), table);
    }

    /** Summed {@code dt} and {@code dr} for this entity against this source. */
    public static float[] resolve(LivingEntity entity, DamageSource source) {
        float threshold = 0.0F;
        float resistance = 0.0F;

        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ArmorResistanceTable table = ITEM_TABLES.get(stack.getItem());
            if (table != null) {
                ArmorResistance entry = table.resolve(source);
                threshold += entry.threshold();
                resistance += entry.resistance();
            }
        }

        ArmorResistanceTable set = wornSetTable(entity);
        if (set != null) {
            ArmorResistance entry = set.resolve(source);
            threshold += entry.threshold();
            resistance += entry.resistance();
        }

        return new float[]{threshold, resistance};
    }

    private static ArmorResistanceTable wornSetTable(LivingEntity entity) {
        if (SET_TABLES.isEmpty()) {
            return null;
        }
        List<Item> worn = List.of(
            itemOf(entity, EquipmentSlot.HEAD),
            itemOf(entity, EquipmentSlot.CHEST),
            itemOf(entity, EquipmentSlot.LEGS),
            itemOf(entity, EquipmentSlot.FEET));
        if (worn.contains(null)) {
            return null;
        }
        return SET_TABLES.get(worn);
    }

    private static Item itemOf(LivingEntity entity, EquipmentSlot slot) {
        ItemStack stack = entity.getItemBySlot(slot);
        return stack.isEmpty() ? null : stack.getItem();
    }

    /** Category → entry, for tooltips and debugging. */
    public static Map<DamageCategory, ArmorResistance> categoriesOf(Item item) {
        ArmorResistanceTable table = ITEM_TABLES.get(item);
        return table == null ? new EnumMap<>(DamageCategory.class) : Map.copyOf(categories(table));
    }

    private static Map<DamageCategory, ArmorResistance> categories(ArmorResistanceTable table) {
        Map<DamageCategory, ArmorResistance> map = new EnumMap<>(DamageCategory.class);
        for (DamageCategory category : DamageCategory.values()) {
            map.put(category, table.resolveCategory(category));
        }
        return map;
    }
}
