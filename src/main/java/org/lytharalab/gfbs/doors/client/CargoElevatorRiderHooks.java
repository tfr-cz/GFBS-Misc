package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A rising collision surface cannot push entities up in vanilla (collision only blocks
 * movement), so the local player is carried onto the platform surface each tick while
 * standing inside the swept band. The local player is client-authoritative, therefore
 * this stays on the client side.
 */
@OnlyIn(Dist.CLIENT)
public final class CargoElevatorRiderHooks {
    private CargoElevatorRiderHooks() {
    }

    public static void carryLocalPlayer(org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity elevator) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || player.isSpectator()) return;
        if (elevator.isRemoved() || elevator.getLevel() != minecraft.level) return;
        BlockPos core = elevator.getBlockPos();
        double platformY = core.getY() + 1 + elevator.getExtension();

        double window = 0.75D;
        double feet = player.getY();
        if (feet < platformY - window || feet > platformY + window) return;
        if (Math.abs(player.getX() - (core.getX() + 0.5D)) > 1.6D) return;
        if (Math.abs(player.getZ() - (core.getZ() + 0.5D)) > 1.6D) return;

        double delta = platformY - feet;
        if (Math.abs(delta) > 1.0E-4D) {
            player.setPos(player.getX(), player.getY() + delta, player.getZ());
        }
        player.setDeltaMovement(player.getDeltaMovement().x, 0.0D, player.getDeltaMovement().z);
        player.setOnGround(true);
        player.resetFallDistance();
    }
}
