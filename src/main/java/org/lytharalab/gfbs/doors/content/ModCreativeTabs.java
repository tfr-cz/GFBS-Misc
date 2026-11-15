package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gfbs_doors");
    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.gfbs_doors.main"))
                .icon(() -> new ItemStack((ItemLike)ModItems.door("vault_door").get()))
                .displayItems((parameters, output) -> {
                    for (DoorSpec spec : DoorSpecs.ALL) {
                        output.accept((ItemLike)ModItems.door(spec.id).get());
                    }

                    output.accept((ItemLike)ModItems.DOOR_FRAME_ITEM.get());
                    output.accept((ItemLike)ModItems.DOOR_FRAME_TONE_ITEM.get());
                    output.accept((ItemLike)ModItems.DOOR_CONFIGURATOR.get());
                    output.accept((ItemLike)ModItems.HAZMAT_HELMET.get());
                    output.accept((ItemLike)ModItems.HAZMAT_CHEST.get());
                    output.accept((ItemLike)ModItems.HAZMAT_LEGS.get());
                    output.accept((ItemLike)ModItems.HAZMAT_BOOTS.get());
                    output.accept((ItemLike)ModItems.POWER_HELMET.get());
                    output.accept((ItemLike)ModItems.POWER_CHEST.get());
                    output.accept((ItemLike)ModItems.POWER_LEGS.get());
                    output.accept((ItemLike)ModItems.POWER_BOOTS.get());
                    output.accept((ItemLike)ModItems.FUSION_CORE.get());
                    output.accept((ItemLike)ModItems.CRUCIBLE.get());
                    // The Euphemium set is missing on purpose: NTM gives it no creative tab either, so it stays a
                    // /give-only item. Nothing else adds items implicitly, so "not listed here" is what hides it.
                })
                .build()
    );

    private ModCreativeTabs() {
    }

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
