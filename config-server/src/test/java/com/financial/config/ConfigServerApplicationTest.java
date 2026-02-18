package com.financial.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigServerApplicationTest {

    @Test
    void contextLoads() {
        assertNotNull(new ConfigServerApplication());
    }
}
