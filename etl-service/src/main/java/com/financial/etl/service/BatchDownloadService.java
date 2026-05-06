package com.financial.etl.service;

import com.financial.etl.entity.BatchDownloadLog;
import com.financial.etl.entity.BatchStatus;
import com.financial.etl.repository.BatchDownloadLogRepository;
import com.financial.etl.transform.DataTransformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BatchDownloadService {

    private static final Logger log = LoggerFactory.getLogger(BatchDownloadService.class);

    @Value("${batch.symbols}")
    private List<String> symbols;

    @Value("${batch.rate-limit.max-per-minute:8}")
    private int maxPerMinute;

    @Value("${batch.rate-limit.pause-ms:60000}")
    private long pauseMs;

    private final EtlService etlService;
    private final BatchDownloadLogRepository logRepository;
    private final DataTransformService dataTransformService;

    public BatchDownloadService(EtlService etlService, BatchDownloadLogRepository logRepository,
                                DataTransformService dataTransformService) {
        this.etlService = etlService;
        this.logRepository = logRepository;
        this.dataTransformService = dataTransformService;
    }

    @Async("batchTaskExecutor")
    public void downloadAllSymbols() {
        if (isAlreadyRunningOrCompleted()) {
            log.info("Batch ya ejecutado o en progreso hoy ({}). Saltando descarga.", LocalDate.now());
            runTransform();
            return;
        }

        log.info("Iniciando descarga batch de {} activos financieros.", symbols.size());
        BatchDownloadLog batchLog = initializeBatchLog();

        try {
            List<List<String>> batches = partitionSymbols(symbols, maxPerMinute);
            log.info("Activos divididos en {} grupo(s) de máximo {} símbolos.", batches.size(), maxPerMinute);

            for (int i = 0; i < batches.size(); i++) {
                log.info("Procesando grupo {}/{}: {}", i + 1, batches.size(), batches.get(i));
                processBatch(batches.get(i), batchLog);

                if (i < batches.size() - 1) {
                    pauseBetweenBatches();
                }
            }

            finalizeBatchLog(batchLog);
            runTransform();

        } catch (Exception e) {
            markBatchAsFailed(batchLog, e.getMessage());
            log.error("Error fatal durante la descarga batch", e);
        }
    }

    public Optional<BatchDownloadLog> getLastBatchStatus() {
        return logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now());
    }

    private void runTransform() {
        log.info("Iniciando transformación automática de datos financieros...");
        try {
            dataTransformService.transformAll();
            log.info("Transformación automática completada.");
        } catch (Exception e) {
            log.error("Error durante la transformación automática", e);
        }
    }

    private boolean isAlreadyRunningOrCompleted() {
        return logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now())
                .map(bl -> bl.getStatus() == BatchStatus.COMPLETED
                        || bl.getStatus() == BatchStatus.COMPLETED_WITH_ERRORS
                        || bl.getStatus() == BatchStatus.IN_PROGRESS)
                .orElse(false);
    }

    private BatchDownloadLog initializeBatchLog() {
        BatchDownloadLog batchLog = new BatchDownloadLog();
        batchLog.setDownloadDate(LocalDate.now());
        batchLog.setStatus(BatchStatus.IN_PROGRESS);
        batchLog.setStartedAt(LocalDateTime.now());
        batchLog.setTotalSymbols(symbols.size());
        batchLog.setDownloadedSymbols(0);
        batchLog.setFailedSymbols(0);
        return logRepository.save(batchLog);
    }

    private void processBatch(List<String> batch, BatchDownloadLog batchLog) {
        for (String symbol : batch) {
            try {
                etlService.extractAndLoad(symbol, batchLog.getId());
                batchLog.setDownloadedSymbols(batchLog.getDownloadedSymbols() + 1);
                log.info("Símbolo {} descargado correctamente. ({}/{})",
                        symbol, batchLog.getDownloadedSymbols(), batchLog.getTotalSymbols());
            } catch (Exception e) {
                batchLog.setFailedSymbols(batchLog.getFailedSymbols() + 1);
                log.error("Error descargando símbolo {}: {}", symbol, e.getMessage());
            }
            logRepository.save(batchLog);
        }
    }

    private void pauseBetweenBatches() {
        log.info("Pausa de {}ms entre grupos para respetar el límite de la API...", pauseMs);
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Pausa entre grupos interrumpida.");
        }
    }

    private void finalizeBatchLog(BatchDownloadLog batchLog) {
        batchLog.setCompletedAt(LocalDateTime.now());
        batchLog.setStatus(batchLog.getFailedSymbols() == 0
                ? BatchStatus.COMPLETED
                : BatchStatus.COMPLETED_WITH_ERRORS);
        logRepository.save(batchLog);
        log.info("Batch finalizado: {}/{} símbolos descargados, {} fallidos.",
                batchLog.getDownloadedSymbols(), batchLog.getTotalSymbols(), batchLog.getFailedSymbols());
    }

    private void markBatchAsFailed(BatchDownloadLog batchLog, String errorMessage) {
        batchLog.setStatus(BatchStatus.FAILED);
        batchLog.setCompletedAt(LocalDateTime.now());
        batchLog.setErrorMessage(errorMessage);
        logRepository.save(batchLog);
    }

    private <T> List<List<T>> partitionSymbols(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }
}
