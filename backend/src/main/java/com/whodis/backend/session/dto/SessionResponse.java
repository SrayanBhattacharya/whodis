package com.whodis.backend.session.dto;

import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.entity.SessionStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        Instant createdAt,
        Instant expiresAt,
        SessionStatus status
) {

    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getCreatedAt(),
                session.getExpiresAt(),
                session.getStatus()
        );
    }
}
