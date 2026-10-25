package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity;

@OnlyIn(Dist.CLIENT)
public final class CargoElevatorClientSync {
    private CargoElevatorClientSync() {
    }

    public static void apply(BlockPos pos, double extension) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof CargoElevatorBlockEntity be) {
            be.applyClientSync(extension);
        }
    }
}
