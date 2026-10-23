package com.whodis.backend.photo.repository;

import com.whodis.backend.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {
    List<Photo> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    Optional<Photo> findByIdAndSessionId(UUID photoId, UUID sessionId);
}
