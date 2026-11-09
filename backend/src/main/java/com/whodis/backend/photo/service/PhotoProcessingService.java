package com.whodis.backend.photo.service;

import com.whodis.backend.ml.client.MlServiceClient;
import com.whodis.backend.ml.dto.DetectedFace;
import com.whodis.backend.ml.dto.FaceMatchResult;
import com.whodis.backend.ml.dto.PhotoFaceDetectionResponse;
import com.whodis.backend.ml.service.FaceMatchingService;
import com.whodis.backend.photoface.entity.PhotoFace;
import com.whodis.backend.photoface.service.PhotoFacePersistenceService;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import com.whodis.backend.referenceimage.repository.ReferenceEmbeddingRepository;
import com.whodis.backend.referenceimage.service.EmbeddingCodec;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoProcessingService {

    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final MlServiceClient mlServiceClient;
    private final ReferenceEmbeddingRepository referenceEmbeddingRepository;
    private final FaceMatchingService faceMatchingService;
    private final PhotoFacePersistenceService persistenceService;

    public void processPhoto(UUID jobId, UUID photoId) {

        Photo photo = photoRepository.findByIdWithSession(photoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Photo not found: " + photoId
                        )
                );

        byte[] imageBytes;

        try (InputStream inputStream =
                     storageService.load(photo.getStorageKey())) {

            imageBytes = inputStream.readAllBytes();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load photo: " + photoId,
                    e
            );
        }

        PhotoFaceDetectionResponse detectionResponse =
                mlServiceClient.detectFaces(
                        imageBytes,
                        photo.getOriginalFilename(),
                        photo.getContentType()
                );

        List<ReferenceEmbedding> referenceEmbeddings =
                referenceEmbeddingRepository.findAllBySessionId(
                        photo.getSession().getId()
                );

        List<PhotoFace> faces = detectionResponse.faces()
                .stream()
                .map(face -> createPhotoFace(
                        photo,
                        face,
                        detectionResponse,
                        referenceEmbeddings
                ))
                .toList();

        persistenceService.saveFacesAndCompleteJob(jobId, photoId, faces);
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

        var box = face.boundingBox();

        photoFace.setBoundingBox("""
                {"x1":%s,"y1":%s,"x2":%s,"y2":%s}
                """.formatted(
                box.x1(),
                box.y1(),
                box.x2(),
                box.y2()
        ).trim());

        photoFace.setEmbedding(
                EmbeddingCodec.encode(face.embedding())
        );

        photoFace.setModelName(response.model());
        photoFace.setModelVersion(response.modelVersion());
        photoFace.setEmbeddingDimension(response.dimension());
        photoFace.setCreatedAt(Instant.now());

        return photoFace;
    }
}