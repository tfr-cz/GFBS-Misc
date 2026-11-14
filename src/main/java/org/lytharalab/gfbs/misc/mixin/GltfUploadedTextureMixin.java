package org.lytharalab.gfbs.misc.mixin;

import org.lwjgl.opengl.GL11C;
import org.lwjgl.opengl.GL12C;
import org.lwjgl.opengl.GL30C;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps glTF-uploaded door textures crisp: the generated mipmaps blur the pixel art, so the upload is pinned to
 * {@code GL_NEAREST} filtering with mipmapping switched off.
 *
 * <p>Targets {@code gfbs_gltf}'s uploaded-texture class, which only exists from 1.6.0 on.
 * {@link GfbsMiscMixinPlugin} vetoes this mixin when that class is absent, so 1.5.x still loads.</p>
 */
@Mixin(targets = "org.lytharalab.gfbs.gltf.client.render.GltfUploadedTexture", remap = false)
public class GltfUploadedTextureMixin {
    private static final boolean DEBUG = Boolean.getBoolean("gfbs_misc.debug.texture");

    @Redirect(
        method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage;IIIIZI)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11C;glTexParameteri(III)V"),
        remap = false)
    private void gfbsmisc$adjustTextureParameters(int target, int pname, int param) {
        int value = param;
        if (pname == GL12C.GL_TEXTURE_MAX_LEVEL) {
            value = 0;
        } else if (pname == GL11C.GL_TEXTURE_MAG_FILTER) {
            value = GL11C.GL_NEAREST;
        } else if (pname == GL11C.GL_TEXTURE_MIN_FILTER && param >= GL11C.GL_NEAREST_MIPMAP_NEAREST && param <= GL11C.GL_LINEAR_MIPMAP_LINEAR) {
            value = GL11C.GL_LINEAR;
        }

        if (DEBUG) {
            LoggerFactory.getLogger("gfbs_doors").info("[gfbs_misc/mixin] glTexParameteri pname={} {} -> {}", pname, glName(pname), value);
        }

        GL11C.glTexParameteri(target, pname, value);
    }

    @Redirect(
        method = "<init>(Lcom/mojang/blaze3d/platform/NativeImage;IIIIZI)V",
        at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGenerateMipmap(I)V"),
        remap = false)
    private void gfbsmisc$skipMipmap(int target) {
        if (DEBUG) {
            LoggerFactory.getLogger("gfbs_doors").info("[gfbs_misc/mixin] skip glGenerateMipmap({})", target);
        }
    }

    private static String glName(int pname) {
        return switch (pname) {
            case GL11C.GL_TEXTURE_MAG_FILTER -> "MAG_FILTER";
            case GL11C.GL_TEXTURE_MIN_FILTER -> "MIN_FILTER";
            case GL11C.GL_TEXTURE_WRAP_S -> "WRAP_S";
            case GL11C.GL_TEXTURE_WRAP_T -> "WRAP_T";
            case GL12C.GL_TEXTURE_MAX_LEVEL -> "MAX_LEVEL";
            default -> "?";
        };
    }
}
