package com.financial.etl.service;

import com.financial.etl.entity.BatchDownloadLog;
import com.financial.etl.entity.BatchStatus;
import com.financial.etl.entity.FinancialSeries;
import com.financial.etl.exception.DataDownloadException;
import com.financial.etl.repository.BatchDownloadLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BatchDownloadServiceTest {

    @Mock
    private EtlService etlService;

    @Mock
    private BatchDownloadLogRepository logRepository;

    @InjectMocks
    private BatchDownloadService batchDownloadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(batchDownloadService, "symbols", List.of("AAPL", "MSFT", "GOOGL"));
        ReflectionTestUtils.setField(batchDownloadService, "maxPerMinute", 8);
        ReflectionTestUtils.setField(batchDownloadService, "pauseMs", 0L);
    }

    @Test
    void downloadAllSymbols_skipsIfAlreadyCompletedToday() {
        BatchDownloadLog existing = new BatchDownloadLog();
        existing.setStatus(BatchStatus.COMPLETED);
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.of(existing));

        batchDownloadService.downloadAllSymbols();

        verify(logRepository, never()).save(any(BatchDownloadLog.class));
        verify(etlService, never()).extractAndLoad(anyString(), anyLong());
    }

    @Test
    void downloadAllSymbols_skipsIfAlreadyInProgressToday() {
        BatchDownloadLog existing = new BatchDownloadLog();
        existing.setStatus(BatchStatus.IN_PROGRESS);
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.of(existing));

        batchDownloadService.downloadAllSymbols();

        verify(logRepository, never()).save(any(BatchDownloadLog.class));
        verify(etlService, never()).extractAndLoad(anyString(), anyLong());
    }

    @Test
    void downloadAllSymbols_downloadsAllSymbolsWhenNoLogExists() {
        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setStatus(BatchStatus.IN_PROGRESS);
        savedLog.setTotalSymbols(3);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);

        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any(BatchDownloadLog.class))).thenReturn(savedLog);
        when(etlService.extractAndLoad(anyString(), eq(1L))).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        verify(etlService, times(3)).extractAndLoad(anyString(), eq(1L));
    }

    @Test
    void downloadAllSymbols_marksCompletedWhenAllSucceed() {
        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setStatus(BatchStatus.IN_PROGRESS);
        savedLog.setTotalSymbols(3);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);

        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any(BatchDownloadLog.class))).thenReturn(savedLog);
        when(etlService.extractAndLoad(anyString(), eq(1L))).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        ArgumentCaptor<BatchDownloadLog> captor = ArgumentCaptor.forClass(BatchDownloadLog.class);
        verify(logRepository, atLeastOnce()).save(captor.capture());

        List<BatchDownloadLog> savedLogs = captor.getAllValues();
        BatchDownloadLog lastSave = savedLogs.get(savedLogs.size() - 1);
        assertThat(lastSave.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void downloadAllSymbols_marksCompletedWithErrorsWhenSomeSymbolsFail() {
        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setStatus(BatchStatus.IN_PROGRESS);
        savedLog.setTotalSymbols(3);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);

        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any(BatchDownloadLog.class))).thenReturn(savedLog);
        when(etlService.extractAndLoad("AAPL", 1L)).thenReturn(new FinancialSeries());
        when(etlService.extractAndLoad("MSFT", 1L))
                .thenThrow(new DataDownloadException("Error API"));
        when(etlService.extractAndLoad("GOOGL", 1L)).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        ArgumentCaptor<BatchDownloadLog> captor = ArgumentCaptor.forClass(BatchDownloadLog.class);
        verify(logRepository, atLeastOnce()).save(captor.capture());

        List<BatchDownloadLog> savedLogs = captor.getAllValues();
        BatchDownloadLog lastSave = savedLogs.get(savedLogs.size() - 1);
        assertThat(lastSave.getStatus()).isEqualTo(BatchStatus.COMPLETED_WITH_ERRORS);
    }

    @Test
    void getLastBatchStatus_returnsCurrentDayLog() {
        BatchDownloadLog log = new BatchDownloadLog();
        log.setDownloadDate(LocalDate.now());
        log.setStatus(BatchStatus.COMPLETED);
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.of(log));

        Optional<BatchDownloadLog> result = batchDownloadService.getLastBatchStatus();

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void getLastBatchStatus_returnsEmptyWhenNoBatchToday() {
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());

        Optional<BatchDownloadLog> result = batchDownloadService.getLastBatchStatus();

        assertThat(result).isEmpty();
    }

    @Test
    void downloadAllSymbols_skipsIfAlreadyCompletedWithErrorsToday() {
        BatchDownloadLog existing = new BatchDownloadLog();
        existing.setStatus(BatchStatus.COMPLETED_WITH_ERRORS);
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.of(existing));

        batchDownloadService.downloadAllSymbols();

        verify(logRepository, never()).save(any(BatchDownloadLog.class));
        verify(etlService, never()).extractAndLoad(anyString(), anyLong());
    }

    @Test
    void downloadAllSymbols_retriesIfPreviousBatchFailed() {
        BatchDownloadLog existing = new BatchDownloadLog();
        existing.setStatus(BatchStatus.FAILED);
        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.of(existing));

        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setTotalSymbols(3);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);
        when(logRepository.save(any(BatchDownloadLog.class))).thenReturn(savedLog);
        when(etlService.extractAndLoad(anyString(), eq(1L))).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        verify(etlService, times(3)).extractAndLoad(anyString(), eq(1L));
    }

    @Test
    void downloadAllSymbols_marksBatchAsFailed_whenFatalExceptionOccurs() {
        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setTotalSymbols(3);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);

        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any(BatchDownloadLog.class)))
                .thenReturn(savedLog)
                .thenThrow(new RuntimeException("DB unavailable"))
                .thenReturn(savedLog);
        when(etlService.extractAndLoad(anyString(), eq(1L))).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        ArgumentCaptor<BatchDownloadLog> captor = ArgumentCaptor.forClass(BatchDownloadLog.class);
        verify(logRepository, atLeastOnce()).save(captor.capture());

        List<BatchDownloadLog> savedLogs = captor.getAllValues();
        BatchDownloadLog lastSave = savedLogs.get(savedLogs.size() - 1);
        assertThat(lastSave.getStatus()).isEqualTo(BatchStatus.FAILED);
    }

    @Test
    void downloadAllSymbols_processesMultipleBatches() {
        ReflectionTestUtils.setField(batchDownloadService, "symbols", List.of("AAPL", "MSFT"));
        ReflectionTestUtils.setField(batchDownloadService, "maxPerMinute", 1);

        BatchDownloadLog savedLog = new BatchDownloadLog();
        savedLog.setId(1L);
        savedLog.setTotalSymbols(2);
        savedLog.setDownloadedSymbols(0);
        savedLog.setFailedSymbols(0);

        when(logRepository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now()))
                .thenReturn(Optional.empty());
        when(logRepository.save(any(BatchDownloadLog.class))).thenReturn(savedLog);
        when(etlService.extractAndLoad(anyString(), eq(1L))).thenReturn(new FinancialSeries());

        batchDownloadService.downloadAllSymbols();

        verify(etlService, times(2)).extractAndLoad(anyString(), eq(1L));
    }
}
