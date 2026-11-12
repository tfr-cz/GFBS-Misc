package org.lytharalab.gfbs.doors.content.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.MapColor;

public class ToneDoorFrameBlock extends Block {
    public ToneDoorFrameBlock() {
        super(Properties.of().mapColor(MapColor.METAL).strength(5.0F, 50.0F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }
}
