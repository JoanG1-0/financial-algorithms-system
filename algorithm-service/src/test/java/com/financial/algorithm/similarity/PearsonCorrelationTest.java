package com.financial.algorithm.similarity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PearsonCorrelationTest {

    private PearsonCorrelation pearson;

    @BeforeEach
    void setUp() {
        pearson = new PearsonCorrelation();
    }

    @Test
    void compute_perfectPositiveCorrelation_returnsOne() {
        double[] x = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] y = {2.0, 4.0, 6.0, 8.0, 10.0};
        assertEquals(1.0, pearson.compute(x, y), 1e-9);
    }

    @Test
    void compute_perfectNegativeCorrelation_returnsMinusOne() {
        double[] x = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] y = {5.0, 4.0, 3.0, 2.0, 1.0};
        assertEquals(-1.0, pearson.compute(x, y), 1e-9);
    }

    @Test
    void compute_constantSeries_returnsNaN() {
        double[] x = {1.0, 1.0, 1.0, 1.0};
        double[] y = {1.0, 2.0, 3.0, 4.0};
        assertTrue(Double.isNaN(pearson.compute(x, y)));
    }

    @Test
    void compute_differentLengths_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> pearson.compute(new double[]{1.0, 2.0}, new double[]{1.0}));
    }
}
