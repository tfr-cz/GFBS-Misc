package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.DoorSpec;
import org.lytharalab.gfbs.doors.content.block.DoorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class DoorVisualCache {
    private static final Map<DoorBlockEntity, DoorVisual> CACHE = new WeakHashMap<>();

    private DoorVisualCache() {
    }

    public static DoorVisual get(DoorBlockEntity be, DoorSpec spec, GltfAsset asset) {
        DoorVisual visual = CACHE.get(be);
        if (visual != null && visual.asset() == asset) return visual;
        if (visual != null) visual.close();

        visual = new DoorVisual(spec, asset);
        visual.applySkin(be.getSkin());
        CACHE.put(be, visual);
        return visual;
    }

    public static void clear() {
        for (DoorVisual visual : CACHE.values()) visual.close();
        CACHE.clear();
    }
}
