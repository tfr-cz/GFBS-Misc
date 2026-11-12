package org.lytharalab.gfbs.doors.client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OnlyIn(Dist.CLIENT)
public final class TransitionSealAnimation {
    private static final Logger LOGGER = LoggerFactory.getLogger("gfbs_doors");
    private static final int MAGIC = 1195786820;
    private static final ResourceLocation RESOURCE = new ResourceLocation("gfbs_doors", "models/doors/transition_seal.anim");
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
            this.nodes.put(names[i], i);
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
        Optional resource = Minecraft.getInstance().getResourceManager().getResource(RESOURCE);
        if (resource.isEmpty()) {
            LOGGER.error("Missing transition seal animation {}", RESOURCE);
            return null;
        } else {
            try {
                TransitionSealAnimation var19;
                try (DataInputStream input = new DataInputStream(new BufferedInputStream(((Resource)resource.get()).open()))) {
                    int magic = input.readInt();
                    int version = input.readInt();
                    if (magic != 1195786820 || version != 1) {
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
                        names[i] = new String(data, StandardCharsets.UTF_8);
                    }

                    float[] times = new float[frameCount];

                    for (int i = 0; i < frameCount; i++) {
                        times[i] = input.readFloat();
                    }

                    float[] matrices = new float[frameCount * nodeCount * 16];

                    for (int i = 0; i < matrices.length; i++) {
                        matrices[i] = input.readFloat();
                    }

                    var19 = new TransitionSealAnimation(duration, times, matrices, names);
                }

                return var19;
            } catch (IOException var14) {
                LOGGER.error("Unable to read transition seal animation {}", RESOURCE, var14);
                return null;
            }
        }
    }

    public float duration() {
        return this.duration;
    }

    public Iterable<String> nodes() {
        return this.nodes.keySet();
    }

    @Nullable
    public float[] localMatrix(String node, float seconds) {
        Integer index = (Integer)this.nodes.get(node);
        if (index == null) {
            return null;
        } else {
            float time = Mth.clamp(seconds, 0.0F, this.duration);
            int left = 0;
            int right = this.frameCount - 1;

            while (left < right - 1) {
                int middle = left + right >>> 1;
                if (this.times[middle] <= time) {
                    left = middle;
                } else {
                    right = middle;
                }
            }

            float blend = 0.0F;
            if (this.times[right] > this.times[left]) {
                blend = (time - this.times[left]) / (this.times[right] - this.times[left]);
            }

            blend = Mth.clamp(blend, 0.0F, 1.0F);
            int baseLeft = (left * this.nodeCount + index) * 16;
            int baseRight = (right * this.nodeCount + index) * 16;
            float[] columnMajor = new float[16];

            for (int row = 0; row < 4; row++) {
                for (int column = 0; column < 4; column++) {
                    int i = row * 4 + column;
                    float value = this.matrices[baseLeft + i] + (this.matrices[baseRight + i] - this.matrices[baseLeft + i]) * blend;
                    columnMajor[column * 4 + row] = value;
                }
            }

            return columnMajor;
        }
    }
}
