
package com.whodis.backend.processingjob.queue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PhotoProcessingQueueTest {

    private static final String STREAM_KEY =
            "whodis:photo-processing";

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private PhotoProcessingQueue queue;

    @BeforeEach
    void setUp() {
        connectionFactory =
                new LettuceConnectionFactory("localhost", 6379);

        connectionFactory.afterPropertiesSet();
        connectionFactory.start();

        redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();

        connectionFactory.getConnection().ping();

        queue = new PhotoProcessingQueue(redisTemplate);

        redisTemplate.delete(STREAM_KEY);
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void publishShouldWriteJobIdToRedisStream() {
        UUID jobId = UUID.randomUUID();

        queue.publish(jobId);

        List<MapRecord<String, Object, Object>> records =
                redisTemplate.opsForStream()
                        .range(STREAM_KEY, Range.unbounded());

        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getValue())
                .containsEntry("jobId", jobId.toString());
    }
}
