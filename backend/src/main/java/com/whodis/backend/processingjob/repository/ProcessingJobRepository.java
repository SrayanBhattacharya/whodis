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

    @Modifying
    @Query("""
        UPDATE ProcessingJob j
        SET j.startedAt = :now,
            j.attemptCount = j.attemptCount + 1,
            j.errorMessage = null
        WHERE j.id = :jobId
          AND j.status = :status
          AND j.startedAt <= :cutoff
        """)
    int reclaimStaleJob(
            @Param("jobId") UUID jobId,
            @Param("status") ProcessingJobStatus status,
            @Param("cutoff") Instant cutoff,
            @Param("now") Instant now
    );

    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
    UPDATE ProcessingJob j
    SET j.status = :pendingStatus,
        j.startedAt = null,
        j.completedAt = null,
        j.errorMessage = :errorMessage
    WHERE j.id = :jobId
      AND j.status = :processingStatus
      AND j.attemptCount < :maxAttempts
    """)
    int scheduleRetry(
            @Param("jobId") UUID jobId,
            @Param("processingStatus") ProcessingJobStatus processingStatus,
            @Param("pendingStatus") ProcessingJobStatus pendingStatus,
            @Param("errorMessage") String errorMessage,
            @Param("maxAttempts") int maxAttempts
    );
}
