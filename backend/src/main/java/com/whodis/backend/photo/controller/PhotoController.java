package com.whodis.backend.photo.controller;

import com.whodis.backend.photo.dto.PhotoUploadResponse;
import com.whodis.backend.photo.service.PhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<PhotoUploadResponse>> uploadPhotos(
            @PathVariable UUID sessionId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        List<PhotoUploadResponse> responses =
                photoService.uploadPhotos(sessionId, files);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(responses);
    }
}