package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Charge storage for the T-51b pieces, the 1.20.1 counterpart of HBM's {@code ArmorFSBPowered}.
 *
 * <p>Every piece carries its own {@code charge} long, exactly like NTM: all four drain their own
 * {@code 5}/tick while the full set is worn, and the set effects only run while every piece still has
 * charge ({@code hasPoweredSet} mirrors {@code ArmorFSB#hasFSBArmor}, which calls {@code isArmorEnabled} =
 * {@code charge > 0} on each piece). Nothing is shared, so pulling one piece off does not empty the
 * others.</p>
 *
 * <p>A piece without the tag counts as full: HBM's {@code getCharge} seeds the tag with {@code maxPower}
 * on first read, so a freshly crafted piece starts charged. This port keeps that result without writing
 * the tag on a read path.</p>
 */
public final class PowerArmorCharge {
    /** HBM's {@code maxPower} for every T-51b piece. */
    public static final long MAX_CHARGE = 1_000_000L;
    /** HBM's {@code drain}, per worn piece per tick. */
    public static final long DRAIN_PER_TICK = 5L;
    /** HBM's {@code consumption}: one point of incoming damage costs this much charge. */
    public static final long DAMAGE_CONSUMPTION = 1_000L;
    /** HBM's {@code chargeRate}: the most a charger may push into one piece per tick. */
    public static final long CHARGE_RATE = 10_000L;

    private static final String TAG = "charge";
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private PowerArmorCharge() {
    }

    public static boolean isPowerArmor(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof PowerArmorItem;
    }

    /** Current charge, clamped into {@code [0, MAX_CHARGE]}; a missing tag reads as full. */
    public static long get(ItemStack stack) {
        if (!isPowerArmor(stack)) {
            return 0L;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG)) {
            return MAX_CHARGE;
        }
        return clampCharge(tag.getLong(TAG));
    }

    public static void set(ItemStack stack, long charge) {
        if (!isPowerArmor(stack)) {
            return;
        }
        stack.getOrCreateTag().putLong(TAG, clampCharge(charge));
    }

    /** Adds up to {@code amount}; returns how much actually went in (0 when already full). */
    public static long add(ItemStack stack, long amount) {
        long before = get(stack);
        long after = added(before, amount);
        if (after != before) {
            set(stack, after);
        }
        return after - before;
    }

    /** Removes up to {@code amount}; returns how much actually came out (0 when already empty). */
    public static long discharge(ItemStack stack, long amount) {
        long before = get(stack);
        long after = removed(before, amount);
        if (after != before) {
            set(stack, after);
        }
        return before - after;
    }

    /** Pure math, so the saturation rules are testable without an item registry. */
    static long added(long current, long amount) {
        return clampCharge(current + Math.max(0L, amount));
    }

    static long removed(long current, long amount) {
        return clampCharge(current - Math.max(0L, amount));
    }

    public static boolean isPowered(ItemStack stack) {
        return get(stack) > 0L;
    }

    /** All four armour slots hold a T-51b piece, charge ignored. */
    public static boolean hasFullSet(LivingEntity entity) {
        return hasFullSet(stacksOf(entity));
    }

    /** All four slots hold a T-51b piece and every one of them still has charge. */
    public static boolean hasPoweredSet(LivingEntity entity) {
        return hasPoweredSet(stacksOf(entity));
    }

    public static boolean hasFullSet(Iterable<ItemStack> pieces) {
        int seen = 0;
        for (ItemStack piece : pieces) {
            if (!isPowerArmor(piece)) {
                return false;
            }
            seen++;
        }
        return seen == ARMOR_SLOTS.length;
    }

    public static boolean hasPoweredSet(Iterable<ItemStack> pieces) {
        int seen = 0;
        for (ItemStack piece : pieces) {
            if (!isPowerArmor(piece) || !isPowered(piece)) {
                return false;
            }
            seen++;
        }
        return seen == ARMOR_SLOTS.length;
    }

    /**
     * HBM's {@code ArmorFSBPowered#onArmorTick} drain: each worn piece pays {@link #DRAIN_PER_TICK} every
     * tick, but only while the full set is worn and only outside creative mode.
     *
     * @return {@code true} when the set was actually charged (i.e. all four pieces had charge to pay)
     */
    public static boolean drainWornSet(LivingEntity entity) {
        if (!hasFullSet(entity)) {
            return false;
        }
        if (entity instanceof Player player && player.isCreative()) {
            return false;
        }
        boolean drained = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = entity.getItemBySlot(slot);
            if (discharge(piece, DRAIN_PER_TICK) > 0L) {
                drained = true;
            }
        }
        return drained;
    }

    private static List<ItemStack> stacksOf(LivingEntity entity) {
        List<ItemStack> out = new ArrayList<>(ARMOR_SLOTS.length);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            out.add(entity.getItemBySlot(slot));
        }
        return out;
    }

    static long clampCharge(long value) {
        if (value < 0L) {
            return 0L;
        }
        return Math.min(value, MAX_CHARGE);
    }

    /**
     * The bar colour for a charge ratio: the vanilla durability-bar hue, so a full piece is green, half a
     * piece is yellow and an empty one is red.
     *
     * <p>HBM never picked a colour for the bar it repurposed - it left {@code getDurabilityForDisplay} alone
     * and let 1.7.10 draw the usual green-to-red ramp. 1.20.1 asks for the colour explicitly through
     * {@code Item#getBarColor}, whose vanilla body is the same {@code hsvToRgb(ratio / 3, 1, 1)} formula
     * used here.</p>
     */
    public static int barColor(float ratio) {
        float clamped = Math.max(0.0F, Math.min(1.0F, ratio));
        return Mth.hsvToRgb(clamped / 3.0F, 1.0F, 1.0F);
    }

    /**
     * HBM's {@code BobMathUtil#getShortNumber}: raw integer below 1000, otherwise a SI-suffixed double
     * (two decimals, one above 100 in magnitude) - so {@code 1_000_000} reads {@code 1.0M}.
     */
    public static String shortNumber(long value) {
        long abs = Math.abs(value);
        if (abs < 1_000L) {
            return Long.toString(value);
        }
        String suffix;
        double scale;
        if (abs >= 1_000_000_000_000_000_000L) {
            suffix = "E";
            scale = 1.0E18;
        } else if (abs >= 1_000_000_000_000_000L) {
            suffix = "P";
            scale = 1.0E15;
        } else if (abs >= 1_000_000_000_000L) {
            suffix = "T";
            scale = 1.0E12;
        } else if (abs >= 1_000_000_000L) {
            suffix = "G";
            scale = 1.0E9;
        } else if (abs >= 1_000_000L) {
            suffix = "M";
            scale = 1.0E6;
        } else {
            suffix = "k";
            scale = 1_000.0;
        }
        double scaled = (double) value / scale;
        scaled = scaled <= -100.0 ? Math.round(scaled * 10.0) / 10.0 : Math.round(scaled * 100.0) / 100.0;
        return scaled + suffix;
    }
}
