package com.financial.report.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PatternResponseTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 24, 10, 0);

        PatternResponse response = new PatternResponse();
        response.setSymbol("TSLA");
        response.setPatternType("CONSECUTIVE_UP");
        response.setOccurrences(7);
        response.setRelativeFrequency(0.035);
        response.setComputedAt(now);

        assertEquals("TSLA", response.getSymbol());
        assertEquals("CONSECUTIVE_UP", response.getPatternType());
        assertEquals(7, response.getOccurrences());
        assertEquals(0.035, response.getRelativeFrequency(), 1e-9);
        assertEquals(now, response.getComputedAt());
    }
}
