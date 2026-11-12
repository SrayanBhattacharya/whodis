package com.whodis.backend.photo.service;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import com.whodis.backend.processingjob.repository.ProcessingJobRepository;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoDeletionService {

    private final SessionService sessionService;
    private final PhotoRepository photoRepository;
    private final PhotoFaceRepository photoFaceRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final StorageService storageService;

    @Transactional
    public boolean deletePhoto(UUID sessionId, UUID photoId) {

        // Validate the session.
        sessionService.getSession(sessionId);

        // Lock the photo row.
        Photo photo = photoRepository
                .findByIdAndSessionIdForUpdate(photoId, sessionId)
                .orElse(null);

        if (photo == null) {
            return false;
        }

        // Prevent deletion while processing is active.
        boolean processing = processingJobRepository
                .existsByPhotoIdAndStatus(
                        photoId,
                        ProcessingJobStatus.PROCESSING
                );

        if (processing) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a photo while it is being processed"
            );
        }

        String storageKey = photo.getStorageKey();

        // Delete dependent database records first.
        photoFaceRepository.deleteAllByPhotoId(photoId);
        processingJobRepository.deleteAllByPhotoId(photoId);

        // Delete the photo record.
        photoRepository.delete(photo);

        // Delete the physical file only after DB commit.
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            storageService.delete(storageKey);
                        } catch (Exception e) {
                            log.error(
                                    "Failed to delete stored photo: {}",
                                    storageKey,
                                    e
                            );
                        }
                    }
                }
        );

        return true;
    }
}