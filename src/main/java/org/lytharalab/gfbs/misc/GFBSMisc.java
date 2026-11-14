package org.lytharalab.gfbs.misc;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lytharalab.gfbs.doors.content.ModBlockEntities;
import org.lytharalab.gfbs.doors.content.ModBlocks;
import org.lytharalab.gfbs.doors.content.ModCreativeTabs;
import org.lytharalab.gfbs.doors.content.ModItems;
import org.lytharalab.gfbs.doors.content.ModSounds;
import org.lytharalab.gfbs.doors.content.armor.PowerArmorItem;
import org.lytharalab.gfbs.misc.network.MiscNetwork;
import org.slf4j.Logger;

@Mod("gfbs_misc")
public final class GFBSMisc {
    public static final String MODID = "gfbs_misc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public GFBSMisc() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.register(bus);
        ModBlocks.register(bus);
        ModBlockEntities.register(bus);
        ModItems.register(bus);
        ModCreativeTabs.register(bus);
        bus.addListener(this::commonSetup);
        registerClassicBarIfPresent();
    }

    /**
     * Classic Bar caches its bar list while loading its config, which is earlier than client setup, so the bar
     * has to be registered here in the constructor. The guard keeps its classes off machines without it.
     */
    private static void registerClassicBarIfPresent() {
        if (!FMLEnvironment.dist.isClient()) {
            return;
        }
        if (!org.lytharalab.gfbs.doors.client.armor.compat.ClassicBarPresence.PRESENT) {
            return;
        }
        try {
            org.lytharalab.gfbs.doors.client.armor.compat.ClassicBarPowerBars.register();
        } catch (Throwable broken) {
            // A Classic Bar API change must not take the whole mod down; the vanilla-layout HUD stays unused.
            LOGGER.warn("Classic Bar present but its bar API rejected the charge bars", broken);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MiscNetwork::register);
        // Registries are populated by now, so the armour resistance tables can be built against real items.
        event.enqueueWork(() -> PowerArmorItem.registerResistances(
            () -> ModItems.POWER_HELMET.get(),
            () -> ModItems.POWER_CHEST.get(),
            () -> ModItems.POWER_LEGS.get(),
            () -> ModItems.POWER_BOOTS.get()));
    }
}
