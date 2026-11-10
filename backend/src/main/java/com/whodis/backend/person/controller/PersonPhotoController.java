package com.whodis.backend.person.controller;

import com.whodis.backend.person.dto.PersonPhotoResponse;
import com.whodis.backend.person.service.PersonPhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/people/{personId}/photos")
@RequiredArgsConstructor
public class PersonPhotoController {

    private final PersonPhotoService personPhotoService;

    @GetMapping
    public ResponseEntity<List<PersonPhotoResponse>> getPersonPhotos(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId
    ) {
        List<PersonPhotoResponse> responses = personPhotoService
                .getPersonPhotos(sessionId, personId)
                .stream()
                .map(PersonPhotoResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }
}
