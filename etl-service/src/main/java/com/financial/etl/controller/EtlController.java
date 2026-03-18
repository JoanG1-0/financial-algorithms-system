package com.financial.etl.controller;

import com.financial.etl.entity.CleanedRecord;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.entity.PriceRecord;
import com.financial.etl.service.EtlService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/assets")
    public ResponseEntity<List<FinancialSeries>> getAllAssets() {
        return ResponseEntity.ok(etlService.findAllAssets());
    }

    @GetMapping("/series/{symbol}/prices")
    public ResponseEntity<Page<PriceRecord>> getPrices(
            @PathVariable("symbol") String symbol,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        return ResponseEntity.ok(etlService.findPricesBySymbol(symbol, PageRequest.of(page, size)));
    }

    @GetMapping("/series/{symbol}/cleaned")
    public ResponseEntity<Page<CleanedRecord>> getCleaned(
            @PathVariable("symbol") String symbol,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "30") int size) {
        return ResponseEntity.ok(etlService.findCleanedBySymbol(symbol, PageRequest.of(page, size)));
    }
}
