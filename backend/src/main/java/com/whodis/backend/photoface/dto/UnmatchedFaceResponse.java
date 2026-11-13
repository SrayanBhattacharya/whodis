package com.whodis.backend.photoface.dto;

import com.whodis.backend.photoface.entity.PhotoFace;

import java.time.Instant;
import java.util.UUID;

public record UnmatchedFaceResponse(
        UUID faceId,
        UUID photoId,
        String originalFilename,
        String contentType,
        String boundingBox,
        Float similarityScore,
        Instant createdAt
) {
    public static UnmatchedFaceResponse from(PhotoFace photoFace) {
        return new UnmatchedFaceResponse(
                photoFace.getId(),
                photoFace.getPhoto().getId(),
                photoFace.getPhoto().getOriginalFilename(),
                photoFace.getPhoto().getContentType(),
                photoFace.getBoundingBox(),
                photoFace.getSimilarityScore(),
                photoFace.getCreatedAt()
        );
    }
}