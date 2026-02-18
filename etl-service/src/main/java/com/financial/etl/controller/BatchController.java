package com.financial.etl.controller;

import com.financial.etl.entity.BatchDownloadLog;
import com.financial.etl.service.BatchDownloadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/etl/batch")
public class BatchController {

    private final BatchDownloadService batchDownloadService;

    public BatchController(BatchDownloadService batchDownloadService) {
        this.batchDownloadService = batchDownloadService;
    }

    @GetMapping("/status")
    public ResponseEntity<BatchDownloadLog> getStatus() {
        return batchDownloadService.getLastBatchStatus()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/trigger")
    public ResponseEntity<String> triggerBatch() {
        batchDownloadService.downloadAllSymbols();
        return ResponseEntity.accepted().body("Descarga batch iniciada");
    }
}
