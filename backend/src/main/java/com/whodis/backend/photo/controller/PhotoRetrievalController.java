package com.whodis.backend.photo.controller;

import com.whodis.backend.photo.service.PhotoRetrievalService;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/photos")
@RequiredArgsConstructor
public class PhotoRetrievalController {

    private final PhotoRetrievalService photoRetrievalService;

    @GetMapping("/{photoId}")
    public ResponseEntity<InputStreamResource> getPhoto(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId
    ) {
        return photoRetrievalService
                .getPhoto(sessionId, photoId)
                .map(retrievedPhoto -> {
                    var photo = retrievedPhoto.photo();

                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(
                                    photo.getContentType()
                            ))
                            .contentLength(photo.getFileSize())
                            .header(
                                    HttpHeaders.CONTENT_DISPOSITION,
                                    "inline; filename=\"" +
                                            photo.getOriginalFilename() + "\""
                            )
                            .body(new InputStreamResource(
                                    retrievedPhoto.content()
                            ));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}