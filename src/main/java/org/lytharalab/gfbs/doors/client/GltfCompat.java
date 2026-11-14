package org.lytharalab.gfbs.doors.client;

import java.lang.reflect.Method;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Optional bridge to the {@code gfbs_gltf} 1.6.0 API ({@code prepareScene}, {@code shadowFrustumCulling},
 * {@code labPbrEmission}).
 *
 * <p>Probed by reflection so the mod keeps running on 1.5.x: when the newer API is missing every call is a
 * no-op and the renderer stays on its old path.</p>
 */
public final class GltfCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors");

    private static Method prepareSceneMethod;
    private static Method shadowFrustumMethod;
    private static Method labPbrEmissionMethod;
    private static boolean resolved;

    private GltfCompat() {
    }

    private static synchronized void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;

        prepareSceneMethod = find("org.lytharalab.gfbs.gltf.api.client.GltfRenderer", "prepareScene", GltfAsset.class, int.class);
        shadowFrustumMethod = find("org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions", "shadowFrustumCulling", boolean.class);
        labPbrEmissionMethod = find("org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions", "labPbrEmission", boolean.class);

        if (prepareSceneMethod != null) {
            LOGGER.info("gfbs_gltf 1.6.0 特性已启用: prepareScene / shadowFrustumCulling / labPbrEmission");
        } else {
            LOGGER.info("gfbs_gltf 版本低于 1.6.0，新特性不可用，按旧行为运行");
        }
    }

    private static Method find(String className, String method, Class<?>... params) {
        try {
            Method found = Class.forName(className).getMethod(method, params);
            found.setAccessible(true);
            return found;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    public static void prepareScene(GltfAsset asset) {
        resolve();
        if (prepareSceneMethod == null || asset == null) {
            return;
        }
        try {
            prepareSceneMethod.invoke(null, asset, 0);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.debug("prepareScene 失败，忽略", e);
        }
    }

    public static void applyOptions(GltfRenderOptions options) {
        resolve();
        if (options == null) {
            return;
        }
        if (shadowFrustumMethod != null) {
            try {
                shadowFrustumMethod.invoke(options, true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.debug("shadowFrustumCulling 失败，忽略", e);
            }
        }
        if (labPbrEmissionMethod != null) {
            try {
                labPbrEmissionMethod.invoke(options, false);
            } catch (ReflectiveOperationException | RuntimeException e) {
                LOGGER.debug("labPbrEmission 失败，忽略", e);
            }
        }
    }

    public static boolean hasModernApi() {
        resolve();
        return prepareSceneMethod != null;
    }
}
