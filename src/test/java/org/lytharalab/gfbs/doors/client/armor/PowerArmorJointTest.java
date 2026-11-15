package org.lytharalab.gfbs.doors.client.armor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the joint transform: the mesh is authored in absolute biped pixels, so a part is rotated about its
 * <em>rest</em> pivot and translated to wherever an animation moved the pivot. Using the moved pivot on both
 * sides cancels the translation and detaches the limb - the bug TaCZ's shoulder-swinging gun poses exposed.
 */
class PowerArmorJointTest {
    private static final float EPSILON = 1.0E-5F;

    @Test
    void aRestPosePartEndsUpUntranslated() {
        float[] matrix = PowerArmorLayer.localMatrix(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F,
            PowerArmorModels.Part.LEFT_ARM);
        assertEquals(0.0F, matrix[12], EPSILON);
        assertEquals(0.0F, matrix[13], EPSILON);
        assertEquals(0.0F, matrix[14], EPSILON);
    }

    @Test
    void aMovedPivotTranslatesTheMeshByTheDifference() {
        // TaCZ swings the shoulder around the body by rewriting x/z; the arm must travel with it.
        float[] matrix = PowerArmorLayer.localMatrix(8.0F, 2.0F, -3.0F, 0.0F, 0.0F, 0.0F,
            PowerArmorModels.Part.LEFT_ARM);
        assertEquals(3.0F, matrix[12], EPSILON, "8 - rest 5");
        assertEquals(0.0F, matrix[13], EPSILON, "2 - rest 2");
        assertEquals(-3.0F, matrix[14], EPSILON, "-3 - rest 0");
    }

    @Test
    void rotationHappensAroundTheRestPivot() {
        float angle = 1.0F;
        float[] matrix = PowerArmorLayer.localMatrix(5.0F, 2.0F, 0.0F, 0.0F, 0.0F, angle,
            PowerArmorModels.Part.LEFT_ARM);
        // Rotating (5, 2, 0) about the x axis and subtracting it again: the x stays, the other two swing.
        assertEquals(1.0F, matrix[0], EPSILON, "m00 is untouched by an x rotation");
        assertEquals(0.0F, matrix[12], EPSILON, "the pivot's x does not move");
        assertEquals(2.0F - 2.0F * (float) Math.cos(angle), matrix[13], EPSILON);
        assertEquals(-2.0F * (float) Math.sin(angle), matrix[14], EPSILON);
    }
}
