package com.whodis.backend.processingjob.queue;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PhotoProcessingQueue {
    private static final String STREAM_KEY = "whodis:photo-processing";

    private final StringRedisTemplate redisTemplate;

    public void publish(UUID jobId) {
        MapRecord<String, String, String> record =
                StreamRecords.mapBacked(
                        Map.of("jobId", jobId.toString())
                ).withStreamKey(STREAM_KEY);

        redisTemplate.opsForStream().add(record);
    }
}
