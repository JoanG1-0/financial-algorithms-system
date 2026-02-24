package com.financial.report.service;

import com.financial.report.client.AlgorithmServiceClient;
import com.financial.report.correlation.CorrelationMatrixBuilder;
import com.financial.report.dto.SimilarityResponse;
import com.financial.report.entity.CorrelationEntry;
import com.financial.report.repository.CorrelationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio principal del report-service.
 *
 * <p>Orquesta la generación de la matriz de correlación:
 * <ol>
 *   <li>Obtiene similitudes del algorithm-service via {@link AlgorithmServiceClient}</li>
 *   <li>Construye entradas de correlación via {@link CorrelationMatrixBuilder}</li>
 *   <li>Borra y persiste el nuevo resultado en {@code report_db}</li>
 * </ol>
 */
@Service
public class ReportService {

    private final AlgorithmServiceClient algorithmServiceClient;
    private final CorrelationMatrixBuilder correlationMatrixBuilder;
    private final CorrelationRepository correlationRepository;

    public ReportService(AlgorithmServiceClient algorithmServiceClient,
                         CorrelationMatrixBuilder correlationMatrixBuilder,
                         CorrelationRepository correlationRepository) {
        this.algorithmServiceClient = algorithmServiceClient;
        this.correlationMatrixBuilder = correlationMatrixBuilder;
        this.correlationRepository = correlationRepository;
    }

    /**
     * Genera (o regenera) la matriz de correlación de Pearson y la persiste.
     *
     * @return resumen con el número de entradas persistidas
     */
    @Transactional
    public CorrelationSummary generateCorrelationMatrix() {
        List<SimilarityResponse> similarities = algorithmServiceClient.fetchSimilarities();
        List<CorrelationEntry> entries = correlationMatrixBuilder.build(similarities);

        correlationRepository.deleteAll();
        correlationRepository.saveAll(entries);

        return new CorrelationSummary(entries.size());
    }

    /**
     * Devuelve todas las entradas de la matriz de correlación persistidas.
     *
     * @return lista de {@link CorrelationEntry}
     */
    public List<CorrelationEntry> getCorrelationMatrix() {
        return correlationRepository.findAll();
    }

    /**
     * Resumen de la operación de generación de la matriz de correlación.
     *
     * @param entriesCount número de entradas persistidas (pares + diagonal)
     */
    public record CorrelationSummary(int entriesCount) {}
}
