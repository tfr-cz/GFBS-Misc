package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class DoorGeometry {
    public static final AABB FULL = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    public static final AABB EMPTY = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

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

    private static int localRotation(Direction dir) {
        return switch (dir) {
            case NORTH -> 3;
            case EAST -> 2;
            default -> 0;
            case SOUTH -> 1;
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
        return boundFor(spec, x, y, z, open, collision, 0);
    }

    public static AABB boundFor(DoorSpec spec, int x, int y, int z, boolean open, boolean collision, int extraHeight) {
        DoorSpec base = spec.baseId == null ? null : (DoorSpec)DoorSpecs.BY_ID.get(spec.baseId);
        return base != null ? scaledBoundFor(spec, base, x, y, z, open, collision) : baseBoundFor(spec, x, y, z, open, collision, extraHeight);
    }

    private static AABB baseBoundFor(DoorSpec spec, int x, int y, int z, boolean open, boolean collision) {
        return baseBoundFor(spec, x, y, z, open, collision, 0);
    }

    private static AABB baseBoundFor(DoorSpec spec, int x, int y, int z, boolean open, boolean collision, int extraHeight) {
        String var7 = spec.id;

        return switch (var7) {
            case "vault_door" -> open && y != 0 && y != 4 ? (Math.abs(z) == 2 ? FULL : EMPTY) : FULL;
            case "fire_door" -> !open
            ? FULL
            : (
                z == 1
                    ? new AABB(0.5, 0.0, 0.0, 1.0, 1.0, 1.0)
                    : (
                        z == -2
                            ? new AABB(0.0, 0.0, 0.0, 0.5, 1.0, 1.0)
                            : (y > 1 ? new AABB(0.0, 0.75, 0.0, 1.0, 1.0, 1.0) : (y == 0 ? new AABB(0.0, 0.0, 0.0, 1.0, collision ? 0.0 : 0.1, 1.0) : EMPTY))
                    )
            );
            case "sliding_blast_door" -> !open
            ? FULL
            : (
                y == 0
                    ? new AABB(0.0, 0.0, 0.0, 1.0, collision ? 0.0 : 0.08, 1.0)
                    : (Math.abs(x) != 3 && Math.abs(z) != 3 ? (y == 3 ? new AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0) : EMPTY) : FULL)
            );
            case "sliding_seal_door" -> collision && open ? EMPTY : new AABB(0.0, 0.0, 0.75, 1.0, 1.0, 1.0);
            case "secure_access_door" -> !open
            ? (y > 0 ? new AABB(0.0, 0.0, 0.375, 1.0, 1.0, 0.625) : FULL)
            : (
                y == 1
                    ? new AABB(0.0, 0.0, 0.0, 1.0, collision ? 0.0 : 0.0625, 1.0)
                    : (y == 4 ? new AABB(0.0, 0.5, 0.15, 1.0, 1.0, 0.85) : (y == 0 ? FULL : EMPTY))
            );
            case "round_airlock_door" -> !open
            ? FULL
            : (
                z == 1
                    ? new AABB(0.4, 0.0, 0.0, 1.0, 1.0, 1.0)
                    : (
                        z == -2
                            ? new AABB(0.0, 0.0, 0.0, 0.6, 1.0, 1.0)
                            : (y == 3 ? new AABB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0) : (y == 0 ? new AABB(0.0, 0.0, 0.0, 1.0, collision ? 0.0 : 0.0625, 1.0) : EMPTY))
                    )
            );
            case "qe_sliding_door" -> collision && open
            ? (z == 0 ? new AABB(0.875, 0.0, 0.8125, 1.0, 1.0, 1.0) : new AABB(0.0, 0.0, 0.8125, 0.125, 1.0, 1.0))
            : new AABB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0);
            case "qe_containment" -> !open
            ? new AABB(0.0, 0.0, 0.5, 1.0, 1.0, 1.0)
            : (y > 1 ? new AABB(0.0, 0.25, 0.5, 1.0, 1.0, 1.0) : (y == 0 ? new AABB(0.0, 0.0, 0.5, 1.0, collision ? 0.0 : 0.125, 1.0) : EMPTY));
            case "water_door" -> !open
            ? new AABB(0.0, 0.0, 0.75, 1.0, 1.0, 1.0)
            : (y > 1 ? new AABB(0.0, 0.85, 0.75, 1.0, 1.0, 1.0) : (y == 0 ? new AABB(0.0, 0.0, 0.75, 1.0, collision ? 0.0 : 0.15, 1.0) : EMPTY));
            case "large_vehicle_door" -> !open
            ? FULL
            : (
                z == 3
                    ? new AABB(0.4, 0.0, 0.0, 1.0, 1.0, 1.0)
                    : (z == -3 ? new AABB(0.0, 0.0, 0.0, 0.6, 1.0, 1.0) : (y == 0 ? new AABB(0.0, 0.0, 0.0, 1.0, collision ? 0.0 : 0.0625, 1.0) : EMPTY))
            );
            case "silo_hatch" -> !open ? FULL : (Math.abs(x) != 2 && Math.abs(z) != 2 ? EMPTY : FULL);
            case "silo_hatch_large" -> !open ? FULL : (Math.abs(x) != 3 && Math.abs(z) != 3 ? EMPTY : FULL);
            case "blast_door" -> !open ? FULL : (y != 0 && y != 6 + Math.max(0, extraHeight) ? EMPTY : FULL);
            case "reactor_door" -> !open ? FULL : (y == 2 ? FULL : (z != -1 && z != 1 ? EMPTY : FULL));
            case "cargo_door" -> !open
            ? new AABB(0.0, 0.0, 0.375, 1.0, 1.0, 0.625)
            : (y > 1 ? new AABB(0.0, 0.25, 0.375, 1.0, 1.0, 0.625) : (y == 0 ? new AABB(0.0, 0.0, 0.375, 1.0, collision ? 0.0 : 0.125, 0.625) : EMPTY));
            case "transition_seal" -> {
                if (!open) {
                    yield FULL;
                } else if (z < -12 || z > 12) {
                    yield EMPTY;
                } else if (z == -12) {
                    yield new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 0.5);
                } else if (z == 12) {
                    yield new AABB(0.0, 0.0, 0.5, 1.0, 1.0, 1.0);
                } else {
                    int hole = switch (y) {
                        case 2, 21 -> 3;
                        case 3, 20 -> 5;
                        case 4, 19 -> 6;
                        case 5, 18 -> 7;
                        case 6, 17 -> 8;
                        case 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 -> 9;
                        default -> -1;
                    };
                    yield hole >= 0 && Math.abs(z) <= hole ? EMPTY : FULL;
                }
            }
            default -> open ? EMPTY : FULL;
        };
    }

    private static AABB scaledBoundFor(DoorSpec spec, DoorSpec base, int x, int y, int z, boolean open, boolean collision) {
        if (!open && collision) {
            return FULL;
        } else {
            int[] od = base.dimensions;
            int[] nd = spec.dimensions;
            int oldWidth = od[4] + od[5] + 1;
            int newWidth = nd[4] + nd[5] + 1;
            int oldHeight = od[0] + od[1] + 1;
            int newHeight = nd[0] + nd[1] + 1;
            float sx = (float)newWidth / (float)oldWidth;
            float sy = (float)newHeight / (float)oldHeight;
            int oldWidthLow = -od[4];
            int newWidthLow = -nd[4];
            int oldHeightLow = -od[1];
            int newHeightLow = -nd[1];
            int by = mapCell(y, newHeightLow, oldHeightLow, sy, (double)(nd[0] + 1 - nd[1]) / 2.0);
            double start = (double)oldWidthLow + (double)(z - newWidthLow) / (double)sx;
            double end = (double)oldWidthLow + (double)(z + 1 - newWidthLow) / (double)sx;
            int first = (int)Math.floor(start + 1.0E-7);
            int last = (int)Math.floor(end - 1.0E-7);
            if (last < first) {
                first = last;
            }

            double minX = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE;
            double maxY = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE;
            double maxZ = -Double.MAX_VALUE;
            boolean any = false;

            for (int bz = first; bz <= last; bz++) {
                if (!(Math.min(end, (double)bz + 1.0) - Math.max(start, (double)bz) <= 1.0E-7)) {
                    AABB b = baseBoundFor(base, x, by, bz, open, collision);
                    if (!isEmpty(b)) {
                        double widthMin = (double)newWidthLow + ((double)bz + b.minX - (double)oldWidthLow) * (double)sx - (double)z;
                        double widthMax = (double)newWidthLow + ((double)bz + b.maxX - (double)oldWidthLow) * (double)sx - (double)z;
                        widthMin = Math.max(widthMin, 0.0);
                        widthMax = Math.min(widthMax, 1.0);
                        if (!(widthMax - widthMin <= 1.0E-7)) {
                            double heightMin = (double)newHeightLow + ((double)by + b.minY - (double)oldHeightLow) * (double)sy - (double)y;
                            double heightMax = (double)newHeightLow + ((double)by + b.maxY - (double)oldHeightLow) * (double)sy - (double)y;
                            heightMin = Math.max(heightMin, 0.0);
                            heightMax = Math.min(heightMax, 1.0);
                            if (!(heightMax - heightMin <= 1.0E-7)) {
                                any = true;
                                minX = Math.min(minX, widthMin);
                                maxX = Math.max(maxX, widthMax);
                                minY = Math.min(minY, heightMin);
                                maxY = Math.max(maxY, heightMax);
                                minZ = Math.min(minZ, b.minZ);
                                maxZ = Math.max(maxZ, b.maxZ);
                            }
                        }
                    }
                }
            }

            if (!any) {
                return EMPTY;
            } else {
                if (collision) {
                    boolean widthEdge = z == newWidthLow || z == newWidthLow + newWidth - 1;
                    boolean heightEdge = y == newHeightLow || y == newHeightLow + newHeight - 1;
                    if (!widthEdge && !heightEdge) {
                        if (maxX - minX >= 0.999 && maxY - minY >= 0.999) {
                            return FULL;
                        }

                        return EMPTY;
                    }
                }

                return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
            }
        }
    }

    private static int mapCell(int newCell, int newLow, int oldLow, float scale, double center) {
        double start = (double)oldLow + (double)(newCell - newLow) / (double)scale;
        double end = (double)oldLow + (double)(newCell + 1 - newLow) / (double)scale;
        int first = (int)Math.floor(start + 1.0E-7);
        int last = (int)Math.floor(end - 1.0E-7);
        if (last < first) {
            first = last;
        }

        int best = first;
        double bestOverlap = -1.0;

        for (int cell = first; cell <= last; cell++) {
            double overlap = Math.min(end, (double)cell + 1.0) - Math.max(start, (double)cell);
            if (overlap > bestOverlap + 1.0E-7) {
                best = cell;
                bestOverlap = overlap;
            } else if (Math.abs(overlap - bestOverlap) <= 1.0E-7 && Math.abs((double)cell + 0.5 - center) < Math.abs((double)best + 0.5 - center)) {
                best = cell;
            }
        }

        return best;
    }

    public static AABB worldBox(DoorSpec spec, BlockPos core, BlockPos part, Direction facing, boolean open, boolean collision) {
        return worldBox(spec, core, part, facing, open, collision, 0);
    }

    public static AABB worldBox(DoorSpec spec, BlockPos core, BlockPos part, Direction facing, boolean open, boolean collision, int extraHeight) {
        int dx = part.getX() - core.getX();
        int dy = part.getY() - core.getY();
        int dz = part.getZ() - core.getZ();
        int[] local = rotateLocal(dx, dy, dz, localRotation(facing));
        AABB b = boundFor(spec, local[0], local[1], local[2], open, collision, extraHeight);
        double x = (double)part.getX();
        double y = (double)part.getY();
        double z = (double)part.getZ();
        double minY = y + Math.min(b.minY, b.maxY);
        double maxY = y + Math.max(b.minY, b.maxY);
        double minX;
        double minZ;
        double maxX;
        double maxZ;
        switch (facing) {
            case NORTH:
                minX = x + 1.0 - Math.max(b.minX, b.maxX);
                maxX = x + 1.0 - Math.min(b.minX, b.maxX);
                minZ = z + 1.0 - Math.max(b.minZ, b.maxZ);
                maxZ = z + 1.0 - Math.min(b.minZ, b.maxZ);
                break;
            case EAST:
                minX = x + Math.min(b.minZ, b.maxZ);
                maxX = x + Math.max(b.minZ, b.maxZ);
                minZ = z + 1.0 - Math.max(b.minX, b.maxX);
                maxZ = z + 1.0 - Math.min(b.minX, b.maxX);
                break;
            case WEST:
                minX = x + 1.0 - Math.max(b.minZ, b.maxZ);
                maxX = x + 1.0 - Math.min(b.minZ, b.maxZ);
                minZ = z + Math.min(b.minX, b.maxX);
                maxZ = z + Math.max(b.minX, b.maxX);
                break;
            default:
                minX = x + Math.min(b.minX, b.maxX);
                maxX = x + Math.max(b.minX, b.maxX);
                minZ = z + Math.min(b.minZ, b.maxZ);
                maxZ = z + Math.max(b.minZ, b.maxZ);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean isEmpty(AABB box) {
        return box.minX == box.maxX || box.minY == box.maxY || box.minZ == box.maxZ;
    }
}
