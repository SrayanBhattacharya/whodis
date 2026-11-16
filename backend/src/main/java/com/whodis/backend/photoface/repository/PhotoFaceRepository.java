package com.whodis.backend.photoface.repository;

import com.whodis.backend.photoface.entity.PhotoFace;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
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

    @EntityGraph(attributePaths = "photo")
    List<PhotoFace> findAllByPersonIdAndPhotoSessionIdOrderByCreatedAtAsc(
            UUID personId,
            UUID sessionId
    );

    @EntityGraph(attributePaths = "photo")
    List<PhotoFace> findAllByPhotoSessionIdAndPersonIsNullOrderByCreatedAtAsc(
            UUID sessionId
    );

    @EntityGraph(attributePaths = "photo")
    @Query("""
    SELECT pf
    FROM PhotoFace pf
    WHERE pf.photo.session.id = :sessionId
      AND pf.person IS NULL
      AND (
          SELECT COUNT(pf2)
          FROM PhotoFace pf2
          WHERE pf2.photo = pf.photo
      ) < 3
    ORDER BY pf.createdAt ASC
    """)
    List<PhotoFace> findAllUnmatchedNonGroupFacesBySessionId(
            @Param("sessionId") UUID sessionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT pf
    FROM PhotoFace pf
    WHERE pf.id = :faceId
    """)
    Optional<PhotoFace> findByIdForUpdate(
            @Param("faceId") UUID faceId
    );

    @Modifying(flushAutomatically = true)
    @Query("""
    DELETE FROM PhotoFace pf
    WHERE pf.photo.session.id = :sessionId
    """)
    void deleteAllBySessionId(
            @Param("sessionId") UUID sessionId
    );
}
