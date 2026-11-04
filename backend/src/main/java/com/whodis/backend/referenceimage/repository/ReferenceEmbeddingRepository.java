package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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
}
