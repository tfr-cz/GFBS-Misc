package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.lytharalab.gfbs.doors.content.armor.resistance.ArmorResistances;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * HBM's Euphemium suit - "Ep" in Chinese, the pink endgame set.
 *
 * <p>Unlike the T-51b and the hazmat suit this one is drawn by the vanilla armour layer: NTM's
 * {@code euphemium_1.png} / {@code euphemium_2.png} are plain 64x32 armour sheets, so all the item has to do is
 * point the layer at the right one. Helmet, chestplate and boots share layer 1, leggings use layer 2.</p>
 *
 * <p>The rest is NTM's, minus radiation and gas hazards:</p>
 * <ul>
 *   <li>{@code setDamage} does nothing, so the suit never wears out;</li>
 *   <li>a full set nullifies any single hit below a million damage
 *       ({@code DamageResistanceHandler: setOther(1000000.0F, 1.0F)}), and the cancelled hit also plays the
 *       "break" tink HBM's {@code ModEventHandler#onEntityAttacked} played;</li>
 *   <li>while worn the set keeps regeneration, resistance, fire resistance and saturation topped up and caps
 *       fall speed - that lives in {@link EuphemiumSetBonus};</li>
 *   <li>it is epic, unstackable and absent from the creative tab, so {@code /give} is the only way in.</li>
 * </ul>
 */
public final class EuphemiumArmorItem extends ArmorItem {
    private static final String TEXTURE_ROOT = "gfbs_doors:textures/armor/";

    /**
     * NTM marks every piece {@code EnumRarity.epic} and {@code maxStackSize 1}; putting both here means no
     * registration site can forget them.
     */
    public EuphemiumArmorItem(ArmorItem.Type type) {
        super(EuphemiumArmorMaterial.INSTANCE, type, new Properties().rarity(Rarity.EPIC).stacksTo(1));
    }

    /** The full-set entry: absorb up to a million per hit, and everything past that is still nullified. */
    public static void registerResistances(Supplier<Item> helmet, Supplier<Item> chest,
                                           Supplier<Item> legs, Supplier<Item> boots) {
        ArmorResistances.registerSet(helmet.get(), chest.get(), legs.get(), boots.get(),
            EuphemiumArmorMaterial.resistanceTable());
    }

    /** NTM's {@code ArmorEuphemium#setDamage}: empty on purpose, the suit cannot take durability damage. */
    @Override
    public void setDamage(ItemStack stack, int damage) {
    }

    /**
     * HBM picked the sheet per item, not per material; the vanilla layer would otherwise ask for one
     * {@code <material>_layer_N.png} for all four.
     */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return TEXTURE_ROOT + (this.getType() == ArmorItem.Type.LEGGINGS ? "euphemium_2.png" : "euphemium_1.png");
    }

    /** HBM's {@code ArmorUtil#checkArmor}: all four slots hold this suit. */
    public static boolean isCompleteSet(LivingEntity entity) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof EuphemiumArmorItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The tink HBM played when the suit ate a hit ({@code HbmPlayerProps#plink(player, "random.break", 0.5F,
     * 1.0F + random * 0.5F)}), audible to the wearer only.
     */
    public static void playAbsorbSound(Player player) {
        if (!isCompleteSet(player)) {
            return;
        }
        player.playNotifySound(SoundEvents.ITEM_BREAK, SoundSource.PLAYERS,
            0.5F, 1.0F + player.getRandom().nextFloat() * 0.5F);
    }
}
