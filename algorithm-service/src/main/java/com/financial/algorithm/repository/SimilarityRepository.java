package com.financial.algorithm.repository;

import com.financial.algorithm.entity.SimilarityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio Spring Data JPA para {@link SimilarityRecord}.
 *
 * <p>Proporciona operaciones CRUD sobre la tabla {@code similarity_results}.
 */
public interface SimilarityRepository extends JpaRepository<SimilarityRecord, Long> {
}
