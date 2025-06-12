package com.codebridge.documentation.repository;

import com.codebridge.documentation.model.PublishTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing publish targets.
 */
@Repository
public interface PublishTargetRepository extends JpaRepository<PublishTarget, UUID> {

    /**
     * Find a target by name.
     *
     * @param name the target name
     * @return the publish target
     */
    Optional<PublishTarget> findByName(String name);

    /**
     * Find targets by enabled status.
     *
     * @param enabled the enabled status
     * @return the list of publish targets
     */
    List<PublishTarget> findByEnabled(boolean enabled);

    /**
     * Find targets by type.
     *
     * @param type the target type
     * @return the list of publish targets
     */
    List<PublishTarget> findByType(PublishTarget.TargetType type);
}

