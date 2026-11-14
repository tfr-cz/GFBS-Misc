package org.lytharalab.gfbs.doors.client.armor;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Suppresses and restores the 3D skin meshes that 3D Skin Layers (skinlayers3d) keeps per player.
 *
 * <p>That mod mixes {@code PlayerSettings} onto {@code Player} and stores six generated meshes there.
 * Its layer only runs when {@code PlayerSettings#getHeadMesh()} is non-null, so clearing the meshes
 * is the mod's own way of saying "draw no 3D skin for this player".</p>
 *
 * <p>Clearing has to be undone by hand. {@code SkinUtil.setup3dLayers} short-circuits on
 * "skin unchanged", so once the meshes are null it never rebuilds them - the player would keep a flat
 * skin after taking the suit off. {@link #invalidate} therefore drops the cached skin location once,
 * which makes the mod regenerate its meshes from the texture on the next frame.</p>
 *
 * <p>Everything is reflective on purpose: this mod must keep working with skinlayers3d absent, and a
 * compile-time reference would pin the suit to one version of it. If the interface is renamed the
 * lookup fails once and only this feature goes away - the vanilla overlay is hidden by
 * {@link HazmatSkinLayers} regardless.</p>
 */
@OnlyIn(Dist.CLIENT)
final class ThirdPartySkinLayers {
    private static final String SETTINGS = "dev.tr7zw.skinlayers.accessor.PlayerSettings";
    private static final Logger LOG = Logger.getLogger("gfbs_misc/skinlayers3d");

    private static boolean resolved;
    private static Class<?> settingsInterface;
    private static Method clearMeshes;
    private static Method setCurrentSkin;

    private ThirdPartySkinLayers() {
    }

    /** Hides the 3D layers by emptying the mesh cache the mod renders from. */
    static void suppress(AbstractClientPlayer player) {
        if (!ready() || !settingsInterface.isInstance(player)) {
            return;
        }
        try {
            clearMeshes.invoke(player);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // A version that changed shape is not a reason to spam the log every frame.
        }
    }

    /** Drops the "already built for this skin" marker so the meshes are rebuilt once. */
    static void invalidate(AbstractClientPlayer player) {
        if (!ready() || !settingsInterface.isInstance(player)) {
            return;
        }
        try {
            setCurrentSkin.invoke(player, (Object) null);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Same as above: the feature degrades, nothing else breaks.
        }
    }

    private static boolean ready() {
        if (!resolved) {
            resolve();
        }
        return clearMeshes != null && setCurrentSkin != null;
    }

    private static void resolve() {
        resolved = true;
        if (net.minecraftforge.fml.ModList.get().getModFileById("skinlayers3d") == null) {
            return;
        }
        try {
            settingsInterface = Class.forName(SETTINGS);
            clearMeshes = settingsInterface.getMethod("clearMeshes");
            setCurrentSkin = settingsInterface.getMethod("setCurrentSkin", ResourceLocation.class);
        } catch (ClassNotFoundException | NoSuchMethodException | LinkageError exception) {
            LOG.log(Level.FINE, "3D Skin Layers present but its PlayerSettings API changed", exception);
            clearMeshes = null;
            setCurrentSkin = null;
        }
    }
}
