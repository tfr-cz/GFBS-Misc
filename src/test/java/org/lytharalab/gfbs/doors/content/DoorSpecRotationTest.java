package org.lytharalab.gfbs.doors.content;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the two lookup tables a plain decompile-and-rebuild dropped: the precomputed per-facing dimensions and the
 * variant-to-base link. Both exist so the renderer stops allocating and re-scanning {@code BY_ID} every frame.
 */
class DoorSpecRotationTest {
    @Test
    void theAuthoredFacingIsSouthAndTheOtherThreeRotateIt() {
        DoorSpec spec = DoorSpec.builder("probe")
            .dims(1, 2, 3, 4, 5, 6)
            .time(20)
            .model("doors/probe")
            .parts("main")
            .skins("probe")
            .sounds(null, null, null, null, null, null, null)
            .build();

        assertArrayEquals(new int[]{1, 2, 3, 4, 5, 6}, spec.rotatedDims(Direction.SOUTH), "south is the authored facing");
        assertArrayEquals(new int[]{1, 2, 4, 3, 6, 5}, spec.rotatedDims(Direction.NORTH));
        assertArrayEquals(new int[]{1, 2, 6, 5, 3, 4}, spec.rotatedDims(Direction.EAST));
        assertArrayEquals(new int[]{1, 2, 5, 6, 4, 3}, spec.rotatedDims(Direction.WEST));
        assertArrayEquals(spec.dimensions, spec.rotatedDims(Direction.UP), "vertical facings keep the authored array");
        assertSame(spec, spec.base(), "a spec without a base is its own base");
    }

    @Test
    void everyScaledVariantResolvesToItsBaseAndCachesTheLookup() {
        int variants = 0;
        for (DoorSpec spec : DoorSpecs.ALL) {
            if (spec.baseId == null) {
                continue;
            }
            DoorSpec base = spec.base();
            assertEquals(spec.baseId, base.id, "variant " + spec.id);
            assertSame(base, spec.base(), "base lookup is cached on repeat calls");
            variants++;
        }
        assertTrue(variants > 0, "the catalog has no scaled variants, so this test proves nothing");
    }
}
