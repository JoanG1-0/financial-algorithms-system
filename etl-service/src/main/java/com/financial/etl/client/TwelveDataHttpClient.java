package com.financial.etl.client;

import com.financial.etl.config.TwelveDataConfig;
import com.financial.etl.exception.DataDownloadException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Component
public class TwelveDataHttpClient {

    private final RestTemplate restTemplate;
    private final TwelveDataConfig config;

    public TwelveDataHttpClient(RestTemplate restTemplate,
                                TwelveDataConfig config) {
        this.restTemplate = restTemplate;
        this.config = config;
    }

    public String downloadTimeSeries(String symbol) {

        String startDate = LocalDate.now().minusYears(5).toString();
        String endDate   = LocalDate.now().toString();

        String url = String.format(
                "%s/time_series?symbol=%s&interval=1day&start_date=%s&end_date=%s&apikey=%s",
                config.getBaseUrl(),
                symbol,
                startDate,
                endDate,
                config.getApiKey()
        );

        try {
            return restTemplate.getForObject(url, String.class);
        } catch (RestClientException e) {
            throw new DataDownloadException(
                    "Error descargando datos de TwelveData para el símbolo: " + symbol, e);
        }
    }
}
