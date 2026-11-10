package com.whodis.backend.person.dto;

import com.whodis.backend.photoface.entity.PhotoFace;

import java.time.Instant;
import java.util.UUID;

public record PersonPhotoResponse(
        UUID photoId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt,
        String boundingBox,
        Float similarityScore
) {
    public static PersonPhotoResponse from(PhotoFace photoFace) {
        var photo = photoFace.getPhoto();

        return new PersonPhotoResponse(
                photo.getId(),
                photo.getOriginalFilename(),
                photo.getContentType(),
                photo.getFileSize(),
                photo.getCreatedAt(),
                photoFace.getBoundingBox(),
                photoFace.getSimilarityScore()
        );
    }
}
