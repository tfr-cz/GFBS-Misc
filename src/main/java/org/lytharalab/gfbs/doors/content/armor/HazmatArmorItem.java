package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.lytharalab.gfbs.doors.client.armor.HazmatArmorModels;

import java.util.function.Consumer;
import javax.annotation.Nullable;

/**
 * One piece of the protective suit.
 *
 * <p>Rendering follows HBM's power-armor approach, translated to 1.20.1:</p>
 * <ul>
 *   <li>each slot owns its own model instance ({@link HazmatArmorModels}) instead of one shared
 *       biped with a 64x32 sheet, so the boots can be chunkier than the leggings;</li>
 *   <li>the pose is not recomputed - Forge copies the already animated player model into ours
 *       ({@code IClientItemExtensions#getGenericArmorModel}), which is HBM's
 *       {@code copyPropertiesFromBiped} trick;</li>
 *   <li>every slot binds its own texture through {@link #getArmorTexture}, like HBM's
 *       per-piece helmet/chest/arm/leg textures.</li>
 * </ul>
 */
public final class HazmatArmorItem extends ArmorItem {
    private static final String TEXTURE_ROOT = "gfbs_doors:textures/armor/";

    public HazmatArmorItem(ArmorItem.Type type, Properties properties) {
        super(HazmatArmorMaterial.INSTANCE, type, properties);
    }

    @Override
    public int getEnchantmentValue() {
        return HazmatArmorMaterial.INSTANCE.getEnchantmentValue();
    }

    /** HBM-style per-piece texture; the vanilla layer would use one shared sheet per material. */
    @Nullable
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return TEXTURE_ROOT + "hazmat_" + textureName(this.getType()) + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public net.minecraft.client.model.HumanoidModel<?> getHumanoidArmorModel(
                LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot,
                net.minecraft.client.model.HumanoidModel<?> original) {
                // Forge copies pose and part visibility from `original` into the returned model.
                return HazmatArmorModels.get(equipmentSlot);
            }
        });
    }

    private static String textureName(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chest";
            case LEGGINGS -> "legs";
            case BOOTS -> "boots";
            default -> "chest";
        };
    }
}
