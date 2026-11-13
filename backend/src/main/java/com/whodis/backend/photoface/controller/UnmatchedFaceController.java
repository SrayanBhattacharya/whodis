package com.whodis.backend.photoface.controller;

import com.whodis.backend.photoface.dto.UnmatchedFaceResponse;
import com.whodis.backend.photoface.service.UnmatchedFaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/unmatched-faces")
@RequiredArgsConstructor
public class UnmatchedFaceController {

    private final UnmatchedFaceService unmatchedFaceService;

    @GetMapping
    public ResponseEntity<List<UnmatchedFaceResponse>> getUnmatchedFaces(
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(
                unmatchedFaceService.getUnmatchedFaces(sessionId)
        );
    }
}