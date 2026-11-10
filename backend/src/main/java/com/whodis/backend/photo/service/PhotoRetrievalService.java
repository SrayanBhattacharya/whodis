package com.whodis.backend.photo.service;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.session.service.SessionService;
import com.whodis.backend.storage.service.StorageService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoRetrievalService {

    private final PhotoRepository photoRepository;
    private final SessionService sessionService;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public Optional<RetrievedPhoto> getPhoto(
            UUID sessionId,
            UUID photoId
    ) {
        sessionService.getSession(sessionId);

        return photoRepository
                .findByIdAndSessionId(photoId, sessionId)
                .map(photo -> new RetrievedPhoto(
                        photo,
                        storageService.load(photo.getStorageKey())
                ));
    }

    public record RetrievedPhoto(
            Photo photo,
            InputStream content
    ) {}
}