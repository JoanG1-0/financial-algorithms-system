package com.financial.report.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimilarityResponseTest {

    @Test
    void gettersAndSetters_workCorrectly() {
        LocalDateTime now = LocalDateTime.of(2026, 2, 24, 10, 0);

        SimilarityResponse response = new SimilarityResponse();
        response.setTickerA("AAPL");
        response.setTickerB("MSFT");
        response.setEuclidean(0.12);
        response.setPearson(0.85);
        response.setDtw(3.5);
        response.setCosine(0.92);
        response.setComputedAt(now);

        assertEquals("AAPL", response.getTickerA());
        assertEquals("MSFT", response.getTickerB());
        assertEquals(0.12, response.getEuclidean(), 1e-9);
        assertEquals(0.85, response.getPearson(), 1e-9);
        assertEquals(3.5, response.getDtw(), 1e-9);
        assertEquals(0.92, response.getCosine(), 1e-9);
        assertEquals(now, response.getComputedAt());
    }
}
