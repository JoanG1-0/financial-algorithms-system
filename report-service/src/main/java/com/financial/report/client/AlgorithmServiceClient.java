package com.financial.report.client;

import com.financial.report.dto.SimilarityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Cliente HTTP que consume el endpoint de similitud del algorithm-service.
 *
 * <p>Realiza GET a {@code {algorithmUrl}/api/algorithm/similarity} y
 * deserializa la respuesta como {@code List<SimilarityResponse>}.
 *
 * <p>Patrón idéntico a {@code EtlServiceClient} en el algorithm-service:
 * RestTemplate + @Value para la URL base.
 */
@Component
public class AlgorithmServiceClient {

    private final RestTemplate restTemplate;
    private final String algorithmBaseUrl;
    private final String similarityPath;

    public AlgorithmServiceClient(RestTemplate restTemplate,
                                  @Value("${algorithm.service.url}") String algorithmBaseUrl,
                                  @Value("${algorithm.service.similarity-path}") String similarityPath) {
        this.restTemplate = restTemplate;
        this.algorithmBaseUrl = algorithmBaseUrl;
        this.similarityPath = similarityPath;
    }

    /**
     * Obtiene todos los resultados de similitud persistidos en el algorithm-service.
     *
     * @return lista de resultados de similitud entre pares de activos
     */
    public List<SimilarityResponse> fetchSimilarities() {
        String url = algorithmBaseUrl + similarityPath;
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SimilarityResponse>>() {}
        ).getBody();
    }
}
