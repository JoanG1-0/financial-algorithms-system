package com.financial.algorithm.patterns;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowTest {

    private SlidingWindow slidingWindow;

    @BeforeEach
    void setUp() {
        slidingWindow = new SlidingWindow();
    }

    @Test
    void extract_n5_k3_returns3Windows() {
        double[] series = {1.0, 2.0, 3.0, 4.0, 5.0};
        List<SlidingWindow.WindowSlice> windows = slidingWindow.extract(series, 3);

        assertEquals(3, windows.size());
        assertEquals(0, windows.get(0).startIndex());
        assertEquals(1, windows.get(1).startIndex());
        assertEquals(2, windows.get(2).startIndex());
    }

    @Test
    void extract_kGreaterThanN_returnsEmpty() {
        double[] series = {1.0, 2.0, 3.0};
        List<SlidingWindow.WindowSlice> result = slidingWindow.extract(series, 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void extract_kEqualsN_returns1Window() {
        double[] series = {10.0, 20.0, 30.0};
        List<SlidingWindow.WindowSlice> result = slidingWindow.extract(series, 3);

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).startIndex());
        assertEquals(3, result.get(0).values().length);
    }

    @Test
    void extract_k1_returnsNWindows() {
        double[] series = {1.0, 2.0, 3.0, 4.0};
        List<SlidingWindow.WindowSlice> result = slidingWindow.extract(series, 1);
        assertEquals(4, result.size());
    }
}
