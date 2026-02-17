package com.financial.etl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwelveDataConfig {

    @Value("${twelvedata.base-url}")
    private String baseUrl;

    @Value("${twelvedata.api-key}")
    private String apiKey;

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }
}
