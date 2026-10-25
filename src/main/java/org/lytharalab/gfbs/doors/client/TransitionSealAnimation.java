package org.lytharalab.gfbs.doors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lytharalab.gfbs.doors.GFBSDoors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class TransitionSealAnimation {
    private static final Logger LOGGER = LoggerFactory.getLogger(GFBSDoors.MODID);
    private static final int MAGIC = 0x47464244;
    private static final ResourceLocation RESOURCE =
            new ResourceLocation(GFBSDoors.MODID, "models/doors/transition_seal.anim");

    private static TransitionSealAnimation instance;
    private static boolean attempted;

    private final Map<String, Integer> nodes = new HashMap<>();
    private final float duration;
    private final float[] times;
    private final float[] matrices;
    private final int frameCount;
    private final int nodeCount;

    private TransitionSealAnimation(float duration, float[] times, float[] matrices, String[] names) {
        this.duration = duration;
        this.times = times;
        this.matrices = matrices;
        this.frameCount = times.length;
        this.nodeCount = names.length;
        for (int i = 0; i < names.length; i++) {
            nodes.put(names[i], i);
        }
    }

    @Nullable
    public static TransitionSealAnimation get() {
        if (!attempted) {
            attempted = true;
            instance = load();
        }
        return instance;
    }

    public static void clear() {
        instance = null;
        attempted = false;
    }

    private static TransitionSealAnimation load() {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(RESOURCE);
        if (resource.isEmpty()) {
            LOGGER.error("Missing transition seal animation {}", RESOURCE);
            return null;
        }
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(resource.get().open()))) {
            int magic = input.readInt();
            int version = input.readInt();
            if (magic != MAGIC || version != 1) {
                LOGGER.error("Unsupported transition seal animation format {}", RESOURCE);
                return null;
            }
            int nodeCount = input.readInt();
            float duration = input.readFloat();
            int frameCount = input.readInt();

            String[] names = new String[nodeCount];
            for (int i = 0; i < nodeCount; i++) {
                int length = input.readUnsignedShort();
                byte[] data = new byte[length];
                input.readFully(data);
                names[i] = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }

            float[] times = new float[frameCount];
            for (int i = 0; i < frameCount; i++) times[i] = input.readFloat();

            float[] matrices = new float[frameCount * nodeCount * 16];
            for (int i = 0; i < matrices.length; i++) matrices[i] = input.readFloat();

            return new TransitionSealAnimation(duration, times, matrices, names);
        } catch (IOException exception) {
            LOGGER.error("Unable to read transition seal animation {}", RESOURCE, exception);
            return null;
        }
    }

    public float duration() {
        return duration;
    }

    public Iterable<String> nodes() {
        return nodes.keySet();
    }

    /**
     * Returns the node's local matrix at the given time in column-major order, ready for
     * {@code GltfNodeState.localMatrix}.
     */
    @Nullable
    public float[] localMatrix(String node, float seconds) {
        Integer index = nodes.get(node);
        if (index == null) return null;

        float time = Mth.clamp(seconds, 0F, duration);
        int left = 0;
        int right = frameCount - 1;
        while (left < right - 1) {
            int middle = (left + right) >>> 1;
            if (times[middle] <= time) left = middle;
            else right = middle;
        }
        float blend = 0F;
        if (times[right] > times[left]) {
            blend = (time - times[left]) / (times[right] - times[left]);
        }
        blend = Mth.clamp(blend, 0F, 1F);

        int baseLeft = (left * nodeCount + index) * 16;
        int baseRight = (right * nodeCount + index) * 16;
        float[] columnMajor = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int i = row * 4 + column;
                float value = matrices[baseLeft + i]
                        + (matrices[baseRight + i] - matrices[baseLeft + i]) * blend;
                columnMajor[column * 4 + row] = value;
            }
        }
        return columnMajor;
    }
}
