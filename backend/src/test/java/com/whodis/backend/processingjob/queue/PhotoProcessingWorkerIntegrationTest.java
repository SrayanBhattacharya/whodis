package com.whodis.backend.processingjob.queue;

import com.whodis.backend.photo.entity.Photo;
import com.whodis.backend.photo.repository.PhotoRepository;
import com.whodis.backend.processingjob.entity.ProcessingJob;
import com.whodis.backend.processingjob.entity.ProcessingJobStatus;
import com.whodis.backend.processingjob.repository.ProcessingJobRepository;
import com.whodis.backend.processingjob.service.ProcessingJobService;
import com.whodis.backend.session.entity.Session;
import com.whodis.backend.session.entity.SessionStatus;
import com.whodis.backend.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PhotoProcessingWorkerIntegrationTest {

    private static final String STREAM_KEY = "whodis:photo-processing";
    private static final String GROUP_NAME = "whodis:photo-processing:workers";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PhotoProcessingQueue queue;

    @Autowired
    private PhotoProcessingWorker worker;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private ProcessingJobService jobService;

    @BeforeEach
    void cleanStream() {
        redisTemplate.delete(STREAM_KEY);
        redisTemplate.opsForStream().add(
                STREAM_KEY,
                Map.of("initialization", "true")
        );
        redisTemplate.opsForStream().createGroup(
                STREAM_KEY,
                org.springframework.data.redis.connection.stream.ReadOffset.from("0"),
                GROUP_NAME
        );
    }

    @Test
    void workerShouldCompleteJobAndAcknowledgeMessage() {
        Instant now = Instant.now();

        Session session = new Session(
                UUID.randomUUID(),
                now,
                now.plusSeconds(3600),
                SessionStatus.ACTIVE
        );
        session = sessionRepository.saveAndFlush(session);

        Photo photo = new Photo(
                UUID.randomUUID(),
                session,
                UUID.randomUUID().toString(),
                "test.jpg",
                "image/jpeg",
                100L,
                now
        );
        photo = photoRepository.saveAndFlush(photo);

        ProcessingJob job = jobService.createJob(photo.getId());

        queue.publish(job.getId());

        List<MapRecord<String, Object, Object>> messages =
                redisTemplate.opsForStream()
                        .range(STREAM_KEY, Range.unbounded());

        MapRecord<String, Object, Object> message = messages.stream()
                .filter(record -> record.getValue().containsKey("jobId"))
                .filter(record -> job.getId().toString().equals(
                        record.getValue().get("jobId").toString()))
                .findFirst()
                .orElseThrow();

        worker.processMessage(message);

        ProcessingJob updatedJob = jobService.getJob(job.getId());

        assertThat(updatedJob.getStatus())
                .isEqualTo(ProcessingJobStatus.COMPLETED);

        Long pendingCount = redisTemplate.opsForStream()
                .pending(STREAM_KEY, GROUP_NAME)
                .getTotalPendingMessages();

        assertThat(pendingCount).isZero();
    }
}