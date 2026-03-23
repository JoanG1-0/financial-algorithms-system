package com.financial.etl.controller;

import com.financial.etl.dto.BenchmarkSummaryDto;
import com.financial.etl.dto.CleanedRecordDto;
import com.financial.etl.sorting.SortingBenchmark;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
