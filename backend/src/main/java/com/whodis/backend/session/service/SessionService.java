package com.whodis.backend.session.service;

import com.whodis.backend.common.config.SessionProperties;
import com.whodis.backend.session.entity.Session;

import com.whodis.backend.session.entity.SessionStatus;
import com.whodis.backend.session.repository.SessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final SessionRepository sessionRepository;
    private final SessionProperties sessionProperties;

    @Transactional
    public Session createSession() {
        Instant now = Instant.now();

        Session session = new Session(
                UUID.randomUUID(),
                now,
                now.plus(sessionProperties.getDuration()),
                SessionStatus.ACTIVE
        );

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session getSession(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.isExpired()) {
            throw new SessionExpiredException(sessionId);
        }

        return session;
    }
}
