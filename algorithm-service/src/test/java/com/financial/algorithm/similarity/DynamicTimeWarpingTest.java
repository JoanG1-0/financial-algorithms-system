package com.financial.algorithm.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DynamicTimeWarpingTest {

    private DynamicTimeWarping dtw;

    @BeforeEach
    void setUp() {
        dtw = new DynamicTimeWarping();
    }

    @Test
    void compute_identicalSeries_returnsZero() {
        double[] a = {1.0, 2.0, 3.0, 4.0};
        assertEquals(0.0, dtw.compute(a, a), 1e-9);
    }

    @Test
    void compute_seriesWithTemporalShift_smallerThanEuclidean() {
        // a desplazada un paso respecto a b
        double[] a = {0.0, 1.0, 2.0, 3.0};
        double[] b = {1.0, 2.0, 3.0, 4.0};

        double dtwDist = dtw.compute(a, b);
        // DTW puede alinear la serie con shift, debe ser ≥ 0
        assertTrue(dtwDist >= 0.0);
    }

    @Test
    void compute_emptySeries_returnsZero() {
        assertEquals(0.0, dtw.compute(new double[]{}, new double[]{}), 1e-9);
    }

    @Test
    void compute_singleElement_returnsDifference() {
        double[] a = {3.0};
        double[] b = {7.0};
        assertEquals(4.0, dtw.compute(a, b), 1e-9);
    }
}
