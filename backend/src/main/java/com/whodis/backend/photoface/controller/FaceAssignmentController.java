package com.whodis.backend.photoface.controller;

import com.whodis.backend.photoface.dto.AssignFaceRequest;
import com.whodis.backend.photoface.dto.FaceAssignmentResponse;
import com.whodis.backend.photoface.service.FaceAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/faces")
@RequiredArgsConstructor
public class FaceAssignmentController {

    private final FaceAssignmentService faceAssignmentService;

    @PatchMapping("/{faceId}/assign")
    public ResponseEntity<FaceAssignmentResponse> assignFace(
            @PathVariable UUID sessionId,
            @PathVariable UUID faceId,
            @RequestBody AssignFaceRequest request
    ) {
        return ResponseEntity.ok(
                faceAssignmentService.assignFace(
                        sessionId,
                        faceId,
                        request
                )
        );
    }
}