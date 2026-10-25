package org.lytharalab.gfbs.doors.content;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.gfbs.doors.GFBSDoors;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorItem;
import org.lytharalab.gfbs.doors.content.block.DoorConfiguratorItem;
import org.lytharalab.gfbs.doors.content.block.DoorItem;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, GFBSDoors.MODID);

    public static final Map<String, RegistryObject<Item>> DOOR_ITEMS = new LinkedHashMap<>();
    public static final RegistryObject<Item> CARGO_ELEVATOR_ITEM;
    public static final RegistryObject<Item> DOOR_CONFIGURATOR;

    static {
        for (DoorSpec spec : DoorSpecs.ALL) {
            DOOR_ITEMS.put(spec.id, ITEMS.register(spec.id,
                    () -> new DoorItem(ModBlocks.door(spec.id).get(), new Item.Properties(), spec)));
        }
        CARGO_ELEVATOR_ITEM = ITEMS.register("cargo_elevator",
                () -> new CargoElevatorItem(ModBlocks.CARGO_ELEVATOR.get(), new Item.Properties()));
        DOOR_CONFIGURATOR = ITEMS.register("door_configurator",
                () -> new DoorConfiguratorItem(new Item.Properties().stacksTo(1)));
    }

    private ModItems() {
    }

    public static RegistryObject<Item> door(String id) {
        return DOOR_ITEMS.get(id);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
