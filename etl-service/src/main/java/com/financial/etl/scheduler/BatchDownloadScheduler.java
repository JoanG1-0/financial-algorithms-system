package com.financial.etl.scheduler;

import com.financial.etl.service.BatchDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BatchDownloadScheduler {

    private static final Logger log = LoggerFactory.getLogger(BatchDownloadScheduler.class);

    private final BatchDownloadService batchDownloadService;

    public BatchDownloadScheduler(BatchDownloadService batchDownloadService) {
        this.batchDownloadService = batchDownloadService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Aplicación lista. Iniciando verificación de descarga batch diaria de activos financieros...");
        batchDownloadService.downloadAllSymbols();
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI")
    public void onMarketClose() {
        log.info("Scheduler diario activado. Iniciando descarga y transformación de datos del día...");
        batchDownloadService.downloadAllSymbols();
    }
}
