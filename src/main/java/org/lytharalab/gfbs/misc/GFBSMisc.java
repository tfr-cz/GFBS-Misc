package org.lytharalab.gfbs.misc;

import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.lytharalab.cabinelevator.content.ModBlocks;
import org.lytharalab.cabinelevator.content.ModBlockEntities;
import org.lytharalab.cabinelevator.content.ModCreativeTabs;
import org.lytharalab.cabinelevator.content.ModItems;
import org.lytharalab.gfbs.misc.network.MiscNetwork;

/**
 * GFBS : Misc bundles the former GFBS-Doors addon and the standalone cabin
 * elevator. Registered objects keep their original namespaces (gfbs_doors:,
 * cabin_elevator:) so worlds built with either mod keep working.
 */
@Mod(GFBSMisc.MODID)
public final class GFBSMisc {
    public static final String MODID = "gfbs_misc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public GFBSMisc() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        org.lytharalab.gfbs.doors.content.ModSounds.register(bus);
        org.lytharalab.gfbs.doors.content.ModBlocks.register(bus);
        org.lytharalab.gfbs.doors.content.ModBlockEntities.register(bus);
        org.lytharalab.gfbs.doors.content.ModItems.register(bus);
        org.lytharalab.gfbs.doors.content.ModCreativeTabs.register(bus);
        ModBlocks.register(bus);
        ModBlockEntities.register(bus);
        ModItems.register(bus);
        ModCreativeTabs.register(bus);
        bus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(MiscNetwork::register);
    }
}
