package com.financial.etl.controller;

import com.financial.etl.dto.TransformSummary;
import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.repository.CleanedRecordRepository;
import com.financial.etl.transform.DataTransformService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/etl/transform")
public class TransformController {

    private final DataTransformService transformService;
    private final CleanedRecordRepository cleanedRecordRepository;

    public TransformController(DataTransformService transformService,
                               CleanedRecordRepository cleanedRecordRepository) {
        this.transformService = transformService;
        this.cleanedRecordRepository = cleanedRecordRepository;
    }

    /**
     * Ejecuta el pipeline completo de transformación.
     *
     * @return 202 Accepted con TransformSummary
     */
    @PostMapping
    public ResponseEntity<TransformSummary> transform() {
        TransformSummary summary = transformService.transformAll();
        return ResponseEntity.accepted().body(summary);
    }

    /**
     * Devuelve el conteo de registros CleanedRecord agrupados por DataQuality.
     *
     * @return 200 OK con mapa de calidad → conteo
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Long>> status() {
        List<CleanedRecordRepository.QualityCount> counts =
                cleanedRecordRepository.countByDataQuality();

        Map<String, Long> result = new LinkedHashMap<>();
        for (DataQuality dq : DataQuality.values()) {
            result.put(dq.name(), 0L);
        }
        for (CleanedRecordRepository.QualityCount qc : counts) {
            result.put(qc.getQuality().name(), qc.getTotal());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve el conteo de registros rellenos (FORWARD_FILLED) por activo.
     * Permite verificar cuántas fechas faltaban en cada serie antes del relleno.
     *
     * @return 200 OK con mapa de símbolo → cantidad de registros FORWARD_FILLED
     */
    @GetMapping("/missing-per-symbol")
    public ResponseEntity<Map<String, Long>> missingPerSymbol() {
        Map<String, Long> result = new LinkedHashMap<>();
        for (CleanedRecordRepository.SymbolMissingCount smc :
                cleanedRecordRepository.countForwardFilledPerSymbol()) {
            result.put(smc.getSymbol(), smc.getTotal());
        }
        return ResponseEntity.ok(result);
    }

    /**
     * Devuelve los precios de cierre agrupados por ticker para consumo del algorithm-service.
     *
     * <p>Solo incluye registros con calidad CLEAN o FORWARD_FILLED, ordenados por fecha ASC.
     *
     * @return 200 OK con mapa de ticker → double[] de precios de cierre
     */
    @GetMapping("/cleaned-prices")
    public ResponseEntity<Map<String, double[]>> cleanedPrices() {
        List<DataQuality> usable = List.of(DataQuality.CLEAN, DataQuality.FORWARD_FILLED);
        List<CleanedRecord> records =
                cleanedRecordRepository.findByDataQualityInOrderBySymbolAndDate(usable);

        Map<String, List<Double>> grouped = new TreeMap<>();
        for (CleanedRecord r : records) {
            grouped.computeIfAbsent(r.getSymbol(), k -> new ArrayList<>())
                   .add(r.getClose().doubleValue());
        }

        Map<String, double[]> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : grouped.entrySet()) {
            List<Double> prices = entry.getValue();
            double[] arr = new double[prices.size()];
            for (int i = 0; i < prices.size(); i++) {
                arr[i] = prices.get(i);
            }
            result.put(entry.getKey(), arr);
        }

        return ResponseEntity.ok(result);
    }
}
