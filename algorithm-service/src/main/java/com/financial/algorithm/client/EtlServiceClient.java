package com.financial.algorithm.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Cliente HTTP que consume el endpoint de precios limpios del ETL service.
 *
 * <p>Realiza GET a {@code {etlBaseUrl}/api/etl/transform/cleaned-prices} y
 * deserializa la respuesta como {@code Map<String, double[]>}.
 *
 * <p>Patrón idéntico a {@code TwelveDataHttpClient} en el ETL service:
 * RestTemplate + @Value para la URL base.
 */
@Component
public class EtlServiceClient {

    private static final String CLEANED_PRICES_PATH = "/api/etl/transform/cleaned-prices";

    private final RestTemplate restTemplate;
    private final String etlBaseUrl;

    public EtlServiceClient(RestTemplate restTemplate,
                            @Value("${etl.service.url}") String etlBaseUrl) {
        this.restTemplate = restTemplate;
        this.etlBaseUrl = etlBaseUrl;
    }

    /**
     * Obtiene los precios de cierre limpios de todos los activos del ETL service.
     *
     * @return mapa de ticker → array de precios de cierre en orden cronológico
     */
    public Map<String, double[]> fetchCleanedPrices() {
        String url = etlBaseUrl + CLEANED_PRICES_PATH;
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<Map<String, double[]>>() {}
        ).getBody();
    }
}
