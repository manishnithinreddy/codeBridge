package com.codebridge.core.repository;

import com.codebridge.core.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Base repository interface with soft delete support.
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity, ID> extends JpaRepository<T, ID> {

    /**
     * Finds all non-deleted entities.
     *
     * @return list of non-deleted entities
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false")
    List<T> findAllActive();

    /**
     * Finds a non-deleted entity by its ID.
     *
     * @param id the entity ID
     * @return the entity if found and not deleted
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
    Optional<T> findByIdActive(@Param("id") ID id);

    /**
     * Soft deletes an entity.
     *
     * @param id the entity ID
     * @param deletedBy the user who deleted the entity
     * @return the number of affected rows
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = true, e.deletedAt = CURRENT_TIMESTAMP, e.deletedBy = :deletedBy WHERE e.id = :id")
    int softDelete(@Param("id") ID id, @Param("deletedBy") UUID deletedBy);

    /**
     * Restores a soft-deleted entity.
     *
     * @param id the entity ID
     * @param updatedBy the user who restored the entity
     * @return the number of affected rows
     */
    @Modifying
    @Query("UPDATE #{#entityName} e SET e.deleted = false, e.deletedAt = null, e.deletedBy = null, e.updatedAt = CURRENT_TIMESTAMP, e.updatedBy = :updatedBy WHERE e.id = :id")
    int restore(@Param("id") ID id, @Param("updatedBy") UUID updatedBy);
}

