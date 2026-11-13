package com.whodis.backend.processingjob.controller;

import com.whodis.backend.processingjob.dto.ProcessingJobResponse;
import com.whodis.backend.processingjob.service.ProcessingJobStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}")
@RequiredArgsConstructor
public class ProcessingJobController {

    private final ProcessingJobStatusService processingJobStatusService;

    @GetMapping("/photos/{photoId}/jobs")
    public ResponseEntity<List<ProcessingJobResponse>> getPhotoJobs(
            @PathVariable UUID sessionId,
            @PathVariable UUID photoId
    ) {
        return ResponseEntity.ok(
                processingJobStatusService.getPhotoJobs(
                        sessionId,
                        photoId
                )
        );
    }

    @GetMapping("/processing-jobs/{jobId}")
    public ResponseEntity<ProcessingJobResponse> getJobStatus(
            @PathVariable UUID sessionId,
            @PathVariable UUID jobId
    ) {
        return ResponseEntity.ok(
                processingJobStatusService.getJobStatus(
                        sessionId,
                        jobId
                )
        );
    }
}