package com.financial.etl.unit;

import com.financial.etl.client.TwelveDataHttpClient;
import com.financial.etl.config.TwelveDataConfig;
import com.financial.etl.exception.DataDownloadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class TwelveDataHttpClientTest {

    private RestTemplate restTemplate;
    private TwelveDataConfig config;
    private TwelveDataHttpClient client;

    @BeforeEach
    void setUp() {

        restTemplate = Mockito.mock(RestTemplate.class);
        config = Mockito.mock(TwelveDataConfig.class);

        when(config.getBaseUrl()).thenReturn("https://api.twelvedata.com");
        when(config.getApiKey()).thenReturn("fake-key");

        client = new TwelveDataHttpClient(restTemplate, config);
    }

    @Test
    void shouldReturnMockedResponse() {

        String fakeResponse = "{\"status\":\"ok\"}";

        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class)))
                .thenReturn(fakeResponse);

        String result = client.downloadTimeSeries("AAPL");

        assertEquals(fakeResponse, result);
    }

    @Test
    void shouldThrowDataDownloadExceptionWhenApiFails() {

        when(restTemplate.getForObject(anyString(), Mockito.eq(String.class)))
                .thenThrow(new RestClientException("Connection refused"));

        assertThrows(DataDownloadException.class, () -> client.downloadTimeSeries("AAPL"));
    }
}
