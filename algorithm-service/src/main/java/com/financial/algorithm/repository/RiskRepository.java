package com.financial.algorithm.repository;

import com.financial.algorithm.entity.RiskRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para {@link RiskRecord}.
 *
 * <p>Proporciona operaciones CRUD sobre la tabla {@code risk_profiles}.
 */
public interface RiskRepository extends JpaRepository<RiskRecord, Long> {
}
