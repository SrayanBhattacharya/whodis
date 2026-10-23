package com.whodis.backend.person.service;

import com.whodis.backend.person.dto.CreatePersonRequest;
import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.service.SessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;
    private final SessionService sessionService;

    @Transactional
    public Person createPerson(
            UUID sessionId,
            CreatePersonRequest request
    ) {
        Session session = sessionService.getSession(sessionId);
        String name = request.name().trim();

        if (personRepository.existsBySessionIdAndName(sessionId, name)) {
            throw new DuplicatePersonException(name);
        }

        Person person = new Person(
                UUID.randomUUID(),
                session,
                name,
                Instant.now()
        );

        return personRepository.save(person);
    }

    @Transactional(readOnly = true)
    public List<Person> getPeople(UUID sessionId) {
        sessionService.getSession(sessionId);

        return personRepository.findAllBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    @Transactional(readOnly = true)
    public Person getPerson(
            UUID sessionId,
            UUID personId
    ) {
        sessionService.getSession(sessionId);

        return personRepository
                .findByIdAndSessionId(personId, sessionId)
                .orElseThrow(() -> new PersonNotFoundException(personId));
    }
}
