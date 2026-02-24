package com.financial.algorithm.repository;

import com.financial.algorithm.entity.SmaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para {@link SmaRecord}.
 *
 * <p>Proporciona operaciones CRUD sobre la tabla {@code sma_results}.
 */
public interface SmaRepository extends JpaRepository<SmaRecord, Long> {

    /** Busca el registro SMA más reciente para un símbolo y ventana dados. */
    Optional<SmaRecord> findTopBySymbolAndWindowOrderByComputedAtDesc(String symbol, int window);
}
