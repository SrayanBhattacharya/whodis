package com.whodis.backend.processingjob.service;

import com.whodis.backend.processingjob.queue.PhotoProcessingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class PhotoQueueReconciliationService {

    private final ProcessingJobService processingJobService;
    private final PhotoProcessingQueue photoProcessingQueue;

    @Scheduled(
            fixedDelayString =
                    "${whodis.processing.worker.reconciliation-interval:30000}"
    )
    public void reconcileUnpublishedJobs() {

        List<UUID> jobIds =
                processingJobService.getUnpublishedPendingJobIds();

        for (UUID jobId : jobIds) {
            try {
                photoProcessingQueue.publish(jobId);

                processingJobService.markQueuePublished(jobId);

                log.info(
                        "Successfully republished pending job: {}",
                        jobId
                );

            } catch (Exception e) {
                log.error(
                        "Failed to reconcile processing job: {}",
                        jobId,
                        e
                );
            }
        }
    }
}