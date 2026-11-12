package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "gfbs_doors");
    public static final RegistryObject<BlockEntityType<DoorBlockEntity>> DOOR = TYPES.register(
        "door",
        () -> Builder.of(DoorBlockEntity::new, (Block[])ModBlocks.DOORS.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null)
    );

    private ModBlockEntities() {
    }

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
