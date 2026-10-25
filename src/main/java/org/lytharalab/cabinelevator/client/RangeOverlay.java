package org.lytharalab.cabinelevator.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lytharalab.cabinelevator.data.RangeBox;

@OnlyIn(Dist.CLIENT)
public final class RangeOverlay {
    private static volatile boolean enabled;
    @Nullable
    private static ResourceKey<Level> dimension;
    @Nullable
    private static BlockPos controller;
    @Nullable
    private static RangeBox range;

    private RangeOverlay() {
    }

    public static void show(ResourceKey<Level> levelDimension, RangeBox box, BlockPos controllerPos) {
        dimension = levelDimension;
        controller = controllerPos;
        range = box;
        enabled = true;
    }

    public static void show(RangeBox box, BlockPos controllerPos) {
        // refresh while keeping the recorded dimension
        controller = controllerPos;
        range = box;
        enabled = true;
    }

    public static void disable() {
        enabled = false;
        dimension = null;
        controller = null;
        range = null;
    }

    public static boolean isShowing(BlockPos controllerPos) {
        if (!enabled || dimension == null || !controllerPos.equals(controller)) {
            return false;
        }
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension().equals(dimension);
    }

    @Nullable
    public static AABB shownAabb() {
        if (!enabled || controller == null || range == null) {
            return null;
        }
        return range.aabb(controller);
    }

    public static AABB renderBounds(BlockPos pos) {
        AABB box = shownAabb();
        return box != null && pos.equals(controller) ? box.inflate(1.0D) : new AABB(pos);
    }
}
