package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceImageRepository extends JpaRepository<ReferenceImage, UUID> {

    @EntityGraph(attributePaths = "person")
    List<ReferenceImage> findAllByPersonIdOrderByCreatedAtAsc(UUID personId);

    @EntityGraph(attributePaths = "person")
    Optional<ReferenceImage> findByIdAndPersonId(
            UUID imageId,
            UUID personId
    );

    @Query("""
    SELECT ri.storageKey
    FROM ReferenceImage ri
    WHERE ri.person.session.id = :sessionId
    """)
    List<String> findAllStorageKeysBySessionId(
            @Param("sessionId") UUID sessionId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
    DELETE FROM ReferenceImage ri
    WHERE ri.person.session.id = :sessionId
    """)
    void deleteAllBySessionId(
            @Param("sessionId") UUID sessionId
    );
}
