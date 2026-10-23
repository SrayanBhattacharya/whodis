package com.whodis.backend.session.controller;

import com.whodis.backend.session.dto.SessionResponse;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @PostMapping
    public ResponseEntity<SessionResponse> createSession() {

        Session session = sessionService.createSession();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(SessionResponse.from(session));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<SessionResponse> getSession(
            @PathVariable UUID sessionId
    ) {

        Session session = sessionService.getSession(sessionId);

        return ResponseEntity.ok(SessionResponse.from(session));
    }
}
