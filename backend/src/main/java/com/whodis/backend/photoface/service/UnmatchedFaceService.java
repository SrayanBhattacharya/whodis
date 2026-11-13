package com.whodis.backend.photoface.service;

import com.whodis.backend.photoface.dto.UnmatchedFaceResponse;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnmatchedFaceService {

    private final PhotoFaceRepository photoFaceRepository;
    private final SessionService sessionService;

    @Transactional(readOnly = true)
    public List<UnmatchedFaceResponse> getUnmatchedFaces(UUID sessionId) {

        sessionService.getSession(sessionId);

        return photoFaceRepository
                .findAllByPhotoSessionIdAndPersonIsNullOrderByCreatedAtAsc(
                        sessionId
                )
                .stream()
                .map(UnmatchedFaceResponse::from)
                .toList();
    }
}