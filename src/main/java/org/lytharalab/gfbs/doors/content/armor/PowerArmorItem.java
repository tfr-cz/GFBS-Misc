package org.lytharalab.gfbs.doors.content.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.lytharalab.gfbs.doors.client.armor.EmptyArmorModel;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * One piece of HBM's T-51b power armour - appearance only, no power, no battery, no HUD.
 *
 * <p>These pieces are not drawn by the vanilla armour layer: {@code PowerArmorLayer} renders the
 * imported OBJ instead, because that layer can only draw a {@link HumanoidModel} made of boxes and the
 * imported mesh is far too detailed for that. Handing the layer an empty humanoid keeps it from
 * drawing anything at all, so there is no double image and no vanilla armour texture in the way.</p>
 */
public final class PowerArmorItem extends ArmorItem {
    public PowerArmorItem(ArmorItem.Type type, Properties properties) {
        super(HazmatArmorMaterial.INSTANCE, type, properties);
    }

    /**
     * Never drawn: the vanilla layer renders {@code EmptyArmorModel} for these pieces. It still resolves a
     * texture though, so point it at one that exists instead of letting it ask for a
     * {@code textures/models/armor/<material>_layer_1.png} sheet that was never shipped.
     */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, @Nullable String type) {
        return "gfbs_doors:textures/armor/hazmat_chest.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack,
                                                          EquipmentSlot equipmentSlot,
                                                          HumanoidModel<?> original) {
                return EmptyArmorModel.get();
            }
        });
    }
}
