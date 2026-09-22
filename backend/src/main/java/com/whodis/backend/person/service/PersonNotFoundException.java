package com.whodis.backend.person.service;

import java.util.UUID;

public class PersonNotFoundException extends RuntimeException {
    public PersonNotFoundException(UUID personId) {
        super("Person not found: " + personId);
    }
}
