package com.financial.report.service;

import com.financial.report.client.AlgorithmServiceClient;
import com.financial.report.correlation.CorrelationMatrixBuilder;
import com.financial.report.dto.SimilarityResponse;
import com.financial.report.entity.CorrelationEntry;
import com.financial.report.repository.CorrelationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock private AlgorithmServiceClient algorithmServiceClient;
    @Mock private CorrelationRepository correlationRepository;

    private ReportService service;

    @BeforeEach
    void setUp() {
        CorrelationMatrixBuilder builder = new CorrelationMatrixBuilder();
        service = new ReportService(algorithmServiceClient, builder, correlationRepository);
    }

    @Test
    void generateCorrelationMatrix_callsDeleteAllAndSaveAll() {
        SimilarityResponse pair = new SimilarityResponse();
        pair.setTickerA("AAA");
        pair.setTickerB("BBB");
        pair.setPearson(0.8);

        when(algorithmServiceClient.fetchSimilarities()).thenReturn(List.of(pair));
        when(correlationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        ReportService.CorrelationSummary summary = service.generateCorrelationMatrix();

        // 2 off-diagonal (A→B y B→A) + 2 diagonales = 4 entradas
        assertEquals(4, summary.entriesCount());
        verify(correlationRepository).deleteAll();
        verify(correlationRepository).saveAll(any());
    }

    @Test
    void getCorrelationMatrix_delegatesToRepository() {
        CorrelationEntry entry = new CorrelationEntry();
        entry.setAssetA("AAA");
        entry.setAssetB("BBB");
        entry.setCorrelation(0.8);
        entry.setComputedAt(LocalDateTime.now());
        when(correlationRepository.findAll()).thenReturn(List.of(entry));

        List<CorrelationEntry> result = service.getCorrelationMatrix();

        assertEquals(1, result.size());
        assertEquals("AAA", result.get(0).getAssetA());
    }
}
