package org.lytharalab.gfbs.doors.client.anim;

import org.junit.jupiter.api.Test;
import org.lytharalab.gfbs.doors.client.anim.BusAnimationSequence.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins HBM's keyframe semantics, which the Crucible's clips lean on: linear between keyframes, a zero-length
 * keyframe snaps to its value, and past the end the last keyframe holds.
 */
class BusAnimationSequenceTest {
    private static final float EPSILON = 1.0E-4F;

    @Test
    void theFirstKeyframeInterpolatesFromRest() {
        // NTM starts from whatever the resting pose is (0), not from the first keyframe's own value.
        BusAnimationSequence sequence = new BusAnimationSequence().addPos(10, -20, 5, 100);
        assertEquals(5.0F, sequence.transformation(50)[Dimension.TX.ordinal()], EPSILON);
        assertEquals(-10.0F, sequence.transformation(50)[Dimension.TY.ordinal()], EPSILON);
    }

    @Test
    void valuesInterpolateLinearlyBetweenKeyframes() {
        BusAnimationSequence sequence = new BusAnimationSequence()
            .addPos(10, -20, 5, 100)
            .addPos(0, 0, 0, 100);
        assertEquals(5.0F, sequence.transformation(150)[Dimension.TX.ordinal()], EPSILON, "halfway back to rest");
        assertEquals(0.0F, sequence.transformation(250)[Dimension.TX.ordinal()], EPSILON);
    }

    @Test
    void theLastKeyframeHoldsAfterTheSequenceEnds() {
        BusAnimationSequence sequence = new BusAnimationSequence().addPos(10, 0, 0, 100);
        assertEquals(10.0F, sequence.transformation(5_000)[Dimension.TX.ordinal()], EPSILON);
    }

    @Test
    void aZeroLengthKeyframeSnaps() {
        // The equip clip: snap the guards shut, hold, then open — exactly HBM's addPos(90, 0, 1, 0) chain.
        BusAnimationSequence sequence = new BusAnimationSequence()
            .addRot(90, 0, 1, 0)
            .addRot(90, 0, 1, 800)
            .addRot(0, 0, 1, 50);
        assertEquals(850, sequence.duration());
        assertEquals(90.0F, sequence.transformation(0)[Dimension.RX.ordinal()], EPSILON);
        assertEquals(90.0F, sequence.transformation(400)[Dimension.RX.ordinal()], EPSILON);
        assertEquals(45.0F, sequence.transformation(825)[Dimension.RX.ordinal()], EPSILON);
        assertEquals(0.0F, sequence.transformation(900)[Dimension.RX.ordinal()], EPSILON);
    }

    @Test
    void untouchedChannelsStayAtTheirIdentity() {
        float[] values = new BusAnimationSequence().addPos(1, 2, 3, 100).transformation(50);
        assertEquals(1.0F, values[Dimension.SX.ordinal()], EPSILON, "scale defaults to 1, not 0");
        assertEquals(0.0F, values[Dimension.RX.ordinal()], EPSILON, "rotation defaults to 0");
    }
}
