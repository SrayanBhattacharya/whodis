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
}
