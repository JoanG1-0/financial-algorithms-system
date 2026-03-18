package com.financial.algorithm.repository;

import com.financial.algorithm.entity.RiskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link RiskRecord}.
 *
 * <p>Proporciona operaciones CRUD sobre la tabla {@code risk_profiles}.
 */
public interface RiskRepository extends JpaRepository<RiskRecord, Long> {

    Optional<RiskRecord> findTopByOrderByAnnualizedVolatilityDesc();

    Optional<RiskRecord> findTopByOrderByAnnualizedVolatilityAsc();

    Optional<RiskRecord> findTopByOrderByComputedAtDesc();

    @Query("SELECT r.category AS category, COUNT(r) AS total FROM RiskRecord r GROUP BY r.category")
    List<CategoryCount> countByCategory();

    interface CategoryCount {
        String getCategory();
        Long getTotal();
    }
}
