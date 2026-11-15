package com.whodis.backend.session.service;

import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import com.whodis.backend.processingjob.repository.ProcessingJobRepository;
import com.whodis.backend.referenceimage.repository.ReferenceEmbeddingRepository;
import com.whodis.backend.referenceimage.repository.ReferenceImageRepository;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.repository.SessionRepository;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCleanupService {

    private final SessionRepository sessionRepository;
    private final PhotoRepository photoRepository;
    private final PhotoFaceRepository photoFaceRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final PersonRepository personRepository;
    private final ReferenceImageRepository referenceImageRepository;
    private final ReferenceEmbeddingRepository referenceEmbeddingRepository;
    private final StorageService storageService;

    @Transactional
    public void cleanupSession(UUID sessionId) {

        Session session = sessionRepository.findByIdForUpdate(sessionId)
                .orElse(null);

        if (session == null) {
            return;
        }

        if (!session.isExpired()) {
            return;
        }

        // Lock all photos before checking processing jobs.
        // This prevents workers from claiming new jobs for these photos
        // while cleanup is in progress.
        List<Photo> photos =
                photoRepository.findAllBySessionIdForUpdate(sessionId);

        boolean hasProcessingJobs =
                processingJobRepository.existsBySessionIdAndStatus(
                        sessionId,
                        ProcessingJobStatus.PROCESSING
                );

        if (hasProcessingJobs) {
            log.info(
                    "Skipping cleanup for session {} because processing jobs are still active",
                    sessionId
            );
            return;
        }

        List<String> storageKeys = new ArrayList<>();

        storageKeys.addAll(
                photoRepository.findAllStorageKeysBySessionId(sessionId)
        );

        storageKeys.addAll(
                referenceImageRepository.findAllStorageKeysBySessionId(sessionId)
        );

        // Delete database records in foreign-key dependency order.
        photoFaceRepository.deleteAllBySessionId(sessionId);

        processingJobRepository.deleteAllBySessionId(sessionId);

        referenceEmbeddingRepository.deleteAllBySessionId(sessionId);

        referenceImageRepository.deleteAllBySessionId(sessionId);

        photoRepository.deleteAllBySessionId(sessionId);

        personRepository.deleteAllBySessionId(sessionId);

        sessionRepository.delete(session);

        // Physical files are deleted only after the DB transaction commits.
        deleteStorageFilesAfterCommit(storageKeys);

        log.info("Cleaned up expired session {}", sessionId);
    }

    private void deleteStorageFilesAfterCommit(List<String> storageKeys) {

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {

                        for (String storageKey : storageKeys) {
                            try {
                                storageService.delete(storageKey);
                            } catch (Exception exception) {
                                log.error(
                                        "Failed to delete stored file {}",
                                        storageKey,
                                        exception
                                );
                            }
                        }
                    }
                }
        );
    }
}