package com.whodis.backend.photoface.service;

import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.photoface.dto.AssignFaceRequest;
import com.whodis.backend.photoface.dto.FaceAssignmentResponse;
import com.whodis.backend.photoface.entity.PhotoFace;
import com.whodis.backend.photoface.repository.PhotoFaceRepository;
import com.whodis.backend.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FaceAssignmentService {

    private final PhotoFaceRepository photoFaceRepository;
    private final PersonRepository personRepository;
    private final SessionService sessionService;

    @Transactional
    public FaceAssignmentResponse assignFace(
            UUID sessionId,
            UUID faceId,
            AssignFaceRequest request
    ) {
        sessionService.getSession(sessionId);

        PhotoFace face = photoFaceRepository
                .findByIdForUpdate(faceId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Face not found"
                        )
                );

        if (!face.getPhoto().getSession().getId().equals(sessionId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Face not found in this session"
            );
        }

        Person person = personRepository
                .findByIdAndSessionId(request.personId(), sessionId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Person not found in this session"
                        )
                );

        face.setPerson(person);

        PhotoFace updatedFace = photoFaceRepository.save(face);

        return FaceAssignmentResponse.from(updatedFace);
    }
}