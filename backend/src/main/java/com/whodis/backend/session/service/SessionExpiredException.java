package com.whodis.backend.session.service;

import java.util.UUID;

public class SessionExpiredException extends RuntimeException {
    public SessionExpiredException(UUID sessionId) {
        super("Session has expired: " + sessionId);
    }
}