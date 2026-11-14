package org.lytharalab.gfbs.misc.mixin;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Applies the glTF texture mixin only when its target exists.
 *
 * <p>The target is a {@code gfbs_gltf} 1.6.0 class. On 1.5.x it is missing, and a mixin config that names a
 * missing target aborts startup — so every mixin under {@code org.lytharalab.gfbs.misc.mixin} is gated on its
 * target being loadable. When glTF is upgraded the same jar starts applying it with no code change.</p>
 */
public class GfbsMiscMixinPlugin implements IMixinConfigPlugin {
    private static final String GLTF_UPLOADED_TEXTURE = "org.lytharalab.gfbs.gltf.client.render.GltfUploadedTexture";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (GLTF_UPLOADED_TEXTURE.equals(targetClassName)) {
            return classExists(targetClassName);
        }
        return true;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, GfbsMiscMixinPlugin.class.getClassLoader());
            return true;
        } catch (Throwable missing) {
            return false;
        }
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
