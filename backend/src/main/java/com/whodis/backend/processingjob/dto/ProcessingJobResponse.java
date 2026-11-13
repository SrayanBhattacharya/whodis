package com.whodis.backend.processingjob.dto;

import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;

import java.time.Instant;
import java.util.UUID;

public record ProcessingJobResponse(
        UUID jobId,
        UUID photoId,
        ProcessingJobStatus status,
        int attemptCount,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
    public static ProcessingJobResponse from(ProcessingJob job) {
        return new ProcessingJobResponse(
                job.getId(),
                job.getPhoto().getId(),
                job.getStatus(),
                job.getAttemptCount(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
