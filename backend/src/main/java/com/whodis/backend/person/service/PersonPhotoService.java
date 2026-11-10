package com.whodis.backend.person.service;

import com.whodis.backend.photoface.entity.PhotoFace;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonPhotoService {

    private final PersonService personService;
    private final PhotoFaceRepository photoFaceRepository;

    @Transactional(readOnly = true)
    public List<PhotoFace> getPersonPhotos(
            UUID sessionId,
            UUID personId
    ) {
        personService.getPerson(sessionId, personId);

        return photoFaceRepository
                .findAllByPersonIdAndPhotoSessionIdOrderByCreatedAtAsc(
                        personId,
                        sessionId
                );
    }
}