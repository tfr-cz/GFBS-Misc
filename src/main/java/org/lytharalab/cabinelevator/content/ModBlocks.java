package org.lytharalab.cabinelevator.content;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.lytharalab.cabinelevator.CabinElevatorMod;
import org.lytharalab.cabinelevator.content.block.ElevatorCallerBlock;
import org.lytharalab.cabinelevator.content.block.ElevatorControllerBlock;
import org.lytharalab.cabinelevator.content.block.RedstoneInterfaceBlock;

public final class ModBlocks {
    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK, CabinElevatorMod.MODID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.ITEM, CabinElevatorMod.MODID);

    public static final RegistryObject<ElevatorControllerBlock> CONTROLLER = BLOCKS.register("elevator_controller",
            () -> new ElevatorControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(4.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    public static final RegistryObject<ElevatorCallerBlock> CALLER = BLOCKS.register("elevator_caller",
            () -> new ElevatorCallerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));
    public static final RegistryObject<RedstoneInterfaceBlock> REDSTONE_INTERFACE = BLOCKS.register("redstone_interface",
            () -> new RedstoneInterfaceBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.5F, 6.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.METAL)));

    public static final RegistryObject<Item> CONTROLLER_ITEM = ITEMS.register("elevator_controller",
            () -> new BlockItem(CONTROLLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> CALLER_ITEM = ITEMS.register("elevator_caller",
            () -> new BlockItem(CALLER.get(), new Item.Properties()));
    public static final RegistryObject<Item> REDSTONE_INTERFACE_ITEM = ITEMS.register("redstone_interface",
            () -> new BlockItem(REDSTONE_INTERFACE.get(), new Item.Properties()));

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }
}
