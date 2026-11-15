package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceEmbeddingRepository
        extends JpaRepository<ReferenceEmbedding, UUID> {

    Optional<ReferenceEmbedding> findByReferenceImageId(UUID referenceImageId);

    @Query("""
            SELECT re
            FROM ReferenceEmbedding re
            JOIN FETCH re.referenceImage ri
            JOIN FETCH ri.person p
            WHERE p.session.id = :sessionId
            """)
    List<ReferenceEmbedding> findAllBySessionId(
            @Param("sessionId") UUID sessionId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
    DELETE FROM ReferenceEmbedding re
    WHERE re.referenceImage.id IN (
        SELECT ri.id
        FROM ReferenceImage ri
        WHERE ri.person.session.id = :sessionId
    )
    """)
    void deleteAllBySessionId(
            @Param("sessionId") UUID sessionId
    );
}
