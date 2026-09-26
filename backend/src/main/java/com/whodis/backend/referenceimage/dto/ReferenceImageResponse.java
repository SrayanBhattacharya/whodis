package com.whodis.backend.referenceimage.dto;

import com.whodis.backend.referenceimage.entity.ReferenceImage;

import java.time.Instant;
import java.util.UUID;

public record ReferenceImageResponse(
        UUID id,
        UUID personId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt
) {
    public static ReferenceImageResponse from(ReferenceImage image) {
        return new ReferenceImageResponse(
                image.getId(),
                image.getPerson().getId(),
                image.getOriginalFilename(),
                image.getContentType(),
                image.getFileSize(),
                image.getCreatedAt()
        );
    }
}
