package com.whodis.backend.person.dto;

import com.whodis.backend.person.entity.Person;

import java.time.Instant;
import java.util.UUID;

public record PersonResponse(
        UUID id,
        UUID sessionId,
        String name,
        Instant createdAt
) {
    public static PersonResponse from(Person person) {
        return new PersonResponse(
                person.getId(),
                person.getSession().getId(),
                person.getName(),
                person.getCreatedAt()
        );
    }
}
