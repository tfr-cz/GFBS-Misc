package org.lytharalab.gfbs.doors.client.anim;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * One animated track from HBM's {@code BusAnimationSequence}: nine channels - translate x/y/z, rotate x/y/z,
 * scale x/y/z - each a list of keyframes, each keyframe a target value reached after a duration.
 *
 * <p>This is a straight port of NTM's class of the same name, including the way it reads time: every channel is
 * interpolated independently from the previous keyframe towards the current one, a zero-length keyframe snaps, and
 * past the last keyframe the last value holds. The port drops NTM's {@code CONSTANT} interpolation type because
 * nothing in the Crucible's clips uses it.</p>
 */
public final class BusAnimationSequence {
    public enum Dimension {
        TX, TY, TZ, RX, RY, RZ, SX, SY, SZ
    }

    /** A target value and how long the move towards it takes, in milliseconds. */
    private record Keyframe(float value, int duration) {
    }

    private final Map<Dimension, List<Keyframe>> channels = new EnumMap<>(Dimension.class);

    public BusAnimationSequence() {
        for (Dimension dimension : Dimension.values()) {
            this.channels.put(dimension, new ArrayList<>());
        }
    }

    public BusAnimationSequence addPos(float x, float y, float z, int duration) {
        add(Dimension.TX, x, duration);
        add(Dimension.TY, y, duration);
        add(Dimension.TZ, z, duration);
        return this;
    }

    public BusAnimationSequence addRot(float x, float y, float z, int duration) {
        add(Dimension.RX, x, duration);
        add(Dimension.RY, y, duration);
        add(Dimension.RZ, z, duration);
        return this;
    }

    public BusAnimationSequence add(Dimension dimension, float value, int duration) {
        this.channels.get(dimension).add(new Keyframe(value, duration));
        return this;
    }

    /** Longest channel, i.e. how long the whole sequence lasts. */
    public int duration() {
        int longest = 0;
        for (List<Keyframe> keyframes : this.channels.values()) {
            int total = 0;
            for (Keyframe keyframe : keyframes) {
                total += keyframe.duration();
            }
            longest = Math.max(longest, total);
        }
        return longest;
    }

    /**
     * The nine channel values at {@code millis} into the sequence.
     *
     * <p>Scale channels default to 1 and everything else to 0, which is what makes an empty channel a no-op.</p>
     */
    public float[] transformation(int millis) {
        float[] values = new float[Dimension.values().length];
        for (Dimension dimension : Dimension.values()) {
            boolean isScale = dimension == Dimension.SX || dimension == Dimension.SY || dimension == Dimension.SZ;
            values[dimension.ordinal()] = isScale ? 1.0F : 0.0F;
        }

        for (Map.Entry<Dimension, List<Keyframe>> channel : this.channels.entrySet()) {
            List<Keyframe> keyframes = channel.getValue();
            if (keyframes.isEmpty()) {
                continue;
            }
            float value = 0.0F;
            int start = 0;
            int end = 0;
            Keyframe previous = null;
            Keyframe current = null;
            for (Keyframe keyframe : keyframes) {
                start = end;
                end += keyframe.duration();
                previous = current;
                current = keyframe;
                if (millis < end) {
                    break;
                }
            }
            if (current == null) {
                continue;
            }
            if (millis >= end || current.duration() == 0) {
                value = current.value();
            } else if (previous == null) {
                value = current.value() * (float) (millis - start) / (float) (end - start);
            } else {
                float span = (float) (end - start);
                float progress = span <= 0.0F ? 1.0F : (float) (millis - start) / span;
                value = previous.value() + (current.value() - previous.value()) * progress;
            }
            values[channel.getKey().ordinal()] = value;
        }
        return values;
    }
}
