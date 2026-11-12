package org.lytharalab.gfbs.doors.content;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;
import org.lytharalab.gfbs.doors.content.block.DoorBlock;
import org.lytharalab.gfbs.doors.content.block.DoorFrameBlock;
import org.lytharalab.gfbs.doors.content.block.ToneDoorFrameBlock;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "gfbs_doors");
    public static final Map<String, RegistryObject<Block>> DOORS = new LinkedHashMap<>();
    public static final RegistryObject<Block> DOOR_FRAME;
    public static final RegistryObject<Block> DOOR_FRAME_TONE;

    private ModBlocks() {
    }

    public static RegistryObject<Block> door(String id) {
        return (RegistryObject)DOORS.get(id);
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }

    static {
        for (DoorSpec spec : DoorSpecs.ALL) {
            DOORS.put(spec.id, BLOCKS.register(spec.id, () -> new DoorBlock(spec)));
        }

        DOOR_FRAME = BLOCKS.register("door_frame", DoorFrameBlock::new);
        DOOR_FRAME_TONE = BLOCKS.register("door_frame_tone", ToneDoorFrameBlock::new);
    }
}
