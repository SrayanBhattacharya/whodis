package com.whodis.backend.photo.service;

import com.whodis.backend.ml.client.MlServiceClient;
import com.whodis.backend.ml.dto.DetectedFace;
import com.whodis.backend.ml.dto.FaceMatchResult;
import com.whodis.backend.ml.dto.PhotoFaceDetectionResponse;
import com.whodis.backend.ml.service.FaceMatchingService;
import com.whodis.backend.photoface.entity.PhotoFace;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.processingjob.service.ProcessingJobService;
import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import com.whodis.backend.referenceimage.repository.ReferenceEmbeddingRepository;
import com.whodis.backend.referenceimage.service.EmbeddingCodec;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoProcessingService {

    private final PhotoRepository photoRepository;
    private final PhotoFaceRepository photoFaceRepository;
    private final ReferenceEmbeddingRepository referenceEmbeddingRepository;
    private final StorageService storageService;
    private final MlServiceClient mlServiceClient;
    private final FaceMatchingService faceMatchingService;
    private final ProcessingJobService processingJobService;

    public void processPhoto(UUID jobId, UUID photoId) {

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Photo not found: " + photoId)
                );

        byte[] imageBytes;

        try (InputStream inputStream = storageService.load(photo.getStorageKey())) {
            imageBytes = inputStream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load photo", e);
        }

        PhotoFaceDetectionResponse detectionResponse =
                mlServiceClient.detectFaces(
                        imageBytes,
                        photo.getOriginalFilename(),
                        photo.getContentType()
                );

        UUID sessionId = photoRepository.findSessionIdByPhotoId(photoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Photo session not found")
                );

        List<ReferenceEmbedding> referenceEmbeddings =
                referenceEmbeddingRepository.findAllBySessionId(sessionId);

        List<PhotoFace> detectedFaces = detectionResponse.faces()
                .stream()
                .map(face -> createPhotoFace(
                        photo,
                        face,
                        detectionResponse,
                        referenceEmbeddings
                ))
                .toList();

        saveFacesAndCompleteJob(jobId, detectedFaces);
    }

    @Transactional
    protected void saveFacesAndCompleteJob(
            UUID jobId,
            List<PhotoFace> detectedFaces
    ) {
        photoFaceRepository.saveAll(detectedFaces);
        processingJobService.completeJob(jobId);
    }

    private PhotoFace createPhotoFace(
            Photo photo,
            DetectedFace face,
            PhotoFaceDetectionResponse response,
            List<ReferenceEmbedding> referenceEmbeddings
    ) {
        FaceMatchResult match = faceMatchingService.findBestMatch(
                face.embedding(),
                referenceEmbeddings
        );

        PhotoFace photoFace = new PhotoFace();

        photoFace.setId(UUID.randomUUID());
        photoFace.setPhoto(photo);

        if (match != null) {
            // Resolve Person from the matched reference embedding.
            photoFace.setPerson(
                    referenceEmbeddings.stream()
                            .filter(reference ->
                                    reference.getReferenceImage()
                                            .getPerson()
                                            .getId()
                                            .equals(match.personId())
                            )
                            .findFirst()
                            .orElseThrow()
                            .getReferenceImage()
                            .getPerson()
            );
            photoFace.setSimilarityScore(match.similarity());
        }

        photoFace.setBoundingBox(
                boundingBoxToJson(face)
        );

        photoFace.setEmbedding(
                EmbeddingCodec.encode(face.embedding())
        );

        photoFace.setModelName(response.model());
        photoFace.setModelVersion(response.modelVersion());
        photoFace.setEmbeddingDimension(response.dimension());
        photoFace.setCreatedAt(Instant.now());

        return photoFace;
    }

    private String boundingBoxToJson(DetectedFace face) {
        var box = face.boundingBox();

        return """
                {"x1":%s,"y1":%s,"x2":%s,"y2":%s}
                """.formatted(
                box.x1(),
                box.y1(),
                box.x2(),
                box.y2()
        ).trim();
    }
}