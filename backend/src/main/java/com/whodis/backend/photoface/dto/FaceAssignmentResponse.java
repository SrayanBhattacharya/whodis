package com.whodis.backend.photoface.dto;

import com.whodis.backend.photoface.entity.PhotoFace;

import java.time.Instant;
import java.util.UUID;

public record FaceAssignmentResponse(
        UUID faceId,
        UUID photoId,
        UUID personId,
        String personName,
        String boundingBox,
        Float similarityScore,
        Instant createdAt
) {
    public static FaceAssignmentResponse from(PhotoFace face) {
        return new FaceAssignmentResponse(
                face.getId(),
                face.getPhoto().getId(),
                face.getPerson().getId(),
                face.getPerson().getName(),
                face.getBoundingBox(),
                face.getSimilarityScore(),
                face.getCreatedAt()
        );
    }
}