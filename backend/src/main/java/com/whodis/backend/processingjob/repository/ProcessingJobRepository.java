package com.whodis.backend.processingjob.repository;

import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findAllByPhotoIdOrderByCreatedAtDesc(UUID photoId);

    List<ProcessingJob> findAllByStatusOrderByCreatedAtAsc(
            ProcessingJobStatus status
    );

    @Modifying
    @Query("""
    UPDATE ProcessingJob j
    SET j.status = :processing,
        j.attemptCount = j.attemptCount + 1,
        j.startedAt = :startedAt,
        j.errorMessage = null
    WHERE j.id = :jobId
      AND j.status = :pending
    """)
    int claimJob(
            @Param("jobId") UUID jobId,
            @Param("pending") ProcessingJobStatus pending,
            @Param("processing") ProcessingJobStatus processing,
            @Param("startedAt") Instant startedAt
    );
}
