package com.financial.etl.scheduler;

import com.financial.etl.service.BatchDownloadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BatchDownloadSchedulerTest {

    @Mock
    private BatchDownloadService batchDownloadService;

    @InjectMocks
    private BatchDownloadScheduler scheduler;

    @Test
    void onApplicationReady_callsDownloadAllSymbols() {
        scheduler.onApplicationReady();

        verify(batchDownloadService).downloadAllSymbols();
    }
}
