package com.whodis.backend.processingjob.queue;

import com.whodis.backend.photo.service.PhotoProcessingService;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import com.whodis.backend.processingjob.service.ProcessingJobService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoProcessingWorker {

    private static final String STREAM_KEY = PhotoProcessingStreamConfig.STREAM_KEY;
    private static final String GROUP_NAME = PhotoProcessingStreamConfig.GROUP_NAME;
    private static final String CONSUMER_NAME = "whodis-worker-1";
    private final PhotoProcessingService photoProcessingService;

    private final StringRedisTemplate redisTemplate;
    private final ProcessingJobService processingJobService;

    @Value("${whodis.processing.worker.pending-idle-time:60s}")
    private Duration pendingIdleTime;

    @Value("${whodis.processing.worker.max-attempts:3}")
    private int maxAttempts;

    private static final int RECOVERY_BATCH_SIZE = 10;

    @Scheduled(fixedDelay = 1000)
    public void consumeMessages() {
        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream().read(
                        Consumer.from(GROUP_NAME, CONSUMER_NAME),
                        org.springframework.data.redis.connection.stream
                                .StreamReadOptions.empty().count(10),
                        StreamOffset.create(
                                STREAM_KEY,
                                ReadOffset.lastConsumed()
                        )
                );

        if (messages == null || messages.isEmpty()) {
            return;
        }

        for (MapRecord<String, Object, Object> message : messages) {
            processMessage(message, false);
        }
    }

    private void processMessage(
            MapRecord<String, Object, Object> message,
            boolean recovered
    ) {
        Map<Object, Object> values = message.getValue();
        Object rawJobId = values.get("jobId");

        if (rawJobId == null) {
            // Ignore the stream initialization marker.
            redisTemplate.opsForStream().acknowledge(
                    STREAM_KEY, GROUP_NAME, message.getId()
            );
            return;
        }

        UUID jobId;

        try {
            jobId = UUID.fromString(rawJobId.toString());
        } catch (IllegalArgumentException e) {
            log.error("Invalid job ID in Redis message {}", message.getId());
            redisTemplate.opsForStream().acknowledge(
                    STREAM_KEY, GROUP_NAME, message.getId()
            );
            return;
        }

        try {
            boolean claimed;

            if (!recovered) {
                claimed = processingJobService.claimJob(jobId);
            } else {
                var job = processingJobService.getJob(jobId);

                if (job.getStatus() == ProcessingJobStatus.PENDING) {
                    claimed = processingJobService.claimJob(jobId);
                } else if (job.getStatus() == ProcessingJobStatus.PROCESSING) {
                    claimed = processingJobService.reclaimStaleJob(
                            jobId,
                            pendingIdleTime
                    );
                } else {
                    acknowledgeMessage(message);
                    return;
                }
            }

            if (!claimed) {

                if (recovered) {
                    var job = processingJobService.getJob(jobId);

                    if (job.getStatus() != ProcessingJobStatus.PROCESSING) {
                        acknowledgeMessage(message);
                    } else {
                        log.info(
                                "Job {} is not eligible for recovery",
                                jobId
                        );
                    }

                } else {
                    log.info(
                            "Job {} was already claimed or is unavailable",
                            jobId
                    );

                    acknowledgeMessage(message);
                }

                return;
            }

            UUID photoId = processingJobService.getPhotoId(jobId);

            photoProcessingService.processPhoto(jobId, photoId);

            log.info("Successfully processed photo for job {}", jobId);

        } catch (Exception e) {
        log.error("Failed to handle processing job {}", jobId, e);

        try {
            String errorMessage = getErrorMessage(e);

            boolean retryScheduled = processingJobService.scheduleRetry(
                    jobId,
                    errorMessage,
                    maxAttempts
            );

            if (retryScheduled) {
                log.info(
                        "Retry scheduled for job {}. Message remains pending.",
                        jobId
                );

                return;
            }

            processingJobService.failJob(jobId, errorMessage);

            log.error(
                    "Job {} exhausted its attempts and transitioned to FAILED",
                    jobId
            );

        } catch (Exception failureException) {
            log.error(
                    "Failed to handle failure for job {}. Message remains pending.",
                    jobId,
                    failureException
            );
            return;
        }
    }

        acknowledgeMessage(message);
    }

    @Scheduled(
            fixedDelayString = "${whodis.processing.worker.recovery-interval:30000}"
    )
    public void recoverPendingMessages() {

        PendingMessages pending = redisTemplate.opsForStream().pending(
                STREAM_KEY,
                GROUP_NAME,
                Range.unbounded(),
                RECOVERY_BATCH_SIZE,
                pendingIdleTime
        );

        if (pending.isEmpty()) {
            return;
        }

        RecordId[] ids = pending.stream()
                .map(PendingMessage::getId)
                .toArray(RecordId[]::new);

        List<MapRecord<String, Object, Object>> claimed =
                redisTemplate.opsForStream().claim(
                        STREAM_KEY,
                        GROUP_NAME,
                        CONSUMER_NAME,
                        pendingIdleTime,
                        ids
                );

        if (claimed == null || claimed.isEmpty()) {
            return;
        }

        log.info("Recovered {} pending messages", claimed.size());

        for (MapRecord<String, Object, Object> message : claimed) {
            processRecoveredMessage(message);
        }
    }

    private String getErrorMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        return message.length() > 2000
                ? message.substring(0, 2000)
                : message;
    }

    private void acknowledgeMessage(
            MapRecord<String, Object, Object> message
    ) {
        redisTemplate.opsForStream().acknowledge(
                STREAM_KEY,
                GROUP_NAME,
                message.getId()
        );
    }

    private void processRecoveredMessage(
            MapRecord<String, Object, Object> message
    ) {
        processMessage(message, true);
    }
}