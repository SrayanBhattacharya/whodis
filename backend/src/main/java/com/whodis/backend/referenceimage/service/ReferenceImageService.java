package com.whodis.backend.referenceimage.service;

import com.whodis.backend.ml.client.MlServiceClient;
import com.whodis.backend.ml.dto.ReferenceEmbeddingResponse;
import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.person.service.PersonNotFoundException;
import com.whodis.backend.referenceimage.entity.ReferenceEmbedding;
import com.whodis.backend.referenceimage.entity.ReferenceImage;
import com.whodis.backend.referenceimage.repository.ReferenceEmbeddingRepository;
import com.whodis.backend.referenceimage.repository.ReferenceImageRepository;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageException;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReferenceImageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final ReferenceImageRepository referenceImageRepository;
    private final PersonRepository personRepository;
    private final StorageService storageService;
    private final ImageValidationService imageValidationService;
    private final SessionService sessionService;
    private final MlServiceClient mlServiceClient;
    private final ReferenceEmbeddingRepository referenceEmbeddingRepository;

    @Transactional
    public ReferenceImage uploadReferenceImage(
            UUID sessionId,
            UUID personId,
            MultipartFile file
    ) {
        imageValidationService.validate(file);

        Person person = personRepository
                .findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new PersonNotFoundException(personId));

        String storageKey = null;

        try {
            storageKey = storageService.store(file.getInputStream());

            byte[] imageBytes = file.getBytes();

            ReferenceEmbeddingResponse embedding =
                    mlServiceClient.generateReferenceEmbedding(
                            imageBytes,
                            file.getOriginalFilename(),
                            file.getContentType()
                    );

            if (embedding.dimension() != 512) {
                throw new IllegalStateException(
                        "Unexpected embedding dimension: " + embedding.dimension() + ", expected 512"
                        );
            }

            ReferenceImage referenceImage = new ReferenceImage(
                    UUID.randomUUID(),
                    person,
                    storageKey,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    Instant.now()
            );

            referenceImage = referenceImageRepository.save(referenceImage);

            byte[] embeddingBytes = EmbeddingCodec.encode(embedding.embedding());

            ReferenceEmbedding referenceEmbedding = new ReferenceEmbedding(
                    UUID.randomUUID(),
                    referenceImage,
                    embeddingBytes,
                    embedding.model(),
                    embedding.modelVersion(),
                    embedding.dimension(),
                    Instant.now()
            );
            referenceEmbeddingRepository.save(referenceEmbedding);

            return referenceImage;

        } catch (IOException e) {
            cleanupStoredFile(storageKey);
            throw new StorageException("Failed to process uploaded file", e);

        } catch (RuntimeException e) {
            cleanupStoredFile(storageKey);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ReferenceImage> getReferenceImages(
            UUID sessionId,
            UUID personId
    ) {
        sessionService.getSession(sessionId);

        personRepository.findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new ReferenceImageNotFoundException(personId));

        return referenceImageRepository
                .findAllByPersonIdOrderByCreatedAtAsc(personId);
    }

    @Transactional(readOnly = true)
    public ReferenceImage getReferenceImage(
            UUID sessionId,
            UUID personId,
            UUID imageId
    ) {
        sessionService.getSession(sessionId);

        personRepository.findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new PersonNotFoundException(personId));

        return referenceImageRepository
                .findByIdAndPersonId(imageId, personId)
                .orElseThrow(() -> new ReferenceImageNotFoundException(imageId));
    }

    @Transactional
    public void deleteReferenceImage(
            UUID sessionId,
            UUID personId,
            UUID imageId
    ) {
        sessionService.getSession(sessionId);

        personRepository.findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new PersonNotFoundException(personId));

        ReferenceImage image = referenceImageRepository
                .findByIdAndPersonId(imageId, personId)
                .orElseThrow(() -> new ReferenceImageNotFoundException(imageId));

        storageService.delete(image.getStorageKey());

        referenceImageRepository.delete(image);
    }

    private void cleanupStoredFile(String storageKey) {
        if (storageKey == null) {
            return;
        }

        try {
            storageService.delete(storageKey);
        } catch (RuntimeException e) {
            log.error("Failed to clean up stored file with key: {}", storageKey, e);
        }
    }
}
