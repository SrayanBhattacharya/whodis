package com.whodis.backend.person.repository;

import com.whodis.backend.person.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonRepository extends JpaRepository<Person, UUID> {
    boolean existsBySessionIdAndName(UUID sessionId, String name);
    List<Person> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);
    Optional<Person> findByIdAndSessionId(UUID personId, UUID sessionId);

    @Modifying(flushAutomatically = true)
    @Query("""
    DELETE FROM Person p
    WHERE p.session.id = :sessionId
    """)
    void deleteAllBySessionId(
            @Param("sessionId") UUID sessionId
    );
}
