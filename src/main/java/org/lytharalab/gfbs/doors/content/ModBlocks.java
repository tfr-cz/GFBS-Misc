package org.lytharalab.gfbs.doors.content;

import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.gfbs.doors.GFBSDoors;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlock;
import org.lytharalab.gfbs.doors.content.block.DoorBlock;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, GFBSDoors.MODID);

    public static final Map<String, RegistryObject<Block>> DOORS = new LinkedHashMap<>();
    public static final RegistryObject<Block> CARGO_ELEVATOR;

    static {
        for (DoorSpec spec : DoorSpecs.ALL) {
            DOORS.put(spec.id, BLOCKS.register(spec.id, () -> new DoorBlock(spec)));
        }
        CARGO_ELEVATOR = BLOCKS.register("cargo_elevator", CargoElevatorBlock::new);
    }

    private ModBlocks() {
    }

    public static RegistryObject<Block> door(String id) {
        return DOORS.get(id);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
