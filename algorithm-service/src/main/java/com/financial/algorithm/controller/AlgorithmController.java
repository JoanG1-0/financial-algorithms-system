package com.financial.algorithm.controller;

import com.financial.algorithm.entity.PatternRecord;
import com.financial.algorithm.entity.RiskRecord;
import com.financial.algorithm.entity.SimilarityRecord;
import com.financial.algorithm.entity.SmaRecord;
import com.financial.algorithm.service.AlgorithmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del algorithm-service.
 *
 * <p>Expone los endpoints para disparar el análisis completo y consultar
 * los resultados persistidos por categoría.
 *
 * <table border="1">
 *   <tr><th>Método</th><th>Endpoint</th><th>Descripción</th><th>Status</th></tr>
 *   <tr><td>POST</td><td>/api/algorithm/run</td><td>Dispara análisis completo</td><td>202</td></tr>
 *   <tr><td>GET</td><td>/api/algorithm/similarity</td><td>Matriz de similitud</td><td>200</td></tr>
 *   <tr><td>GET</td><td>/api/algorithm/risk</td><td>Ranking de riesgo</td><td>200</td></tr>
 *   <tr><td>GET</td><td>/api/algorithm/patterns/{symbol}</td><td>Patrones de un activo</td><td>200</td></tr>
 *   <tr><td>GET</td><td>/api/algorithm/indicators/{symbol}/sma</td><td>SMA con ?window=</td><td>200</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/algorithm")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /**
     * Dispara el análisis completo de forma síncrona.
     *
     * @return 202 Accepted con el resumen de registros persistidos
     */
    @PostMapping("/run")
    public ResponseEntity<AlgorithmService.AnalysisSummary> run() {
        AlgorithmService.AnalysisSummary summary = algorithmService.runFullAnalysis();
        return ResponseEntity.accepted().body(summary);
    }

    /**
     * Devuelve todos los resultados de similitud del último análisis.
     *
     * @return 200 OK con lista de {@link SimilarityRecord}
     */
    @GetMapping("/similarity")
    public ResponseEntity<List<SimilarityRecord>> similarity() {
        return ResponseEntity.ok(algorithmService.getSimilarityResults());
    }

    /**
     * Devuelve el ranking de riesgo ordenado ascendentemente por volatilidad.
     *
     * @return 200 OK con lista de {@link RiskRecord}
     */
    @GetMapping("/risk")
    public ResponseEntity<List<RiskRecord>> risk() {
        return ResponseEntity.ok(algorithmService.getRiskResults());
    }

    /**
     * Devuelve los patrones detectados para un activo concreto.
     *
     * @param symbol ticker del activo (e.g. "AAPL")
     * @return 200 OK con lista de {@link PatternRecord}, o 404 si no hay datos
     */
    @GetMapping("/patterns/{symbol}")
    public ResponseEntity<List<PatternRecord>> patterns(@PathVariable("symbol") String symbol) {
        List<PatternRecord> records = algorithmService.getPatternsBySymbol(symbol);
        if (records.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(records);
    }

    /**
     * Devuelve el registro SMA más reciente para un activo y ventana.
     *
     * @param symbol ticker del activo
     * @param window tamaño de la ventana SMA (por defecto 20)
     * @return 200 OK con {@link SmaRecord}, o 404 si no hay datos
     */
    @GetMapping("/indicators/{symbol}/sma")
    public ResponseEntity<SmaRecord> sma(@PathVariable("symbol") String symbol,
                                          @RequestParam(name = "window", defaultValue = "20") int window) {
        SmaRecord record = algorithmService.getSma(symbol, window);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }
}
