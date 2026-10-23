package com.whodis.backend.processingjob.repository;

import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProcessingJobRepository
        extends JpaRepository<ProcessingJob, UUID> {

    List<ProcessingJob> findAllByPhotoIdOrderByCreatedAtDesc(UUID photoId);

    List<ProcessingJob> findAllByStatusOrderByCreatedAtAsc(
            ProcessingJobStatus status
    );
}
