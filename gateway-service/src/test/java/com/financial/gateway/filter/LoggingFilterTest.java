package com.financial.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoggingFilterTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void loggingFilterBeanIsPresent() {
        LoggingFilter filter = applicationContext.getBean(LoggingFilter.class);
        assertNotNull(filter, "LoggingFilter bean must be registered in context");
    }

    @Test
    void loggingFilterOrderIsLowestPrecedence() {
        LoggingFilter filter = applicationContext.getBean(LoggingFilter.class);
        assertEquals(Ordered.LOWEST_PRECEDENCE, filter.getOrder(),
                "LoggingFilter must run last (LOWEST_PRECEDENCE)");
    }
}
