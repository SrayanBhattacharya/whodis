package com.whodis.backend.processingjob.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.RedisSystemException;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PhotoProcessingStreamConfig implements InitializingBean {

    static final String STREAM_KEY = "whodis:photo-processing";
    static final String GROUP_NAME = "whodis:photo-processing:workers";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void afterPropertiesSet() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(STREAM_KEY))) {
            redisTemplate.opsForStream().add(
                    STREAM_KEY,
                    Map.of("initialization", "true")
            );
        }

        try {
            redisTemplate.opsForStream().createGroup(
                    STREAM_KEY,
                    ReadOffset.from("0"),
                    GROUP_NAME
            );

            log.info("Created Redis consumer group: {}", GROUP_NAME);

        } catch (RedisSystemException e) {
            if (hasCause(e, "BUSYGROUP")) {
                log.info(
                        "Redis consumer group already exists: {}",
                        GROUP_NAME
                );
            } else {
                throw e;
            }
        }
    }

    private boolean hasCause(Throwable throwable, String message) {
        Throwable current = throwable;

        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(message)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }
}