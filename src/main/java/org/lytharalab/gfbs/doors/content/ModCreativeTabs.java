package org.lytharalab.gfbs.doors.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.gfbs.doors.GFBSDoors;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, GFBSDoors.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.gfbs_doors.main"))
            .icon(() -> new ItemStack(ModItems.door("vault_door").get()))
            .displayItems((parameters, output) -> {
                for (DoorSpec spec : DoorSpecs.ALL) {
                    output.accept(ModItems.door(spec.id).get());
                }
                output.accept(ModItems.CARGO_ELEVATOR_ITEM.get());
                output.accept(ModItems.DOOR_CONFIGURATOR.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
