package com.whodis.backend.referenceimage.repository;

import com.whodis.backend.referenceimage.entity.ReferenceImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReferenceImageRepository extends JpaRepository<ReferenceImage, UUID> {
    List<ReferenceImage> findAllByPersonIdOrderByCreatedAtAsc(UUID personId);
}
