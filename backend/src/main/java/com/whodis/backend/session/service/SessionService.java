package com.whodis.backend.session.service;

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
    private static final Duration SESSION_DURATION = Duration.ofHours(24);
    private final SessionRepository sessionRepository;

    @Transactional
    public Session createSession() {
        Instant now = Instant.now();

        Session session = new Session(
                UUID.randomUUID(),
                now,
                now.plus(SESSION_DURATION),
                SessionStatus.ACTIVE
        );

        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Session getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));
    }
}
