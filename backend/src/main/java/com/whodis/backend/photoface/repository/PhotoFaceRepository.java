package com.whodis.backend.photoface.repository;

import com.whodis.backend.photoface.entity.PhotoFace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PhotoFaceRepository extends JpaRepository<PhotoFace, UUID> {
    List<PhotoFace> findAllByPhotoIdOrderByCreatedAtAsc(UUID photoId);

    List<PhotoFace> findAllByPersonIdOrderByCreatedAtAsc(UUID personId);

    long countByPhotoId(UUID photoId);

    long countByPersonId(UUID personId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
    DELETE FROM PhotoFace pf
    WHERE pf.photo.id = :photoId
    """)
    void deleteAllByPhotoId(@Param("photoId") UUID photoId);
}
