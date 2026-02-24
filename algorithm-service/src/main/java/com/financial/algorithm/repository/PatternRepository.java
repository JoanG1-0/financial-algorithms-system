package com.financial.algorithm.repository;

import com.financial.algorithm.entity.PatternRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para {@link PatternRecord}.
 *
 * <p>Proporciona operaciones CRUD sobre la tabla {@code pattern_results}.
 */
public interface PatternRepository extends JpaRepository<PatternRecord, Long> {

    /** Busca todos los patrones detectados para un símbolo dado. */
    List<PatternRecord> findBySymbol(String symbol);
}
