package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceImage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
