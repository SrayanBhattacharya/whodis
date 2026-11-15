package com.whodis.backend.session.repository;

import com.whodis.backend.session.entity.Session;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    @Query("""
        SELECT s.id
        FROM Session s
        WHERE s.expiresAt <= :now
        ORDER BY s.expiresAt ASC
        """)
    List<UUID> findExpiredSessionIds(@Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Session s
        WHERE s.id = :sessionId
        """)
    Optional<Session> findByIdForUpdate(
            @Param("sessionId") UUID sessionId
    );
}