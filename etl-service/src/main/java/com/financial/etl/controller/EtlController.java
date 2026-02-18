package com.financial.etl.controller;

import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.service.EtlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/etl")
public class EtlController {

    private final EtlService etlService;

    public EtlController(EtlService etlService) {
        this.etlService = etlService;
    }

    @PostMapping("/extract")
    public ResponseEntity<FinancialSeries> extract(@RequestParam("symbol") String symbol) {
        FinancialSeries saved = etlService.extractAndLoad(symbol);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/series/{symbol}")
    public ResponseEntity<List<FinancialSeries>> getSeries(@PathVariable("symbol") String symbol) {
        return ResponseEntity.ok(etlService.findBySymbol(symbol));
    }
}
