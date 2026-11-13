package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.EnumMap;
import java.util.Map;

/**
 * Lazily baked, shared model instance per armor slot.
 *
 * <p>One instance per slot (not per player) is safe because Forge copies the animated player pose and
 * the per-slot part visibility into the returned model on every render (see
 * {@code ForgeHooksClient#copyModelProperties}). This is the 1.20.1 equivalent of HBM keeping one
 * {@code ModelArmorXxx} per set and copying the biped pose in {@code setRotationAngles}.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class HazmatArmorModels {
    private static final Map<EquipmentSlot, HumanoidModel<LivingEntity>> BAKED = new EnumMap<>(EquipmentSlot.class);

    private HazmatArmorModels() {
    }

    public static HumanoidModel<LivingEntity> get(EquipmentSlot slot) {
        HumanoidModel<LivingEntity> cached = BAKED.get(slot);
        if (cached != null) {
            return cached;
        }
        HumanoidModel<LivingEntity> built = bake(slot);
        BAKED.put(slot, built);
        return built;
    }

    private static HumanoidModel<LivingEntity> bake(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> HazmatArmorModel.of(HazmatArmorModel.helmet());
            case CHEST -> HazmatArmorModel.of(HazmatArmorModel.chest());
            case LEGS -> HazmatArmorModel.of(HazmatArmorModel.legs());
            case FEET -> HazmatArmorModel.of(HazmatArmorModel.boots());
            default -> HazmatArmorModel.of(HazmatArmorModel.chest());
        };
    }
}
