package com.whodis.backend.referenceimage.service;

import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.person.service.PersonNotFoundException;
import com.whodis.backend.referenceimage.entity.ReferenceImage;
import com.whodis.backend.referenceimage.repository.ReferenceImageRepository;
import com.whodis.backend.storage.service.StorageException;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReferenceImageService {
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final ReferenceImageRepository referenceImageRepository;
    private final PersonRepository personRepository;
    private final StorageService storageService;

    public ReferenceImage uploadReferenceImage(
            UUID sessionId,
            UUID personId,
            MultipartFile file
    ) {
        validateFile(file);

        Person person = personRepository
                .findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new PersonNotFoundException(personId));

        String storageKey = null;

        try {
            storageKey = storageService.store(file.getInputStream());

            ReferenceImage referenceImage = new ReferenceImage(
                    UUID.randomUUID(),
                    person,
                    storageKey,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize(),
                    Instant.now()
            );

            return referenceImageRepository.save(referenceImage);

        } catch (IOException e) {
            cleanupStoredFile(storageKey);
            throw new StorageException("Failed to process uploaded file", e);

        } catch (RuntimeException e) {
            cleanupStoredFile(storageKey);
            throw e;
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("File must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidImageException(
                    "File size must not exceed 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException(
                    "Only JPEG, JPG, PNG, and WebP images are supported"
            );
        }
    }

    private void cleanupStoredFile(String storageKey) {
        if (storageKey == null) {
            return;
        }

        try {
            storageService.delete(storageKey);
        } catch (RuntimeException ignored) {
            // TODO: log cleanup failure
        }
    }
}
