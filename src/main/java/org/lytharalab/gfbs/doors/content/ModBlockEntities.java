package org.lytharalab.gfbs.doors.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.gfbs.doors.GFBSDoors;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, GFBSDoors.MODID);

    public static final RegistryObject<BlockEntityType<DoorBlockEntity>> DOOR = TYPES.register("door",
            () -> BlockEntityType.Builder.of(DoorBlockEntity::new,
                    ModBlocks.DOORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));

    public static final RegistryObject<BlockEntityType<CargoElevatorBlockEntity>> CARGO_ELEVATOR = TYPES.register("cargo_elevator",
            () -> BlockEntityType.Builder.of(CargoElevatorBlockEntity::new, ModBlocks.CARGO_ELEVATOR.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
