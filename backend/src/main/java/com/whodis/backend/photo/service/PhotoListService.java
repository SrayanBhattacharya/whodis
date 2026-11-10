package com.whodis.backend.photo.service;

import com.whodis.backend.photo.dto.PhotoListResponse;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.session.service.SessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoListService {

    private final SessionService sessionService;
    private final PhotoRepository photoRepository;

    @Transactional(readOnly = true)
    public List<PhotoListResponse> getSessionPhotos(UUID sessionId) {
        sessionService.getSession(sessionId);

        return photoRepository
                .findAllBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(PhotoListResponse::from)
                .toList();
    }
}