package com.financial.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.cors.reactive.CorsWebFilter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CorsConfigTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void corsWebFilterBeanIsPresent() {
        CorsWebFilter filter = applicationContext.getBean(CorsWebFilter.class);
        assertNotNull(filter, "CorsWebFilter bean must be registered in context");
    }
}
