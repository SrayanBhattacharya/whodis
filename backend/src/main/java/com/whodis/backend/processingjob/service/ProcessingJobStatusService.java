package com.whodis.backend.processingjob.service;

import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.processingjob.dto.ProcessingJobResponse;
import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.repository.ProcessingJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessingJobStatusService {

    private final PhotoRepository photoRepository;
    private final ProcessingJobRepository processingJobRepository;

    @Transactional(readOnly = true)
    public List<ProcessingJobResponse> getPhotoJobs(
            UUID sessionId,
            UUID photoId
    ) {
        boolean photoExists = photoRepository
                .findByIdAndSessionId(photoId, sessionId)
                .isPresent();

        if (!photoExists) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Photo not found in this session"
            );
        }

        return processingJobRepository
                .findAllByPhotoIdOrderByCreatedAtDesc(photoId)
                .stream()
                .map(ProcessingJobResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProcessingJobResponse getJobStatus(
            UUID sessionId,
            UUID jobId
    ) {
        ProcessingJob job = processingJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Processing job not found"
                        )
                );

        UUID photoId = job.getPhoto().getId();

        boolean photoBelongsToSession = photoRepository
                .findByIdAndSessionId(photoId, sessionId)
                .isPresent();

        if (!photoBelongsToSession) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Processing job not found in this session"
            );
        }

        return ProcessingJobResponse.from(job);
    }
}