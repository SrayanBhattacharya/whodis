package com.whodis.backend.processingjob.queue;

import com.whodis.backend.processingjob.service.ProcessingJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    private final StringRedisTemplate redisTemplate;
    private final ProcessingJobService processingJobService;

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
            processMessage(message);
        }
    }

    private void processMessage(
            MapRecord<String, Object, Object> message
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
            boolean claimed = processingJobService.claimJob(jobId);

            if (!claimed) {
                log.info("Job {} was already claimed or is unavailable", jobId);
                redisTemplate.opsForStream().acknowledge(
                        STREAM_KEY, GROUP_NAME, message.getId()
                );
                return;
            }

            processingJobService.completeJob(jobId);

            redisTemplate.opsForStream().acknowledge(
                    STREAM_KEY, GROUP_NAME, message.getId()
            );

        } catch (Exception e) {
            log.error("Failed to handle processing job {}", jobId, e);
            // Leave the message pending for recovery.
        }
    }
}