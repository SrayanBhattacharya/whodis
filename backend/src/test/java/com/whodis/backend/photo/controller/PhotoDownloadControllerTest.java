package com.whodis.backend.photo.controller;

import com.whodis.backend.common.exception.GlobalExceptionHandler;
import com.whodis.backend.photo.dto.BulkDownloadRequest;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.service.PhotoDownloadService;
import com.whodis.backend.photo.service.PhotoNotFoundException;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.entity.SessionStatus;
import com.whodis.backend.session.service.SessionExpiredException;
import com.whodis.backend.session.service.SessionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PhotoDownloadControllerTest {

    @Mock
    private PhotoDownloadService photoDownloadService;

    private MockMvc mockMvc;

    private UUID sessionId;
    private Session session;

    @BeforeEach
    void setUp() {
        PhotoDownloadController controller = new PhotoDownloadController(photoDownloadService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sessionId = UUID.randomUUID();
        session = new Session(sessionId, Instant.now(), Instant.now().plusSeconds(3600), SessionStatus.ACTIVE);
    }

    private Photo createMockPhoto(UUID photoId, String filename) {
        return new Photo(
                photoId,
                session,
                "key_" + photoId,
                filename,
                "image/jpeg",
                2048L,
                Instant.now()
        );
    }

    @Test
    void downloadSinglePhoto_success() throws Exception {
        UUID photoId = UUID.randomUUID();
        Photo photo = createMockPhoto(photoId, "my image (vacation).jpg");
        InputStream stream = new ByteArrayInputStream("image data".getBytes(StandardCharsets.UTF_8));

        when(photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .thenReturn(new PhotoDownloadService.PhotoDownload(photo, stream));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/photos/{photoId}/download", sessionId, photoId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, 2048L))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("my image (vacation).jpg")));
    }

    @Test
    void downloadSinglePhoto_notFound() throws Exception {
        UUID photoId = UUID.randomUUID();
        when(photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .thenThrow(new PhotoNotFoundException(photoId));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/photos/{photoId}/download", sessionId, photoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
    }

    @Test
    void downloadSinglePhoto_sessionNotFound() throws Exception {
        UUID photoId = UUID.randomUUID();
        when(photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .thenThrow(new SessionNotFoundException(sessionId));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/photos/{photoId}/download", sessionId, photoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"));
    }

    @Test
    void downloadSinglePhoto_sessionExpired() throws Exception {
        UUID photoId = UUID.randomUUID();
        when(photoDownloadService.getSinglePhotoDownload(sessionId, photoId))
                .thenThrow(new SessionExpiredException(sessionId));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/photos/{photoId}/download", sessionId, photoId))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("SESSION_EXPIRED"));
    }

    @Test
    void downloadBulkPhotos_success() throws Exception {
        UUID photoId1 = UUID.randomUUID();
        UUID photoId2 = UUID.randomUUID();

        when(photoDownloadService.getBulkPhotosZip(eq(sessionId), any()))
                .thenReturn(outputStream -> outputStream.write("zip-bytes".getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/photos/download", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[\"" + photoId1 + "\",\"" + photoId2 + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("attachment; filename=\"whodis-photos.zip\"")));
    }

    @Test
    void downloadBulkPhotos_emptyIds_badRequest() throws Exception {
        when(photoDownloadService.getBulkPhotosZip(eq(sessionId), any()))
                .thenThrow(new IllegalArgumentException("Photo IDs list cannot be empty"));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/photos/download", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void downloadBulkPhotos_missingPhoto_notFound() throws Exception {
        UUID missingId = UUID.randomUUID();
        when(photoDownloadService.getBulkPhotosZip(eq(sessionId), any()))
                .thenThrow(new PhotoNotFoundException(missingId));

        mockMvc.perform(post("/api/v1/sessions/{sessionId}/photos/download", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"photoIds\":[\"" + missingId + "\"]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
    }

    @Test
    void downloadPersonPhotos_success() throws Exception {
        UUID personId = UUID.randomUUID();

        when(photoDownloadService.getPersonPhotosZip(sessionId, personId))
                .thenReturn(new PhotoDownloadService.PersonPhotosZip(
                        "whodis-bob-marley-photos.zip",
                        outputStream -> outputStream.write("zip-bytes".getBytes(StandardCharsets.UTF_8))
                ));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/people/{personId}/photos/download", sessionId, personId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/zip"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, org.hamcrest.Matchers.containsString("whodis-bob-marley-photos.zip")));
    }

    @Test
    void downloadPersonPhotos_noPhotosFound() throws Exception {
        UUID personId = UUID.randomUUID();

        when(photoDownloadService.getPersonPhotosZip(sessionId, personId))
                .thenThrow(new NoSuchElementException("No photos found for person: Bob"));

        mockMvc.perform(get("/api/v1/sessions/{sessionId}/people/{personId}/photos/download", sessionId, personId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
