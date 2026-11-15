package org.lytharalab.gfbs.doors.content;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import org.lytharalab.gfbs.doors.content.armor.HazmatArmorItem;
import org.lytharalab.gfbs.doors.content.armor.EuphemiumArmorItem;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;
import org.lytharalab.gfbs.doors.content.armor.FusionCoreItem;
import org.lytharalab.gfbs.doors.content.block.DoorConfiguratorItem;
import org.lytharalab.gfbs.doors.content.block.DoorItem;
import org.lytharalab.gfbs.doors.content.weapon.CrucibleItem;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gfbs_doors");
    public static final Map<String, RegistryObject<Item>> DOOR_ITEMS = new LinkedHashMap<>();
    public static final RegistryObject<Item> DOOR_FRAME_ITEM;
    public static final RegistryObject<Item> DOOR_FRAME_TONE_ITEM;
    public static final RegistryObject<Item> DOOR_CONFIGURATOR;
    public static final RegistryObject<Item> HAZMAT_HELMET;
    public static final RegistryObject<Item> HAZMAT_CHEST;
    public static final RegistryObject<Item> HAZMAT_LEGS;
    public static final RegistryObject<Item> HAZMAT_BOOTS;
    public static final RegistryObject<Item> POWER_HELMET;
    public static final RegistryObject<Item> POWER_CHEST;
    public static final RegistryObject<Item> POWER_LEGS;
    public static final RegistryObject<Item> POWER_BOOTS;
    public static final RegistryObject<Item> EUPHEMIUM_HELMET;
    public static final RegistryObject<Item> EUPHEMIUM_CHEST;
    public static final RegistryObject<Item> EUPHEMIUM_LEGS;
    public static final RegistryObject<Item> EUPHEMIUM_BOOTS;
    public static final RegistryObject<Item> FUSION_CORE;
    public static final RegistryObject<Item> CRUCIBLE;

    private ModItems() {
    }

    public static RegistryObject<Item> door(String id) {
        return (RegistryObject)DOOR_ITEMS.get(id);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    static {
        for (DoorSpec spec : DoorSpecs.ALL) {
            DOOR_ITEMS.put(spec.id, ITEMS.register(spec.id, () -> new DoorItem((Block)ModBlocks.door(spec.id).get(), new Properties(), spec)));
        }

        DOOR_FRAME_ITEM = ITEMS.register("door_frame", () -> new BlockItem((Block)ModBlocks.DOOR_FRAME.get(), new Properties()));
        DOOR_FRAME_TONE_ITEM = ITEMS.register("door_frame_tone", () -> new BlockItem((Block)ModBlocks.DOOR_FRAME_TONE.get(), new Properties()));
        DOOR_CONFIGURATOR = ITEMS.register("door_configurator", () -> new DoorConfiguratorItem(new Properties().stacksTo(1)));

        // Four independent armor pieces, rendered HBM-style with one model + one texture per slot.
        HAZMAT_HELMET = ITEMS.register("hazmat_helmet", () -> new HazmatArmorItem(ArmorItem.Type.HELMET, new Properties()));
        HAZMAT_CHEST = ITEMS.register("hazmat_chest", () -> new HazmatArmorItem(ArmorItem.Type.CHESTPLATE, new Properties()));
        HAZMAT_LEGS = ITEMS.register("hazmat_legs", () -> new HazmatArmorItem(ArmorItem.Type.LEGGINGS, new Properties()));
        HAZMAT_BOOTS = ITEMS.register("hazmat_boots", () -> new HazmatArmorItem(ArmorItem.Type.BOOTS, new Properties()));

        // HBM's T-51b power armour: same four-slot split, but rendered from an imported mesh
        // (PowerArmorLayer) instead of boxes, so the vanilla armour model stays empty.
        POWER_HELMET = ITEMS.register("power_helmet", () -> new PowerArmorItem(ArmorItem.Type.HELMET, new Properties()));
        POWER_CHEST = ITEMS.register("power_chest", () -> new PowerArmorItem(ArmorItem.Type.CHESTPLATE, new Properties()));
        POWER_LEGS = ITEMS.register("power_legs", () -> new PowerArmorItem(ArmorItem.Type.LEGGINGS, new Properties()));
        POWER_BOOTS = ITEMS.register("power_boots", () -> new PowerArmorItem(ArmorItem.Type.BOOTS, new Properties()));

        // HBM's Euphemium ("Ep") suit. Vanilla-rendered, so no models of our own, and deliberately kept out of
        // the creative tab: NTM passes setCreativeTab(null), which makes /give the only way to obtain it.
        EUPHEMIUM_HELMET = ITEMS.register("euphemium_helmet", () -> new EuphemiumArmorItem(ArmorItem.Type.HELMET));
        EUPHEMIUM_CHEST = ITEMS.register("euphemium_chest", () -> new EuphemiumArmorItem(ArmorItem.Type.CHESTPLATE));
        EUPHEMIUM_LEGS = ITEMS.register("euphemium_legs", () -> new EuphemiumArmorItem(ArmorItem.Type.LEGGINGS));
        EUPHEMIUM_BOOTS = ITEMS.register("euphemium_boots", () -> new EuphemiumArmorItem(ArmorItem.Type.BOOTS));

        FUSION_CORE = ITEMS.register("fusion_core", () -> new FusionCoreItem(new Properties()));
        // HBM's Crucible, 30 000 damage and a held speed bonus, drawn from its own OBJ mesh.
        CRUCIBLE = ITEMS.register("crucible", CrucibleItem::new);
    }
}
