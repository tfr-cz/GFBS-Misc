package org.lytharalab.gfbs.doors.content;

import java.util.concurrent.CopyOnWriteArrayList;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;

public final class DoorCoreRegistry {
    private static final CopyOnWriteArrayList<DoorBlockEntity> CORES = new CopyOnWriteArrayList<>();

    private DoorCoreRegistry() {
    }

    public static void register(DoorBlockEntity core) {
        if (!CORES.contains(core)) {
            CORES.add(core);
        }
    }

    public static void unregister(DoorBlockEntity core) {
        CORES.remove(core);
    }

    public static CopyOnWriteArrayList<DoorBlockEntity> cores() {
        return CORES;
    }
}
