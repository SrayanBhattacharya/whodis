package com.whodis.backend.referenceimage.controller;

import com.whodis.backend.referenceimage.dto.ReferenceImageResponse;
import com.whodis.backend.referenceimage.entity.ReferenceImage;
import com.whodis.backend.referenceimage.service.ReferenceImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/people/{personId}/reference-images")
@RequiredArgsConstructor
public class ReferenceImageController {
    private final ReferenceImageService referenceImageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReferenceImageResponse> uploadReferenceImage(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId,
            @RequestParam("file")MultipartFile file
            ) {
        ReferenceImage referenceImage =
                referenceImageService.uploadReferenceImage(
                        sessionId,
                        personId,
                        file
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ReferenceImageResponse.from(referenceImage));
    }

    @GetMapping
    public ResponseEntity<List<ReferenceImageResponse>> getReferenceImages(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId
    ) {
        List<ReferenceImageResponse> images = referenceImageService
                .getReferenceImages(sessionId, personId)
                .stream()
                .map(ReferenceImageResponse::from)
                .toList();

        return ResponseEntity.ok(images);
    }

    @GetMapping("/{imageId}")
    public ResponseEntity<ReferenceImageResponse> getReferenceImage(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId,
            @PathVariable UUID imageId
    ) {
        ReferenceImage image = referenceImageService.getReferenceImage(
                sessionId,
                personId,
                imageId
        );

        return ResponseEntity.ok(
                ReferenceImageResponse.from(image)
        );
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteReferenceImage(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId,
            @PathVariable UUID imageId
    ) {
        referenceImageService.deleteReferenceImage(
                sessionId,
                personId,
                imageId
        );

        return ResponseEntity.noContent().build();
    }
}
