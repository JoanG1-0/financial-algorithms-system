package com.financial.report.client;

import com.financial.report.dto.PatternResponse;
import com.financial.report.dto.RiskResponse;
import com.financial.report.dto.SimilarityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Cliente HTTP que consume los endpoints del algorithm-service.
 *
 * <p>Patrón idéntico a {@code EtlServiceClient} en el algorithm-service:
 * RestTemplate + @Value para la URL base.
 */
@Component
public class AlgorithmServiceClient {

    private final RestTemplate restTemplate;
    private final String algorithmBaseUrl;
    private final String similarityPath;
    private final String riskPath;
    private final String patternsPath;

    public AlgorithmServiceClient(RestTemplate restTemplate,
                                  @Value("${algorithm.service.url}") String algorithmBaseUrl,
                                  @Value("${algorithm.service.similarity-path}") String similarityPath,
                                  @Value("${algorithm.service.risk-path}") String riskPath,
                                  @Value("${algorithm.service.patterns-path}") String patternsPath) {
        this.restTemplate = restTemplate;
        this.algorithmBaseUrl = algorithmBaseUrl;
        this.similarityPath = similarityPath;
        this.riskPath = riskPath;
        this.patternsPath = patternsPath;
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

    /**
     * Obtiene todos los perfiles de riesgo persistidos en el algorithm-service.
     *
     * @return lista de perfiles de riesgo por activo
     */
    public List<RiskResponse> fetchRiskProfiles() {
        String url = algorithmBaseUrl + riskPath;
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<RiskResponse>>() {}
        ).getBody();
    }

    /**
     * Obtiene todos los patrones detectados persistidos en el algorithm-service.
     *
     * @return lista de patrones detectados para todos los activos
     */
    public List<PatternResponse> fetchAllPatterns() {
        String url = algorithmBaseUrl + patternsPath;
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<PatternResponse>>() {}
        ).getBody();
    }
}
