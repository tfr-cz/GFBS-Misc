package org.lytharalab.cabinelevator.content;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.cabinelevator.CabinElevatorMod;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlockEntity;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, CabinElevatorMod.MODID);

    public static final RegistryObject<BlockEntityType<ElevatorControllerBlockEntity>> CONTROLLER =
            TYPES.register("controller",
                    () -> BlockEntityType.Builder.of(ElevatorControllerBlockEntity::new,
                            ModBlocks.CONTROLLER.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
