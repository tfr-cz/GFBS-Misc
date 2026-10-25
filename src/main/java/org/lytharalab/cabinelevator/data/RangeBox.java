package org.lytharalab.cabinelevator.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;

public record RangeBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    public static final int MAX_OFFSET = 64;
    public static final int MAX_SIZE = 64;
    public static final RangeBox DEFAULT = new RangeBox(-2, 0, -2, 3, 4, 3);

    public int width() {
        return maxX - minX;
    }

    public int height() {
        return maxY - minY;
    }

    public int depth() {
        return maxZ - minZ;
    }

    public boolean isValid() {
        if (minX < -MAX_OFFSET || maxX > MAX_OFFSET || minY < -MAX_OFFSET || maxY > MAX_OFFSET
                || minZ < -MAX_OFFSET || maxZ > MAX_OFFSET) {
            return false;
        }
        return width() >= 1 && width() <= MAX_SIZE
                && height() >= 1 && height() <= MAX_SIZE
                && depth() >= 1 && depth() <= MAX_SIZE;
    }

    public AABB aabb(BlockPos controller) {
        return new AABB(controller.getX() + minX, controller.getY() + minY, controller.getZ() + minZ,
                controller.getX() + maxX, controller.getY() + maxY, controller.getZ() + maxZ);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("minX", minX);
        tag.putInt("minY", minY);
        tag.putInt("minZ", minZ);
        tag.putInt("maxX", maxX);
        tag.putInt("maxY", maxY);
        tag.putInt("maxZ", maxZ);
        return tag;
    }

    public static RangeBox load(CompoundTag tag) {
        RangeBox range = new RangeBox(tag.getInt("minX"), tag.getInt("minY"), tag.getInt("minZ"),
                tag.getInt("maxX"), tag.getInt("maxY"), tag.getInt("maxZ"));
        return range.isValid() ? range : DEFAULT;
    }

    public static RangeBox legacy(int width, int height, int depth) {
        int minX = -(Math.max(1, width) / 2);
        int minZ = -(Math.max(1, depth) / 2);
        RangeBox range = new RangeBox(minX, 0, minZ, minX + Math.max(1, width), Math.max(1, height),
                minZ + Math.max(1, depth));
        return range.isValid() ? range : DEFAULT;
    }
}
