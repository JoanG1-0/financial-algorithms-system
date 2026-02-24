package com.financial.algorithm.indicators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimpleMovingAverageTest {

    private SimpleMovingAverage sma;

    @BeforeEach
    void setUp() {
        sma = new SimpleMovingAverage();
    }

    @Test
    void compute_returnsNaN_forFirstWMinusOnePositions() {
        double[] prices = {10, 20, 30, 40, 50};
        double[] result = sma.compute(prices, 3);

        assertTrue(Double.isNaN(result[0]));
        assertTrue(Double.isNaN(result[1]));
        assertFalse(Double.isNaN(result[2]));
    }

    @Test
    void compute_correctValues_forWindowOf3() {
        double[] prices = {10, 20, 30, 40, 50};
        double[] result = sma.compute(prices, 3);

        assertEquals(20.0, result[2], 1e-9);  // (10+20+30)/3
        assertEquals(30.0, result[3], 1e-9);  // (20+30+40)/3
        assertEquals(40.0, result[4], 1e-9);  // (30+40+50)/3
    }

    @Test
    void compute_entireArrayNaN_whenWindowLargerThanSeries() {
        double[] prices = {10, 20, 30};
        double[] result = sma.compute(prices, 5);

        for (double v : result) {
            assertTrue(Double.isNaN(v));
        }
    }

    @Test
    void compute_singleElement_windowOne_returnsValue() {
        double[] prices = {42.0};
        double[] result = sma.compute(prices, 1);

        assertEquals(42.0, result[0], 1e-9);
    }
}
