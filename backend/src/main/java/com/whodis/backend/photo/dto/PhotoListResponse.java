package com.whodis.backend.photo.dto;

import com.whodis.backend.photo.entity.Photo;

import java.time.Instant;
import java.util.UUID;

public record PhotoListResponse(
        UUID photoId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt
) {
    public static PhotoListResponse from(Photo photo) {
        return new PhotoListResponse(
                photo.getId(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getCreatedAt()
        );
    }
}