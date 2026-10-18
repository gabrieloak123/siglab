package br.ufrn.dimap.siglab.repository;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VectorRepositoryTest {
    @Test void rejectsInvalidDimensionsAndNonFiniteValues() {
        assertThrows(IllegalArgumentException.class, () -> VectorRepository.literal(new float[2]));
        float[] vector = new float[1024]; vector[7] = Float.NaN;
        assertThrows(IllegalArgumentException.class, () -> VectorRepository.literal(vector));
    }
    @Test void formatsValidVectorForPgvector() {
        float[] vector = new float[1024]; vector[0] = 0.25f;
        assertTrue(VectorRepository.literal(vector).startsWith("[0.25,0.0,"));
    }
}
