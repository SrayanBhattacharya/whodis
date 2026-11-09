package com.whodis.backend.processingjob.service;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import com.whodis.backend.processingjob.repository.ProcessingJobRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingJobService {
    private final ProcessingJobRepository processingJobRepository;
    private final PhotoRepository photoRepository;

    @Transactional
    public ProcessingJob createJob(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Photo not found: " + photoId
                        )
                );

        ProcessingJob job = new ProcessingJob(
                UUID.randomUUID(),
                photo,
                ProcessingJobStatus.PENDING,
                0,
                null,
                Instant.now(),
                null,
                null,
                null
        );

        return processingJobRepository.save(job);
    }

    @Transactional
    public boolean claimJob(UUID jobId) {
        return processingJobRepository.claimJob(
                jobId,
                ProcessingJobStatus.PENDING,
                ProcessingJobStatus.PROCESSING,
                Instant.now()
        ) == 1;
    }

    @Transactional(readOnly = true)
    public ProcessingJob getJob(UUID jobId) {
        return processingJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Processing job not found: " + jobId
                        )
                );
    }

    @Transactional
    public void completeJob(UUID jobId) {
        ProcessingJob job = getProcessingJob(jobId);
        requireStatus(job, ProcessingJobStatus.PROCESSING);

        job.setStatus(ProcessingJobStatus.COMPLETED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(null);
    }

    @Transactional
    public void failJob(UUID jobId, String errorMessage) {
        ProcessingJob job = getProcessingJob(jobId);
        requireStatus(job, ProcessingJobStatus.PROCESSING);

        job.setStatus(ProcessingJobStatus.FAILED);
        job.setCompletedAt(Instant.now());
        job.setErrorMessage(errorMessage);
    }

    @Transactional(readOnly = true)
    public UUID getPhotoId(UUID jobId) {
        return processingJobRepository.findById(jobId)
                .map(job -> job.getPhoto().getId())
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Processing job not found: " + jobId
                        )
                );
    }

    @Transactional
    public boolean reclaimStaleJob(UUID jobId, Duration idleTimeout) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(idleTimeout);

        return processingJobRepository.reclaimStaleJob(
                jobId,
                ProcessingJobStatus.PROCESSING,
                cutoff,
                now
        ) == 1;
    }

    @Transactional
    public boolean scheduleRetry(
            UUID jobId,
            String errorMessage,
            int maxAttempts
    ) {
        return processingJobRepository.scheduleRetry(
                jobId,
                ProcessingJobStatus.PROCESSING,
                ProcessingJobStatus.PENDING,
                errorMessage,
                maxAttempts
        ) == 1;
    }

    @Transactional
    public void markQueuePublished(UUID jobId) {
        processingJobRepository.markQueuePublished(
                jobId,
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<UUID> getUnpublishedPendingJobIds() {
        return processingJobRepository
                .findTop50ByStatusAndQueuePublishedAtIsNullOrderByCreatedAtAsc(
                        ProcessingJobStatus.PENDING
                )
                .stream()
                .map(job -> job.getId())
                .toList();
    }

    private ProcessingJob getProcessingJob(UUID jobId) {
        return processingJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Processing job not found: " + jobId
                        )
                );
    }

    private void requireStatus(
            ProcessingJob job,
            ProcessingJobStatus expectedStatus
    ) {
        if (job.getStatus() != expectedStatus) {
            throw new IllegalStateException(
                    "Expected job status " + expectedStatus
                            + " but was " + job.getStatus()
            );
        }
    }
}