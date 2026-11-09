package com.whodis.backend.photo.service;

import com.whodis.backend.photo.dto.PhotoUploadResponse;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.queue.PhotoProcessingQueue;
import com.whodis.backend.processingjob.service.ProcessingJobService;
import com.whodis.backend.session.entity.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PhotoPersistenceService {

    private final PhotoRepository photoRepository;
    private final ProcessingJobService processingJobService;
    private final PhotoProcessingQueue photoProcessingQueue;

    @Transactional
    public List<PhotoUploadResponse> persistPhotos(
            Session session,
            List<MultipartFile> files,
            List<String> storageKeys
    ) {
        List<UUID> jobIds = new ArrayList<>();
        List<PhotoUploadResponse> responses = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String storageKey = storageKeys.get(i);

            Photo photo = new Photo(
                    UUID.randomUUID(),
                    session,
                    storageKey,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    Instant.now()
            );

            photo = photoRepository.save(photo);

            ProcessingJob job =
                    processingJobService.createJob(photo.getId());

            jobIds.add(job.getId());
            responses.add(PhotoUploadResponse.from(photo, job));
        }

        List<UUID> jobsToPublish = List.copyOf(jobIds);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (UUID jobId : jobsToPublish) {
                            try {
                                photoProcessingQueue.publish(jobId);
                                processingJobService.markQueuePublished(jobId);
                            } catch (Exception e) {
                                log.error(
                                        "Failed to publish processing job {}",
                                        jobId,
                                        e
                                );
                            }
                        }
                    }
                }
        );

        return responses;
    }
}