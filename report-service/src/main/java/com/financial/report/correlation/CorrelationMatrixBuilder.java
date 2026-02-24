package com.financial.report.correlation;

import com.financial.report.dto.SimilarityResponse;
import com.financial.report.entity.CorrelationEntry;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Componente que construye la lista de entradas de la matriz de correlación de Pearson
 * a partir de los resultados de similitud del algorithm-service.
 *
 * <p>Extrae el campo {@code pearson} de cada par y añade entradas de diagonal
 * (assetA == assetB, correlación == 1.0). Lógica pura, sin dependencias de BD ni HTTP.
 */
@Component
public class CorrelationMatrixBuilder {

    /**
     * Construye la lista completa de entradas de correlación incluyendo la diagonal.
     *
     * @param similarities lista de similitudes entre pares de activos
     * @return lista de {@link CorrelationEntry} (pares off-diagonal + diagonal)
     */
    public List<CorrelationEntry> build(List<SimilarityResponse> similarities) {
        LocalDateTime now = LocalDateTime.now();

        // Recolectar tickers únicos manteniendo orden de aparición
        Set<String> tickers = new LinkedHashSet<>();
        for (SimilarityResponse s : similarities) {
            tickers.add(s.getTickerA());
            tickers.add(s.getTickerB());
        }

        List<CorrelationEntry> entries = new ArrayList<>();

        // Entradas off-diagonal: se incluyen ambas direcciones (A→B y B→A) para
        // representar la matriz simétrica completa (n×n con n activos)
        for (SimilarityResponse s : similarities) {
            CorrelationEntry ab = new CorrelationEntry();
            ab.setAssetA(s.getTickerA());
            ab.setAssetB(s.getTickerB());
            ab.setCorrelation(s.getPearson());
            ab.setComputedAt(now);
            entries.add(ab);

            CorrelationEntry ba = new CorrelationEntry();
            ba.setAssetA(s.getTickerB());
            ba.setAssetB(s.getTickerA());
            ba.setCorrelation(s.getPearson());
            ba.setComputedAt(now);
            entries.add(ba);
        }

        // Entradas de diagonal: correlación consigo mismo == 1.0
        for (String ticker : tickers) {
            CorrelationEntry diag = new CorrelationEntry();
            diag.setAssetA(ticker);
            diag.setAssetB(ticker);
            diag.setCorrelation(1.0);
            diag.setComputedAt(now);
            entries.add(diag);
        }

        return entries;
    }
}
