package org.lytharalab.gfbs.doors.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

@OnlyIn(Dist.CLIENT)
public final class DoorVisualCache {
    private static final Map<Long, DoorVisualCache.Entry> CACHE = new HashMap<>();

    private DoorVisualCache() {
    }

    private static long key(BlockPos pos) {
        return pos.asLong();
    }

    public static DoorVisual get(DoorBlockEntity be, DoorSpec spec, GltfAsset asset) {
        long k = key(be.getBlockPos());
        DoorVisualCache.Entry entry = (DoorVisualCache.Entry)CACHE.get(k);
        if (entry != null && entry.asset == asset && entry.level == be.getLevel()) {
            return entry.visual;
        } else {
            if (entry != null) {
                entry.visual.close();
            }

            DoorVisual visual = new DoorVisual(spec, asset);
            visual.applySkin(be.getSkin());
            CACHE.put(k, new DoorVisualCache.Entry(be.getLevel(), visual, asset));
            return visual;
        }
    }

    public static void invalidate(BlockPos pos) {
        DoorVisualCache.Entry entry = (DoorVisualCache.Entry)CACHE.remove(key(pos));
        if (entry != null) {
            entry.visual.close();
        }
    }

    public static void invalidateLevel(Level keep) {
        Iterator it = CACHE.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry e = (Map.Entry<Long, DoorVisualCache.Entry>)it.next();
            if (((DoorVisualCache.Entry)e.getValue()).level != keep) {
                ((DoorVisualCache.Entry)e.getValue()).visual.close();
                it.remove();
            }
        }
    }

    public static int size() {
        return CACHE.size();
    }

    public static void clear() {
        for (DoorVisualCache.Entry entry : CACHE.values()) {
            entry.visual.close();
        }

        CACHE.clear();
    }

    private static record Entry(Level level, DoorVisual visual, GltfAsset asset) {
    }
}
