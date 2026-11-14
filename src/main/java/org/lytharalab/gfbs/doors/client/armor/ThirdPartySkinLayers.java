package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drops the 3D skin meshes that 3D Skin Layers (skinlayers3d) keeps on the player entity.
 *
 * <p>That mod mixes {@code PlayerSettings} onto {@code AbstractClientPlayer} and stores six generated
 * meshes there. Its layer only runs when {@code PlayerSettings#getHeadMesh()} is non-null, so
 * clearing the meshes is the mod's own supported way of saying "do not draw 3D skin for this
 * player". Clearing them also makes the next frame's {@code SkinUtil.setup3dLayers} short-circuit
 * on the cached skin, so no meshes are regenerated - clearing every frame is cheap, not a rebuild
 * loop.</p>
 *
 * <p>The call is reflective on purpose: this mod must keep working with skinlayers3d absent, and a
 * compile-time reference would tie the suit to one version of it. If the interface or method is ever
 * renamed, the lookup fails once and everything else still works - the vanilla overlay is hidden by
 * {@link HazmatSkinLayers} regardless.</p>
 */
@OnlyIn(Dist.CLIENT)
final class ThirdPartySkinLayers {
    private static final String SETTINGS = "dev.tr7zw.skinlayers.accessor.PlayerSettings";
    private static final Logger LOG = Logger.getLogger("gfbs_misc/skinlayers3d");
    private static Method clearMeshes;
    private static Class<?> settingsInterface;
    private static boolean resolved;

    private ThirdPartySkinLayers() {
    }

    static void drop3dMeshes(AbstractClientPlayer player) {
        Method method = resolve();
        if (method == null || !settingsInterface.isInstance(player)) {
            return;
        }
        try {
            method.invoke(player);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A skin-layers version that changed its shape is not a reason to spam the log every frame.
        }
    }

    private static Method resolve() {
        if (resolved) {
            return clearMeshes;
        }
        resolved = true;
        if (net.minecraftforge.fml.ModList.get().getModFileById("skinlayers3d") == null) {
            return null;
        }
        try {
            settingsInterface = Class.forName(SETTINGS);
            clearMeshes = settingsInterface.getMethod("clearMeshes");
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
            LOG.log(Level.FINE, "3D Skin Layers present but its PlayerSettings API was not found", exception);
            clearMeshes = null;
            settingsInterface = null;
        }
        return clearMeshes;
    }
}
