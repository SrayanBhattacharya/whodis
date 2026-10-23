package com.whodis.backend.person.service;

public class DuplicatePersonException extends RuntimeException {
    public DuplicatePersonException(String name) {
        super("A person with the name '" + name + "' already exists in this session");
    }
}
