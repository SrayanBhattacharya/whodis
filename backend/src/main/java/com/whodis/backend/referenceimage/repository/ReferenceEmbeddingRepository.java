package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.swing.text.html.Option;
import java.util.Optional;
import java.util.UUID;

public interface ReferenceEmbeddingRepository extends JpaRepository<ReferenceEmbedding, UUID> {
    Optional<ReferenceEmbedding> findByReferenceImageId(UUID referenceImageId);
}
