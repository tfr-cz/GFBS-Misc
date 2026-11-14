package org.lytharalab.gfbs.doors.client;

import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional bridge to {@code gfbs_auralis}.
 *
 * <p>Resolved by reflection so the mod still loads (and falls back to the vanilla {@code SoundManager})
 * when Auralis is absent: every method here degrades to a no-op instead of throwing.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class AuralisBackend {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors");

    private static boolean resolved;
    private static boolean available;
    private static Method apiIsInitialized;
    private static Method apiCreate;
    private static Method setPosition;
    private static Method setVolume;
    private static Method setLooping;
    private static Method setMinDistance;
    private static Method setMaxDistance;
    private static Method setPriority;
    private static Method setAutoDisposeOnFinish;
    private static Method play;
    private static Method stop;
    private static Method isPlaying;

    private AuralisBackend() {
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        try {
            Class<?> api = Class.forName("org.lytharalab.gfbs.auralis.api.AuralisApi");
            apiIsInitialized = api.getMethod("isInitialized");
            apiCreate = api.getMethod("create", SoundEvent.class);

            Class<?> instance = Class.forName("org.lytharalab.gfbs.auralis.api.AuralisSoundInstance");
            setPosition = instance.getMethod("setPosition", Vec3.class);
            setVolume = instance.getMethod("setVolume", float.class);
            setLooping = instance.getMethod("setLooping", boolean.class);
            setMinDistance = instance.getMethod("setMinDistance", float.class);
            setMaxDistance = instance.getMethod("setMaxDistance", float.class);
            setPriority = instance.getMethod("setPriority", int.class);
            setAutoDisposeOnFinish = instance.getMethod("setAutoDisposeOnFinish", boolean.class);
            play = instance.getMethod("play");
            stop = instance.getMethod("stop");
            isPlaying = instance.getMethod("isPlaying");
            available = true;
            LOGGER.info("gfbs_auralis 已接管门音效");
        } catch (RuntimeException | ReflectiveOperationException e) {
            available = false;
            LOGGER.debug("未安装 gfbs_auralis，门音效回退到原版 SoundManager");
        }
    }

    public static boolean isAvailable() {
        resolve();
        if (!available) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(apiIsInitialized.invoke(null));
        } catch (RuntimeException | ReflectiveOperationException e) {
            return false;
        }
    }

    public static Handle playLoop(SoundEvent event, BlockPos pos, float volume, float minDistance, float maxDistance, int priority) {
        if (event == null || !isAvailable()) {
            return null;
        }
        try {
            Object instance = apiCreate.invoke(null, event);
            if (instance == null) {
                return null;
            }
            setPosition.invoke(instance, Vec3.atCenterOf(pos));
            setVolume.invoke(instance, volume);
            setLooping.invoke(instance, true);
            setMinDistance.invoke(instance, minDistance);
            setMaxDistance.invoke(instance, maxDistance);
            setPriority.invoke(instance, priority);
            setAutoDisposeOnFinish.invoke(instance, false);
            play.invoke(instance);
            return new Handle(instance);
        } catch (RuntimeException | ReflectiveOperationException e) {
            LOGGER.warn("Auralis 播放失败，回退到原版音效", e);
            return null;
        }
    }

    /** One live Auralis sound; every call degrades to a no-op when the backend disappears mid-session. */
    public static final class Handle {
        private final Object instance;

        private Handle(Object instance) {
            this.instance = instance;
        }

        public void setVolume(float volume) {
            try {
                AuralisBackend.setVolume.invoke(this.instance, volume);
            } catch (RuntimeException | ReflectiveOperationException e) {
                AuralisBackend.LOGGER.debug("Auralis setVolume 失败，忽略", e);
            }
        }

        public void stop() {
            try {
                AuralisBackend.stop.invoke(this.instance);
            } catch (RuntimeException | ReflectiveOperationException e) {
                AuralisBackend.LOGGER.debug("Auralis stop 失败，忽略", e);
            }
        }

        public boolean isPlaying() {
            try {
                return Boolean.TRUE.equals(AuralisBackend.isPlaying.invoke(this.instance));
            } catch (RuntimeException | ReflectiveOperationException e) {
                return false;
            }
        }
    }
}
