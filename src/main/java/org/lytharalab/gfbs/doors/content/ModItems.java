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
import org.lytharalab.gfbs.doors.content.block.DoorConfiguratorItem;
import org.lytharalab.gfbs.doors.content.block.DoorItem;

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
    }
}
