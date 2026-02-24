package com.financial.report.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RiskResponseTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 24, 10, 0);

        RiskResponse response = new RiskResponse();
        response.setTicker("AAPL");
        response.setAnnualizedVolatility(0.2345);
        response.setCategory("MODERATE");
        response.setComputedAt(now);

        assertEquals("AAPL", response.getTicker());
        assertEquals(0.2345, response.getAnnualizedVolatility(), 1e-9);
        assertEquals("MODERATE", response.getCategory());
        assertEquals(now, response.getComputedAt());
    }
}
