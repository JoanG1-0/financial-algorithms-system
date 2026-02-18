package com.financial.etl.scheduler;

import com.financial.etl.service.BatchDownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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
}
