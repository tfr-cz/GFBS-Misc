package org.lytharalab.cabinelevator.content;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.cabinelevator.CabinElevatorMod;
import org.lytharalab.cabinelevator.content.item.LinkerItem;
import org.lytharalab.cabinelevator.content.item.RangeSelectorItem;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, CabinElevatorMod.MODID);

    public static final RegistryObject<LinkerItem> LINKER = ITEMS.register("linker",
            () -> new LinkerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<RangeSelectorItem> RANGE_SELECTOR = ITEMS.register("range_selector",
            () -> new RangeSelectorItem(new Item.Properties().stacksTo(1)));

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
