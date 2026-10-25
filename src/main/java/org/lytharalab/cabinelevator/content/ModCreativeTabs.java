package org.lytharalab.cabinelevator.content;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.cabinelevator.CabinElevatorMod;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, CabinElevatorMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.cabin_elevator"))
            .icon(() -> new ItemStack(ModBlocks.CONTROLLER_ITEM.get()))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.CONTROLLER_ITEM.get());
                output.accept(ModBlocks.CALLER_ITEM.get());
                output.accept(ModBlocks.REDSTONE_INTERFACE_ITEM.get());
                output.accept(ModItems.LINKER.get());
                output.accept(ModItems.RANGE_SELECTOR.get());
            })
            .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
