package com.whodis.backend.photoface.repository;

import com.whodis.backend.photoface.entity.PhotoFace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PhotoFaceRepository extends JpaRepository<PhotoFace, UUID> {
    List<PhotoFace> findAllByPhotoIdOrderByCreatedAtAsc(UUID photoId);

    List<PhotoFace> findAllByPersonIdOrderByCreatedAtAsc(UUID personId);

    long countByPhotoId(UUID photoId);

    long countByPersonId(UUID personId);
}
