package com.whodis.backend.photo.dto;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;

import java.time.Instant;
import java.util.UUID;

public record PhotoUploadResponse(
        UUID photoId,
        UUID jobId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt,
        ProcessingJobStatus status
) {
    public static PhotoUploadResponse from(
            Photo photo,
            ProcessingJob job
    ) {
        return new PhotoUploadResponse(
                photo.getId(),
                job.getId(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getCreatedAt(),
                job.getStatus()
        );
    }
}