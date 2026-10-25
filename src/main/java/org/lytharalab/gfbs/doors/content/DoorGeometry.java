package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class DoorGeometry {
    public static final AABB FULL = new AABB(0, 0, 0, 1, 1, 1);
    public static final AABB EMPTY = new AABB(0, 0, 0, 0, 0, 0);

    private DoorGeometry() {
    }

    public static int[] rotateDims(int[] dim, Direction dir) {
        return switch (dir) {
            case NORTH -> new int[]{dim[0], dim[1], dim[3], dim[2], dim[5], dim[4]};
            case EAST -> new int[]{dim[0], dim[1], dim[5], dim[4], dim[2], dim[3]};
            case WEST -> new int[]{dim[0], dim[1], dim[4], dim[5], dim[3], dim[2]};
            default -> dim;
        };
    }

    /** Mirrors Rotation.getBlockRotation(dir).add(COUNTERCLOCKWISE_90). */
    private static int localRotation(Direction dir) {
        return switch (dir) {
            case NORTH -> 3;
            case SOUTH -> 1;
            case EAST -> 2;
            default -> 0;
        };
    }

    private static int[] rotateLocal(int x, int y, int z, int rotation) {
        return switch (rotation) {
            case 1 -> new int[]{-z, y, x};
            case 2 -> new int[]{-x, y, -z};
            case 3 -> new int[]{z, y, -x};
            default -> new int[]{x, y, z};
        };
    }

    public static AABB boundFor(DoorSpec spec, int x, int y, int z, boolean open, boolean collision) {
        return switch (spec.id) {
            case "vault_door" -> {
                if (!open || y == 0 || y == 4) yield FULL;
                if (Math.abs(z) == 2) yield FULL;
                yield EMPTY;
            }
            case "fire_door" -> {
                if (!open) yield FULL;
                if (z == 1) yield new AABB(0.5, 0, 0, 1, 1, 1);
                if (z == -2) yield new AABB(0, 0, 0, 0.5, 1, 1);
                if (y > 1) yield new AABB(0, 0.75, 0, 1, 1, 1);
                if (y == 0) yield new AABB(0, 0, 0, 1, collision ? 0 : 0.1, 1);
                yield EMPTY;
            }
            case "sliding_blast_door" -> {
                if (!open) yield FULL;
                if (y == 0) yield new AABB(0, 0, 0, 1, collision ? 0 : 0.08, 1);
                if (Math.abs(x) == 3 || Math.abs(z) == 3) yield FULL;
                if (y == 3) yield new AABB(0, 0.5, 0, 1, 1, 1);
                yield EMPTY;
            }
            case "sliding_seal_door" -> (collision && open) ? EMPTY : new AABB(0, 0, 0.75, 1, 1, 1);
            case "secure_access_door" -> {
                if (!open) {
                    yield y > 0 ? new AABB(0, 0, 0.375, 1, 1, 0.625) : FULL;
                }
                if (y == 1) yield new AABB(0, 0, 0, 1, collision ? 0 : 0.0625, 1);
                if (y == 4) yield new AABB(0, 0.5, 0.15, 1, 1, 0.85);
                if (y == 0) yield FULL;
                yield EMPTY;
            }
            case "round_airlock_door" -> {
                if (!open) yield FULL;
                if (z == 1) yield new AABB(0.4, 0, 0, 1, 1, 1);
                if (z == -2) yield new AABB(0, 0, 0, 0.6, 1, 1);
                if (y == 3) yield new AABB(0, 0.5, 0, 1, 1, 1);
                if (y == 0) yield new AABB(0, 0, 0, 1, collision ? 0 : 0.0625, 1);
                yield EMPTY;
            }
            case "qe_sliding_door" -> {
                if (collision && open) {
                    if (z == 0) yield new AABB(0.875, 0, 0.8125, 1, 1, 1);
                    yield new AABB(0, 0, 0.8125, 0.125, 1, 1);
                }
                yield new AABB(0, 0, 0.8125, 1, 1, 1);
            }
            case "qe_containment" -> {
                if (!open) yield new AABB(0, 0, 0.5, 1, 1, 1);
                if (y > 1) yield new AABB(0, 0.25, 0.5, 1, 1, 1);
                if (y == 0) yield new AABB(0, 0, 0.5, 1, collision ? 0 : 0.125, 1);
                yield EMPTY;
            }
            case "water_door" -> {
                if (!open) yield new AABB(0, 0, 0.75, 1, 1, 1);
                if (y > 1) yield new AABB(0, 0.85, 0.75, 1, 1, 1);
                if (y == 0) yield new AABB(0, 0, 0.75, 1, collision ? 0 : 0.15, 1);
                yield EMPTY;
            }
            case "large_vehicle_door" -> {
                if (!open) yield FULL;
                if (z == 3) yield new AABB(0.4, 0, 0, 1, 1, 1);
                if (z == -3) yield new AABB(0, 0, 0, 0.6, 1, 1);
                if (y == 0) yield new AABB(0, 0, 0, 1, collision ? 0 : 0.0625, 1);
                yield EMPTY;
            }
            case "silo_hatch" -> {
                if (!open) yield FULL;
                if (Math.abs(x) == 2 || Math.abs(z) == 2) yield FULL;
                yield EMPTY;
            }
            case "silo_hatch_large" -> {
                if (!open) yield FULL;
                if (Math.abs(x) == 3 || Math.abs(z) == 3) yield FULL;
                yield EMPTY;
            }
            case "blast_door" -> {
                if (!open) yield FULL;
                if (y == 0 || y == 6) yield FULL;
                yield EMPTY;
            }
            case "cargo_door" -> {
                if (!open) yield new AABB(0, 0, 0.375, 1, 1, 0.625);
                if (y > 1) yield new AABB(0, 0.25, 0.375, 1, 1, 0.625);
                if (y == 0) yield new AABB(0, 0, 0.375, 1, collision ? 0 : 0.125, 0.625);
                yield EMPTY;
            }
            case "transition_seal" -> {
                if (!open) yield FULL;
                // Collision follows the smooth-stone facade around the circular opening.
                // The facade spans z=-12..12; its outer columns are vertical half-slabs.
                if (z < -12 || z > 12) yield EMPTY;
                if (z == -12) yield new AABB(0, 0, 0, 1, 1, 0.5);
                if (z == 12) yield new AABB(0, 0, 0.5, 1, 1, 1);
                int hole = switch (y) {
                    case 2, 21 -> 3;
                    case 3, 20 -> 5;
                    case 4, 19 -> 6;
                    case 5, 18 -> 7;
                    case 6, 17 -> 8;
                    case 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 -> 9;
                    default -> -1;
                };
                if (hole >= 0 && Math.abs(z) <= hole) yield EMPTY;
                yield FULL;
            }
            default -> open ? EMPTY : FULL;
        };
    }

    public static AABB worldBox(DoorSpec spec, BlockPos core, BlockPos part, Direction facing, boolean open, boolean collision) {
        int dx = part.getX() - core.getX();
        int dy = part.getY() - core.getY();
        int dz = part.getZ() - core.getZ();
        int[] local = rotateLocal(dx, dy, dz, localRotation(facing));
        AABB b = boundFor(spec, local[0], local[1], local[2], open, collision);

        double x = part.getX();
        double y = part.getY();
        double z = part.getZ();

        double minX;
        double minY = y + Math.min(b.minY, b.maxY);
        double minZ;
        double maxX;
        double maxY = y + Math.max(b.minY, b.maxY);
        double maxZ;

        switch (facing) {
            case NORTH -> {
                minX = x + 1 - Math.max(b.minX, b.maxX);
                maxX = x + 1 - Math.min(b.minX, b.maxX);
                minZ = z + 1 - Math.max(b.minZ, b.maxZ);
                maxZ = z + 1 - Math.min(b.minZ, b.maxZ);
            }
            case WEST -> {
                minX = x + 1 - Math.max(b.minZ, b.maxZ);
                maxX = x + 1 - Math.min(b.minZ, b.maxZ);
                minZ = z + Math.min(b.minX, b.maxX);
                maxZ = z + Math.max(b.minX, b.maxX);
            }
            case EAST -> {
                minX = x + Math.min(b.minZ, b.maxZ);
                maxX = x + Math.max(b.minZ, b.maxZ);
                minZ = z + 1 - Math.max(b.minX, b.maxX);
                maxZ = z + 1 - Math.min(b.minX, b.maxX);
            }
            default -> {
                minX = x + Math.min(b.minX, b.maxX);
                maxX = x + Math.max(b.minX, b.maxX);
                minZ = z + Math.min(b.minZ, b.maxZ);
                maxZ = z + Math.max(b.minZ, b.maxZ);
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean isEmpty(AABB box) {
        return box.minX == box.maxX || box.minY == box.maxY || box.minZ == box.maxZ;
    }
}
