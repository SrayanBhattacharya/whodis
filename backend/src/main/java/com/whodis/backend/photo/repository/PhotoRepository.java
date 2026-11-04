package com.whodis.backend.photo.repository;

import com.whodis.backend.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<Photo> findByIdAndSessionId(UUID photoId, UUID sessionId);

    @Query("""
        SELECT p.session.id
        FROM Photo p
        WHERE p.id = :photoId
        """)
    Optional<UUID> findSessionIdByPhotoId(@Param("photoId") UUID photoId);
}
