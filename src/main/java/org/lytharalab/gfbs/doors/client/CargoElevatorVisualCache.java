package org.lytharalab.gfbs.doors.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.content.block.CargoElevatorBlockEntity;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;

import java.util.Map;
import java.util.WeakHashMap;

@OnlyIn(Dist.CLIENT)
public final class CargoElevatorVisualCache {
    private static final Map<CargoElevatorBlockEntity, CargoElevatorVisual> CACHE = new WeakHashMap<>();

    private CargoElevatorVisualCache() {
    }

    public static CargoElevatorVisual get(CargoElevatorBlockEntity be, GltfAsset asset) {
        CargoElevatorVisual visual = CACHE.get(be);
        if (visual != null && visual.asset() == asset) return visual;
        if (visual != null) visual.close();

        visual = new CargoElevatorVisual(asset);
        CACHE.put(be, visual);
        return visual;
    }

    public static void clear() {
        for (CargoElevatorVisual visual : CACHE.values()) visual.close();
        CACHE.clear();
    }
}
