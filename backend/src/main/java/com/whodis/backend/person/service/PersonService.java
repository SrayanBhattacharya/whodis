package com.whodis.backend.person.service;

import com.whodis.backend.person.dto.CreatePersonRequest;
import com.whodis.backend.person.entity.Person;
import com.whodis.backend.person.repository.PersonRepository;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
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
}
