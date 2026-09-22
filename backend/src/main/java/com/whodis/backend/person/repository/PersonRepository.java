package com.whodis.backend.person.repository;

import com.whodis.backend.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    boolean existsBySessionIdAndName(UUID sessionId, String name);
}
