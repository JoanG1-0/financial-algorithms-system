package com.financial.etl.controller;

import com.financial.etl.dto.TransformSummary;
import com.financial.etl.entity.DataQuality;
import com.financial.etl.repository.CleanedRecordRepository;
import com.financial.etl.transform.DataTransformService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
     * Triggers the full transformation pipeline.
     *
     * @return 202 Accepted with TransformSummary
     */
    @PostMapping
    public ResponseEntity<TransformSummary> transform() {
        TransformSummary summary = transformService.transformAll();
        return ResponseEntity.accepted().body(summary);
    }

    /**
     * Returns counts of CleanedRecord rows grouped by DataQuality.
     *
     * @return 200 OK with map of quality → count
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
}
