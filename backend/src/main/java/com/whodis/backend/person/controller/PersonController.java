package com.whodis.backend.person.controller;

import com.whodis.backend.person.dto.CreatePersonRequest;
import com.whodis.backend.person.dto.PersonResponse;
import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.service.PersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/people")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<PersonResponse> createPerson(
            @PathVariable UUID sessionId,
            @Valid @RequestBody CreatePersonRequest request
    ) {

        Person person = personService.createPerson(
                sessionId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(PersonResponse.from(person));
    }

    @GetMapping
    public ResponseEntity<List<PersonResponse>> getPeople(
            @PathVariable UUID sessionId
    ) {
        List<PersonResponse> people = personService
                .getPeople(sessionId)
                .stream()
                .map(PersonResponse::from)
                .toList();

        return ResponseEntity.ok(people);
    }

    @GetMapping("/{personId}")
    public ResponseEntity<PersonResponse> getPerson(
            @PathVariable UUID sessionId,
            @PathVariable UUID personId
    ) {
        Person person = personService.getPerson(
                sessionId,
                personId
        );

        return ResponseEntity.ok(PersonResponse.from(person));
    }
}
