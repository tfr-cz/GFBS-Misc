package org.lytharalab.gfbs.doors.client.anim;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * A named group of {@link BusAnimationSequence}s, HBM's {@code BusAnimation}.
 *
 * <p>NTM's items described an animation as several independent tracks - the Crucible swings on {@code SWING_ROT}
 * and {@code SWING_TRANS} while its guard plates move on {@code GUARD_ROT} - and the renderer looked each one up by
 * name. Keeping the names is what makes the clips below readable as the originals.</p>
 */
public final class BusAnimation {
    private final Map<String, BusAnimationSequence> buses = new LinkedHashMap<>();
    private int duration;

    public BusAnimation addBus(String name, BusAnimationSequence sequence) {
        this.buses.put(name, sequence);
        this.duration = Math.max(this.duration, sequence.duration());
        return this;
    }

    @Nullable
    public BusAnimationSequence bus(String name) {
        return this.buses.get(name);
    }

    /** How long the longest track runs, in milliseconds. */
    public int duration() {
        return this.duration;
    }
}
