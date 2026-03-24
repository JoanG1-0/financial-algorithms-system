package com.financial.etl.controller;

import com.financial.etl.dto.BenchmarkSummaryDto;
import com.financial.etl.dto.CleanedRecordDto;
import com.financial.etl.sorting.SortingBenchmark;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/etl/sort")
public class SortingController {

    private final SortingBenchmark benchmark;

    public SortingController(SortingBenchmark benchmark) {
        this.benchmark = benchmark;
    }

    /**
     * Ejecuta las dos rondas de 12 algoritmos (fecha+close y volumen)
     * y devuelve los tiempos medidos + el Top 15 por volumen.
     */
    @GetMapping("/benchmark")
    public ResponseEntity<BenchmarkSummaryDto> runBenchmark() {
        return ResponseEntity.ok(benchmark.run());
    }

    /**
     * Devuelve únicamente los 15 registros con mayor volumen de negociación.
     */
    @GetMapping("/top15")
    public ResponseEntity<List<CleanedRecordDto>> top15() {
        return ResponseEntity.ok(benchmark.runTop15());
    }

    /**
     * Ejecuta un algoritmo concreto y devuelve los primeros {@code limit} registros
     * ordenados reales, para verificar que el orden es correcto.
     *
     * Parámetros:
     *   algorithm — nombre del algoritmo (ej. TimSort, QuickSort, HeapSort …)
     *   criterion — "date" (fecha+close ASC) o "volume" (volumen ASC)
     *   limit     — cuántos registros devolver (por defecto 20)
     *
     * Ejemplo: GET /api/etl/sort/result?algorithm=TimSort&criterion=date&limit=20
     */
    @GetMapping("/result")
    public ResponseEntity<List<CleanedRecordDto>> result(
            @RequestParam(name = "algorithm") String algorithm,
            @RequestParam(name = "criterion", defaultValue = "date") String criterion,
            @RequestParam(name = "limit",     defaultValue = "20")   int limit) {
        return ResponseEntity.ok(benchmark.sortedSample(algorithm, criterion, limit));
    }
}
