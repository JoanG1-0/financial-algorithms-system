package com.financial.etl.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "batch_download_log")
public class BatchDownloadLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "download_date", nullable = false)
    private LocalDate downloadDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_symbols")
    private int totalSymbols;

    @Column(name = "downloaded_symbols")
    private int downloadedSymbols;

    @Column(name = "failed_symbols")
    private int failedSymbols;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    void prePersist() {
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }
}
