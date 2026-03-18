package com.financial.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void etlServiceRouteIsConfigured() {
        boolean found = routeLocator.getRoutes()
                .filter(r -> "etl-service".equals(r.getId()))
                .blockFirst() != null;
        assertTrue(found, "etl-service route must be defined");
    }

    @Test
    void algorithmServiceRouteIsConfigured() {
        boolean found = routeLocator.getRoutes()
                .filter(r -> "algorithm-service".equals(r.getId()))
                .blockFirst() != null;
        assertTrue(found, "algorithm-service route must be defined");
    }

    @Test
    void reportServiceRouteIsConfigured() {
        boolean found = routeLocator.getRoutes()
                .filter(r -> "report-service".equals(r.getId()))
                .blockFirst() != null;
        assertTrue(found, "report-service route must be defined");
    }
}
