package com.whodis.backend.photo.controller;

import com.whodis.backend.photo.dto.BulkDownloadRequest;
import com.whodis.backend.photo.service.PhotoDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
@RequiredArgsConstructor
public class PhotoDownloadController {

    private final PhotoDownloadService photoDownloadService;

    @GetMapping("/photos/{photoId}/download")
    public ResponseEntity<InputStreamResource> downloadSinglePhoto(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId
    ) {
        PhotoDownloadService.PhotoDownload download =
                photoDownloadService.getSinglePhotoDownload(sessionId, photoId);

        var photo = download.photo();

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(photo.getOriginalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                .contentLength(photo.getFileSize())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new InputStreamResource(download.stream()));
    }

    @PostMapping("/photos/download")
    public ResponseEntity<StreamingResponseBody> downloadBulkPhotos(
            @PathVariable UUID sessionId,
            @RequestBody BulkDownloadRequest request
    ) {
        StreamingResponseBody stream =
                photoDownloadService.getBulkPhotosZip(sessionId, request.photoIds());

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("whodis-photos.zip", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(stream);
    }

    @GetMapping("/people/{personId}/photos/download")
    public ResponseEntity<StreamingResponseBody> downloadPersonPhotos(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId
    ) {
        PhotoDownloadService.PersonPhotosZip personZip =
                photoDownloadService.getPersonPhotosZip(sessionId, personId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(personZip.filename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(personZip.stream());
    }
}
