package org.lytharalab.gfbs.doors.client.armor.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The direct equivalent of HBM's {@code setOverlay("hbm:textures/misc/..."）}: a 256x256 texture
 * stretched over the whole screen.
 *
 * <p>HBM drew these with a GL scale over the view; the same effect here is a pose scale followed by the
 * full texture. Alpha in the texture decides how much of the view it covers, so a vignette or a visor
 * frame both work.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TextureHudOverlay implements ArmorHudOverlay {
    private static final float TEXTURE_SIZE = 256.0F;

    private final ResourceLocation texture;

    public TextureHudOverlay(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    public void render(ArmorHudContext context) {
        if (context.width() <= 0 || context.height() <= 0) {
            return;
        }
        PoseStack pose = context.graphics().pose();
        pose.pushPose();
        pose.scale(context.width() / TEXTURE_SIZE, context.height() / TEXTURE_SIZE, 1.0F);
        context.graphics().blit(this.texture, 0, 0, 0, 0, (int) TEXTURE_SIZE, (int) TEXTURE_SIZE);
        pose.popPose();
    }
}
