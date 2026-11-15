package com.whodis.backend.session.scheduler;

import com.whodis.backend.session.repository.SessionRepository;
import com.whodis.backend.session.service.SessionCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupScheduler {

    private final SessionRepository sessionRepository;
    private final SessionCleanupService sessionCleanupService;

    @Scheduled(
            fixedDelayString = "${whodis.session.cleanup-interval:300000}"
    )
    public void cleanupExpiredSessions() {

        List<UUID> expiredSessionIds =
                sessionRepository.findExpiredSessionIds(Instant.now());

        if (expiredSessionIds.isEmpty()) {
            return;
        }

        log.info(
                "Found {} expired sessions",
                expiredSessionIds.size()
        );

        for (UUID sessionId : expiredSessionIds) {
            try {
                sessionCleanupService.cleanupSession(sessionId);
            } catch (Exception exception) {
                log.error(
                        "Failed to clean up session {}",
                        sessionId,
                        exception
                );
            }
        }
    }
}