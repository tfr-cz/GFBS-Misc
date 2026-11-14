package org.lytharalab.gfbs.doors.content.armor.resistance;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorCharge;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;

import java.util.EnumMap;
import java.util.HashMap;
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
    private static final float[] NO_RESISTANCE = {0.0F, 0.0F};

    private static final Map<Item, ArmorResistanceTable> ITEM_TABLES = new HashMap<>();
    private static final Map<WornSet, ArmorResistanceTable> SET_TABLES = new HashMap<>();

    /**
     * The four worn pieces, in slot order, with {@code null} for an empty slot.
     *
     * <p>This was a {@code List.of(...)} at first, which is wrong twice over: that factory rejects null
     * elements outright, and an immutable list throws {@link NullPointerException} from
     * {@code contains(null)} (which is how it crashed the server tick). A record holds nulls happily and
     * compares them by reference.</p>
     */
    record WornSet(Item head, Item chest, Item legs, Item feet) {
    }

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
        SET_TABLES.put(keyOf(helmet, chest, legs, boots), table);
    }

    /** Package-private for the regression test: building the key must survive empty slots. */
    static WornSet keyOf(Item head, Item chest, Item legs, Item feet) {
        return new WornSet(head, chest, legs, feet);
    }

    /** Summed {@code dt} and {@code dr} for this entity against this source. */
    public static float[] resolve(LivingEntity entity, DamageSource source) {
        float threshold = 0.0F;
        float resistance = 0.0F;
        if (ITEM_TABLES.isEmpty() && SET_TABLES.isEmpty()) {
            return NO_RESISTANCE;
        }

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

        ArmorResistanceTable set = isUnpoweredPowerSet(entity) ? null : wornSetTable(entity);
        if (set != null) {
            ArmorResistance entry = set.resolve(source);
            threshold += entry.threshold();
            resistance += entry.resistance();
        }

        return new float[]{threshold, resistance};
    }

    /**
     * A T-51b set with any piece flat is "worn but dead": its set resistances must not apply, matching
     * HBM, where {@code hasFSBArmor} fails as soon as one piece reports {@code charge <= 0}.
     */
    private static boolean isUnpoweredPowerSet(LivingEntity entity) {
        boolean powerArmor = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).getItem() instanceof PowerArmorItem) {
                powerArmor = true;
                break;
            }
        }
        return powerArmor && !PowerArmorCharge.hasPoweredSet(entity);
    }

    private static ArmorResistanceTable wornSetTable(LivingEntity entity) {
        if (SET_TABLES.isEmpty()) {
            return null;
        }
        return SET_TABLES.get(keyOf(
            itemOf(entity, EquipmentSlot.HEAD),
            itemOf(entity, EquipmentSlot.CHEST),
            itemOf(entity, EquipmentSlot.LEGS),
            itemOf(entity, EquipmentSlot.FEET)));
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
