package org.lytharalab.cabinelevator.content.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import org.lytharalab.cabinelevator.content.ModBlockEntities;

public class ElevatorControllerBlockEntity extends BlockEntity {
    public ElevatorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROLLER.get(), pos, state);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return DistExecutor.unsafeRunForDist(
                () -> () -> org.lytharalab.cabinelevator.client.RangeOverlay.renderBounds(worldPosition),
                () -> () -> new AABB(worldPosition));
    }
}
