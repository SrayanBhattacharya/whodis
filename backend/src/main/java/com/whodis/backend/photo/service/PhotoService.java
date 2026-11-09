package com.whodis.backend.photo.service;

import com.whodis.backend.photo.dto.PhotoUploadResponse;
import com.whodis.backend.referenceimage.service.ImageValidationService;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoService {

    private final SessionService sessionService;
    private final StorageService storageService;
    private final ImageValidationService imageValidationService;
    private final PhotoPersistenceService photoPersistenceService;

    public List<PhotoUploadResponse> uploadPhotos(
            UUID sessionId,
            List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one photo must be uploaded"
            );
        }

        Session session = sessionService.getSession(sessionId);

        for (MultipartFile file : files) {
            imageValidationService.validate(file);
        }

        List<String> storedKeys = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String storageKey;

                try {
                    storageKey = storageService.store(file.getInputStream());
                } catch (IOException e) {
                    throw new IllegalStateException(
                            "Failed to read uploaded photo",
                            e
                    );
                }

                storedKeys.add(storageKey);
            }

            return photoPersistenceService.persistPhotos(
                    session,
                    files,
                    storedKeys
            );

        } catch (RuntimeException e) {
            for (String storageKey : storedKeys) {
                try {
                    storageService.delete(storageKey);
                } catch (Exception cleanupException) {
                    log.error(
                            "Failed to clean up photo: {}",
                            storageKey,
                            cleanupException
                    );
                }
            }

            throw e;
        }
    }
}
