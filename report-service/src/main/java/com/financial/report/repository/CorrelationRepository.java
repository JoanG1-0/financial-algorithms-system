package com.financial.report.repository;

import com.financial.report.entity.CorrelationEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio JPA para la entidad {@link CorrelationEntry}.
 */
public interface CorrelationRepository extends JpaRepository<CorrelationEntry, Long> {
}
