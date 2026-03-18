package com.financial.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalErrorHandlerTest {

    private GlobalErrorHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalErrorHandler();
    }

    @Test
    void handle_returnsServiceUnavailable_forGenericException() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/etl/transform").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new RuntimeException("connection refused")).block();

        assertEquals(503, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void handle_returnsNotFound_forResponseStatusException404() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/algorithm/run").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)).block();

        assertEquals(404, exchange.getResponse().getStatusCode().value());
    }

    @Test
    void handle_returnsInternalServerError_forResponseStatusException500() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/reports/correlation-matrix").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR)).block();

        assertEquals(500, exchange.getResponse().getStatusCode().value());
    }
}
