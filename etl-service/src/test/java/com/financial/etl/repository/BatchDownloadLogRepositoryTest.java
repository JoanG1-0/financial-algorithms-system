package com.financial.etl.repository;

import com.financial.etl.entity.BatchDownloadLog;
import com.financial.etl.entity.BatchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BatchDownloadLogRepositoryTest {

    @Autowired
    private BatchDownloadLogRepository repository;

    @Test
    void findFirstByDownloadDate_returnsLog_whenExists() {
        BatchDownloadLog log = new BatchDownloadLog();
        log.setDownloadDate(LocalDate.now());
        log.setStatus(BatchStatus.COMPLETED);
        log.setTotalSymbols(5);
        repository.save(log);

        Optional<BatchDownloadLog> result =
                repository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void findFirstByDownloadDate_returnsEmpty_whenNoLogForDate() {
        Optional<BatchDownloadLog> result =
                repository.findFirstByDownloadDateOrderByStartedAtDesc(LocalDate.now().minusDays(1));

        assertThat(result).isEmpty();
    }

    @Test
    void findFirstByDownloadDate_returnsLatest_whenMultipleExistForSameDay() {
        LocalDate today = LocalDate.now();

        BatchDownloadLog first = new BatchDownloadLog();
        first.setDownloadDate(today);
        first.setStatus(BatchStatus.FAILED);
        first.setStartedAt(LocalDateTime.now().minusHours(2));
        repository.save(first);

        BatchDownloadLog second = new BatchDownloadLog();
        second.setDownloadDate(today);
        second.setStatus(BatchStatus.COMPLETED);
        second.setStartedAt(LocalDateTime.now());
        repository.save(second);

        Optional<BatchDownloadLog> result =
                repository.findFirstByDownloadDateOrderByStartedAtDesc(today);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    void prePersist_setsStartedAt_whenNull() {
        BatchDownloadLog log = new BatchDownloadLog();
        log.setDownloadDate(LocalDate.now());
        log.setStatus(BatchStatus.IN_PROGRESS);

        BatchDownloadLog saved = repository.save(log);

        assertThat(saved.getStartedAt()).isNotNull();
    }

    @Test
    void prePersist_keepsStartedAt_whenAlreadySet() {
        LocalDateTime fixedTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0);

        BatchDownloadLog log = new BatchDownloadLog();
        log.setDownloadDate(LocalDate.now());
        log.setStatus(BatchStatus.IN_PROGRESS);
        log.setStartedAt(fixedTime);

        BatchDownloadLog saved = repository.save(log);

        assertThat(saved.getStartedAt()).isEqualTo(fixedTime);
    }
}
