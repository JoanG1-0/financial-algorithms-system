package com.financial.algorithm.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CosineSimilarityTest {

    private CosineSimilarity cosine;

    @BeforeEach
    void setUp() {
        cosine = new CosineSimilarity();
    }

    @Test
    void compute_parallelVectors_returnsOne() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {2.0, 4.0, 6.0};
        assertEquals(1.0, cosine.compute(x, y), 1e-9);
    }

    @Test
    void compute_oppositeVectors_returnsMinusOne() {
        double[] x = {1.0, 2.0, 3.0};
        double[] y = {-1.0, -2.0, -3.0};
        assertEquals(-1.0, cosine.compute(x, y), 1e-9);
    }

    @Test
    void compute_orthogonalVectors_returnsZero() {
        double[] x = {1.0, 0.0};
        double[] y = {0.0, 1.0};
        assertEquals(0.0, cosine.compute(x, y), 1e-9);
    }

    @Test
    void compute_zeroVector_returnsNaN() {
        double[] x = {0.0, 0.0};
        double[] y = {1.0, 2.0};
        assertTrue(Double.isNaN(cosine.compute(x, y)));
    }
}
