package com.whodis.backend.photo.service;

import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.service.PersonNotFoundException;
import com.whodis.backend.person.service.PersonService;
import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoDownloadService {

    private final SessionService sessionService;
    private final PersonService personService;
    private final PhotoRepository photoRepository;
    private final StorageService storageService;
    private final ZipStreamingService zipStreamingService;

    @Transactional(readOnly = true)
    public PhotoDownload getSinglePhotoDownload(UUID sessionId, UUID photoId) {
        sessionService.getSession(sessionId);

        Photo photo = photoRepository.findByIdAndSessionId(photoId, sessionId)
                .orElseThrow(() -> new PhotoNotFoundException(photoId));

        InputStream stream = storageService.load(photo.getStorageKey());
        return new PhotoDownload(photo, stream);
    }

    @Transactional(readOnly = true)
    public StreamingResponseBody getBulkPhotosZip(UUID sessionId, List<UUID> photoIds) {
        sessionService.getSession(sessionId);

        if (photoIds == null || photoIds.isEmpty()) {
            throw new IllegalArgumentException("Photo IDs list cannot be empty");
        }

        // Deduplicate photoIds while preserving request
        Set<UUID> uniqueIds = new LinkedHashSet<>(photoIds);

        List<Photo> photos = photoRepository.findAllByIdInAndSessionId(new ArrayList<>(uniqueIds), sessionId);

        if (photos.size() != uniqueIds.size()) {
            // Find which photo ID(s) were missing or belong to another session
            Set<UUID> foundIds = new HashSet<>();
            for (Photo p : photos) {
                foundIds.add(p.getId());
            }

            for (UUID requestedId : uniqueIds) {
                if (!foundIds.contains(requestedId)) {
                    throw new PhotoNotFoundException(requestedId);
                }
            }
        }

        return zipStreamingService.streamPhotosAsZip(photos);
    }

    @Transactional(readOnly = true)
    public PersonPhotosZip getPersonPhotosZip(UUID sessionId, UUID personId) {
        sessionService.getSession(sessionId);

        Person person = personService.getPerson(sessionId, personId);

        List<Photo> photos = photoRepository.findDistinctPhotosByPersonIdAndSessionId(personId, sessionId);

        if (photos.isEmpty()) {
            throw new NoSuchElementException("No photos found for person: " + person.getName());
        }

        String safePersonName = sanitizePersonName(person.getName());
        String zipFilename = "whodis-" + safePersonName + "-photos.zip";

        StreamingResponseBody stream = zipStreamingService.streamPhotosAsZip(photos);
        return new PersonPhotosZip(zipFilename, stream);
    }

    public static String sanitizePersonName(String personName) {
        if (personName == null || personName.isBlank()) {
            return "person";
        }

        // Replace spaces with hyphen, strip non-alphanumeric except hyphen and underscore
        String sanitized = personName.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9_-]", "");

        if (sanitized.isEmpty()) {
            return "person";
        }

        return sanitized;
    }

    public record PhotoDownload(
            Photo photo,
            InputStream stream
    ) {}

    public record PersonPhotosZip(
            String filename,
            StreamingResponseBody stream
    ) {}
}
