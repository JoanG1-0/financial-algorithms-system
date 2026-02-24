package com.financial.algorithm.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EuclideanDistanceTest {

    private EuclideanDistance euclidean;

    @BeforeEach
    void setUp() {
        euclidean = new EuclideanDistance();
    }

    @Test
    void compute_identicalSeries_returnsZero() {
        double[] a = {0.01, -0.02, 0.03, 0.01};
        assertEquals(0.0, euclidean.compute(a, a), 1e-9);
    }

    @Test
    void compute_knownValues_returnsCorrectDistance() {
        double[] a = {0.0, 0.0};
        double[] b = {3.0, 4.0};
        // sqrt(9 + 16) = 5
        assertEquals(5.0, euclidean.compute(a, b), 1e-9);
    }

    @Test
    void compute_emptySeries_returnsZero() {
        assertEquals(0.0, euclidean.compute(new double[]{}, new double[]{}), 1e-9);
    }

    @Test
    void compute_differentLengths_throwsException() {
        double[] a = {1.0, 2.0};
        double[] b = {1.0};
        assertThrows(IllegalArgumentException.class, () -> euclidean.compute(a, b));
    }
}
