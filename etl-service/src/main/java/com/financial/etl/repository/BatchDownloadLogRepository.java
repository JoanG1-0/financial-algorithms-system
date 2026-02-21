package com.financial.etl.repository;

import com.financial.etl.entity.BatchDownloadLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BatchDownloadLogRepository extends JpaRepository<BatchDownloadLog, Long> {

    Optional<BatchDownloadLog> findFirstByDownloadDateOrderByStartedAtDesc(LocalDate downloadDate);
}
